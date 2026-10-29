package com.onemnemo.grapho.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoMenuLabel
import com.onemnemo.grapho.ui.components.GraphoPopover
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.theme.GraphoSpace

@StringRes
fun sortLabel(key: SortKey): Int = when (key) {
    SortKey.Edited -> R.string.library_sort_edited
    SortKey.Name -> R.string.library_sort_name
    SortKey.Size -> R.string.library_sort_size
}

/** The words for the two directions follow the key: oldest or newest, A to Z, smallest or largest. */
fun directionLabels(key: SortKey): Pair<Int, Int> = when (key) {
    SortKey.Edited -> R.string.library_order_oldest to R.string.library_order_newest
    SortKey.Name -> R.string.library_order_az to R.string.library_order_za
    SortKey.Size -> R.string.library_order_smallest to R.string.library_order_largest
}

/**
 * One column, three levels: sort by, order, group by, and the cover size last. Every choice applies at once,
 * so the popover has no buttons. The size slider is off in list view, where covers have one size.
 */
@Composable
fun SortMenu(expanded: Boolean, onDismiss: () -> Unit, view: LibraryView, onChange: (LibraryView) -> Unit) {
    GraphoPopover(expanded = expanded, onDismiss = onDismiss, narrow = true) {
        GraphoMenuLabel(stringResource(R.string.library_sort_by))
        for (key in SortKey.entries) {
            GraphoMenuItem(
                stringResource(sortLabel(key)),
                checked = view.sortKey == key,
                onClick = { onChange(view.copy(sortKey = key)) },
            )
        }
        val (ascending, descending) = directionLabels(view.sortKey)
        Section(R.string.library_order) {
            SegmentedControl(
                listOf(SortDir.Asc to stringResource(ascending), SortDir.Desc to stringResource(descending)),
                view.sortDir,
                { onChange(view.copy(sortDir = it)) },
                size = SegmentSize.Small,
            )
        }
        Section(R.string.library_group_by) {
            SegmentedControl(
                listOf(
                    GroupBy.None to stringResource(R.string.library_group_none),
                    GroupBy.Date to stringResource(R.string.library_group_date),
                    GroupBy.Type to stringResource(R.string.library_group_type),
                    GroupBy.Letter to stringResource(R.string.library_group_letter),
                ),
                view.group,
                { onChange(view.copy(group = it)) },
                size = SegmentSize.Small,
            )
        }
        val sizeLabel = stringResource(R.string.library_cover_size)
        GraphoMenuLabel(sizeLabel)
        GraphoSlider(
            value = view.coverSize.toFloat(),
            onValueChange = { onChange(view.copy(coverSize = it.toInt())) },
            label = sizeLabel,
            valueRange = 0f..100f,
            enabled = view.mode == ViewMode.Grid,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
        )
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
        GraphoMenuLabel(stringResource(title))
        Column(Modifier.padding(horizontal = GraphoSpace.spaceXs)) { content() }
    }
}
