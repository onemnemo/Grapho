package com.onemnemo.grapho.ui.settings

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.backup.BackupPrefs
import com.onemnemo.grapho.ui.backup.BackupResult
import com.onemnemo.grapho.ui.backup.LibraryBackup
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.GraphoSwitchRow
import com.onemnemo.grapho.ui.components.Notices
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
import kotlinx.coroutines.launch

/**
 * How the library opens: the same sort, grouping and view the library keeps, so a change here is the one the
 * library's own Sort menu makes. [onChanged] tells the library to read it again.
 */
@Composable
internal fun LibraryPage(store: NotebookStore, notices: Notices, onChanged: () -> Unit) {
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
    val scope = rememberCoroutineScope()
    val backup = remember(context) { LibraryBackup(context, store) }
    val backupPrefs = remember(context) { BackupPrefs(context) }
    var folderName by remember { mutableStateOf(backup.folderName()) }
    var auto by remember { mutableStateOf(backupPrefs.auto) }
    var last by remember { mutableStateOf(backupPrefs.lastBackupMs) }
    var running by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            backup.rememberFolder(uri)
            folderName = backup.folderName()
        }
    }
    SettingsPage {
        SettingsGroup(R.string.settings_backup) {
            FieldRow(
                stringResource(R.string.backup_folder),
                value = folderName ?: stringResource(R.string.backup_folder_none),
                onClick = { picker.launch(null) },
            )
            GraphoSwitchRow(
                text = stringResource(R.string.backup_auto),
                hint = stringResource(R.string.backup_auto_hint),
                checked = auto,
                enabled = folderName != null,
                onCheckedChange = {
                    auto = it
                    backupPrefs.auto = it
                },
                modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
            )
            FieldRow(
                stringResource(R.string.backup_now),
                hint = if (last == 0L) {
                    stringResource(R.string.backup_never)
                } else {
                    stringResource(R.string.backup_last, DateUtils.getRelativeTimeSpanString(last).toString())
                },
            ) {
                GraphoButton(
                    text = stringResource(R.string.backup_now),
                    onClick = {
                        running = true
                        scope.launch {
                            val result = backup.run(force = true)
                            running = false
                            last = backupPrefs.lastBackupMs
                            val message = when (result) {
                                BackupResult.Done -> R.string.backup_done
                                BackupResult.NothingNew -> R.string.backup_nothing_new
                                BackupResult.Empty -> R.string.backup_empty
                                BackupResult.Failed -> R.string.backup_failed
                            }
                            notices.show(context.getString(message), isError = result == BackupResult.Failed)
                        }
                    },
                    kind = ButtonKind.Secondary,
                    enabled = folderName != null && !running,
                )
            }
        }
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
