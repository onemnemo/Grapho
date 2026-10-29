package com.onemnemo.grapho.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.library.GroupBy
import com.onemnemo.grapho.ui.library.LibraryPrefs
import com.onemnemo.grapho.ui.library.LibraryView
import com.onemnemo.grapho.ui.library.SortDir
import com.onemnemo.grapho.ui.library.SortKey
import com.onemnemo.grapho.ui.library.ViewMode
import com.onemnemo.grapho.ui.library.directionLabels
import com.onemnemo.grapho.ui.library.sortLabel
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * How the library opens: the same sort, grouping and view the library keeps, so a change here is the one the
 * library's own Sort menu makes. [onChanged] tells the library to read it again.
 */
@Composable
internal fun LibraryPage(onChanged: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { LibraryPrefs(context) }
    var view by remember { mutableStateOf(prefs.loadView()) }
    fun change(next: LibraryView) {
        view = next
        prefs.saveView(next)
        onChanged()
    }
    val (ascending, descending) = directionLabels(view.sortKey)
    val sizeLabel = stringResource(R.string.library_cover_size)
    SettingsPage {
        SettingsGroup(R.string.settings_default_view) {
            FieldRow(stringResource(R.string.library_sort_by)) {
                SegmentedControl(
                    SortKey.entries.map { it to stringResource(sortLabel(it)) },
                    view.sortKey,
                    { change(view.copy(sortKey = it)) },
                )
            }
            FieldRow(stringResource(R.string.library_order)) {
                SegmentedControl(
                    listOf(SortDir.Asc to stringResource(ascending), SortDir.Desc to stringResource(descending)),
                    view.sortDir,
                    { change(view.copy(sortDir = it)) },
                )
            }
            FieldRow(stringResource(R.string.library_group_by)) {
                SegmentedControl(
                    listOf(
                        GroupBy.None to stringResource(R.string.library_group_none),
                        GroupBy.Date to stringResource(R.string.library_group_date),
                        GroupBy.Type to stringResource(R.string.library_group_type),
                        GroupBy.Letter to stringResource(R.string.library_group_letter),
                    ),
                    view.group,
                    { change(view.copy(group = it)) },
                )
            }
            FieldRow(stringResource(R.string.settings_view)) {
                SegmentedControl(
                    listOf(
                        ViewMode.Grid to stringResource(R.string.settings_view_grid),
                        ViewMode.List to stringResource(R.string.settings_view_list),
                    ),
                    view.mode,
                    { change(view.copy(mode = it)) },
                )
            }
            FieldRow(sizeLabel) {
                GraphoSlider(
                    value = view.coverSize.toFloat(),
                    onValueChange = { change(view.copy(coverSize = it.toInt())) },
                    label = sizeLabel,
                    valueRange = 0f..100f,
                    enabled = view.mode == ViewMode.Grid,
                    modifier = Modifier.padding(vertical = GraphoSpace.spaceXs),
                )
            }
        }
    }
}
