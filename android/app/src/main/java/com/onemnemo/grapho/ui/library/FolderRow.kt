package com.onemnemo.grapho.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val RowHeight = 64.dp
private val ArtWidth = 56.dp

/** Room the badge takes at the start of a row while choosing. */
private val BadgeRoom = 44.dp

/** How far each level of folders steps in. */
private val LevelIndent = 20.dp

private const val DIM_ALPHA = 0.6f

/** One folder in the tree of All folders: a small folder, its name and what it holds, indented by its depth. */
@Composable
fun FolderRow(
    node: FolderNode,
    counts: FolderCounts,
    selecting: Boolean,
    selected: Boolean,
    actions: FolderActions,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            selected -> c.selectionFill
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "folderRowFill",
    )
    val start by animateDpAsState(
        (if (selecting) BadgeRoom else 0.dp) + GraphoSpace.spaceMd + LevelIndent * node.depth,
        tween(GraphoMotion.motionDurationSlowMs, easing = GraphoMotion.motionEasingStandard),
        label = "folderRowStart",
    )
    val dim by animateFloatAsState(
        if (selecting && !selected) DIM_ALPHA else 1f,
        tween(GraphoMotion.motionDurationBaseMs),
        label = "folderRowDim",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(RowHeight)
            .pressable(
                scaleOnPress = false,
                role = if (selecting) Role.Checkbox else Role.Button,
                interactionSource = interaction.source,
                onLongClick = if (selecting) null else onLongClick,
                onClick = onClick,
            )
            .focusRing(interaction.focused, GraphoShapes.surface, inset = true)
            .semantics { if (selecting) this.selected = selected }
            .graphicsLayer { alpha = dim }
            .clip(GraphoShapes.surface)
            .background(fill),
    ) {
        SelectionBadge(
            visible = selecting,
            selected = selected,
            small = true,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = GraphoSpace.spaceLg),
        )
        Row(
            Modifier.fillMaxWidth().height(RowHeight).padding(start = start, end = GraphoSpace.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderArt(folderColour(node.folder.id), Modifier.width(ArtWidth))
            Column(Modifier.weight(1f)) {
                Text(
                    node.folder.name,
                    style = Grapho.type.label,
                    color = c.textDefault,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    folderMeta(counts),
                    style = Grapho.type.caption,
                    color = c.textSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!selecting) FolderMoreButton(actions = actions)
        }
    }
}
