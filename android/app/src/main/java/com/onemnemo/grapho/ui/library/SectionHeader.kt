package com.onemnemo.grapho.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val ViewToggleWidth = 96.dp

/**
 * The heading of the notes: the title with a quiet count, then the Sort pill and the grid and list switch.
 * A [compact] window drops the count and the word Sort, so the tools keep their room.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int,
    view: LibraryView,
    onView: (LibraryView) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Grapho.metrics.sizeControlDefault),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(title, style = Grapho.type.heading, color = c.textMuted, maxLines = 1)
            if (!compact) Text(count.toString(), style = Grapho.type.tabular, color = c.textSubtle, maxLines = 1)
        }
        SortPill(view, onView, showName = !compact)
        SegmentedControl(
            options = listOf(
                ViewMode.Grid to stringResource(R.string.library_view_grid),
                ViewMode.List to stringResource(R.string.library_view_list),
            ),
            selected = view.mode,
            onSelect = { onView(view.copy(mode = it)) },
            modifier = Modifier.width(ViewToggleWidth),
            size = SegmentSize.Small,
            icons = mapOf(ViewMode.Grid to R.drawable.ic_grid, ViewMode.List to R.drawable.ic_list),
        )
    }
}

/** "Sort  Last edited" in a pill, opening the sort menu under it. */
@Composable
private fun SortPill(view: LibraryView, onView: (LibraryView) -> Unit, showName: Boolean) {
    val c = Grapho.colors
    var open by remember { mutableStateOf(false) }
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            open || interaction.hovered -> c.surfaceControlHover
            else -> c.surfaceControl
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "sortFill",
    )
    val sortName = stringResource(R.string.library_sort)
    val value = stringResource(sortLabel(view.sortKey))
    val description = stringResource(R.string.library_sort_description, sortName, value)
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = Grapho.metrics.sizeControlSmall)
                .pressable(interactionSource = interaction.source, onClick = { open = true })
                .focusRing(interaction.focused, GraphoShapes.pill)
                .semantics { contentDescription = description }
                .clip(GraphoShapes.pill)
                .background(fill)
                .border(GraphoSpace.borderWidthHairline, c.borderSubtle, GraphoShapes.pill)
                .padding(horizontal = GraphoSpace.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showName) Text(sortName, style = Grapho.type.caption, color = c.textMuted, maxLines = 1)
            Text(value, style = Grapho.type.label, color = c.textDefault, maxLines = 1)
        }
        SortMenu(expanded = open, onDismiss = { open = false }, view = view, onChange = onView)
    }
}
