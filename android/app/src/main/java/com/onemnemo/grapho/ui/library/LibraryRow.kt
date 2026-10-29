package com.onemnemo.grapho.ui.library

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val RowHeight = 64.dp
private val ThumbWidth = 36.dp
private val ThumbHeight = 44.dp
private val ThumbShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp)

/** Room the badge takes at the start of a row while choosing. */
private val BadgeRoom = 44.dp

private const val DIM_ALPHA = 0.6f

@DrawableRes
private fun kindIcon(kind: EntryKind): Int = when (kind) {
    EntryKind.Notebook -> R.drawable.ic_notebook
    EntryKind.Pdf -> R.drawable.ic_pdf
    EntryKind.Canvas -> R.drawable.ic_canvas
}

/**
 * One note in list view: a small cover, its name, and on a window of 600 dp or more the kind, size and date in
 * their own columns. On a compact window the kind and size sit under the name instead.
 */
@Composable
fun LibraryRow(
    entry: NotebookEntry,
    width: WidthClass,
    selecting: Boolean,
    selected: Boolean,
    query: String,
    actions: ItemActions,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val name = entry.displayName()
    val compact = width == WidthClass.Compact
    val fill by animateColorAsState(
        when {
            selected -> c.selectionFill
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "rowFill",
    )
    val start by animateDpAsState(
        if (selecting) BadgeRoom + GraphoSpace.spaceMd else GraphoSpace.spaceMd,
        tween(GraphoMotion.motionDurationSlowMs, easing = GraphoMotion.motionEasingStandard),
        label = "rowStart",
    )
    val dim by animateFloatAsState(
        if (selecting && !selected) DIM_ALPHA else 1f,
        tween(GraphoMotion.motionDurationBaseMs),
        label = "rowDim",
    )
    val columnGap = GraphoSpace.spaceLg
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
            horizontalArrangement = Arrangement.spacedBy(columnGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(ThumbWidth, ThumbHeight).clip(ThumbShape)) {
                when (entry.kind) {
                    EntryKind.Notebook -> CoverArt(CoverPalette.resolve(entry.coverColor), null)
                    EntryKind.Pdf -> PdfArt()
                    EntryKind.Canvas -> CanvasArt(entry.background)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        highlighted(name, query),
                        style = Grapho.type.label,
                        color = c.textDefault,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.starred) {
                        GraphoIcon(
                            R.drawable.ic_star_filled,
                            stringResource(R.string.library_starred),
                            c.textSubtle,
                            size = GraphoSpace.sizeIconInline,
                        )
                    }
                }
                if (compact) {
                    Text(
                        metaLabel(entry),
                        style = Grapho.type.caption,
                        color = c.textSubtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!compact) {
                val typeWidth = if (width == WidthClass.Medium) 110.dp else 150.dp
                Row(
                    Modifier.width(typeWidth),
                    horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GraphoIcon(kindIcon(entry.kind), null, c.textSubtle, size = GraphoSpace.sizeIconInline)
                    Text(kindLabel(entry.kind), style = Grapho.type.caption, color = c.textMuted, maxLines = 1)
                }
                Text(
                    sizeLabel(entry),
                    style = Grapho.type.tabular,
                    color = c.textMuted,
                    maxLines = 1,
                    modifier = Modifier.width(90.dp),
                )
                Text(
                    agoLabel(agoOf(entry.modifiedMs)),
                    style = Grapho.type.tabular,
                    color = c.textMuted,
                    maxLines = 1,
                    modifier = Modifier.width(100.dp),
                )
            }
            if (!selecting) ItemMoreButton(kind = entry.kind, actions = actions)
        }
    }
}
