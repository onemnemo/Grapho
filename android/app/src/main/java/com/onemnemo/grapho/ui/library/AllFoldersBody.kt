package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val BottomRoom = 150.dp

/**
 * Every folder as a tree, each under the folder it is in, with Recently deleted at the end. A tap opens the
 * folder. The dots, long press and selection work as they do on Home.
 */
@Composable
fun AllFoldersBody(
    folders: List<LibraryFolder>,
    entries: List<NotebookEntry>,
    deletedCount: Int,
    picks: Picks,
    gutter: Dp,
    actionsFor: (LibraryFolder) -> FolderActions,
    onOpen: (LibraryFolder) -> Unit,
    onPick: (LibraryFolder) -> Unit,
    onNewFolder: () -> Unit,
    onDeleted: () -> Unit,
) {
    val tree = remember(folders) { folderTree(folders) }
    val side = gutter - GraphoSpace.spaceMd
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = side, end = side, bottom = BottomRoom),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
    ) {
        if (tree.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LibraryEmpty(EmptyKind.Folders, onPrimary = onNewFolder, onSecondary = null)
                }
            }
        }
        items(tree, key = { it.folder.id }) { node ->
            FolderRow(
                node = node,
                counts = folderCounts(node.folder.id, entries, folders),
                selecting = picks.selecting,
                selected = picks.hasKey(folderKey(node.folder.id)),
                actions = actionsFor(node.folder),
                onClick = { if (picks.selecting) onPick(node.folder) else onOpen(node.folder) },
                onLongClick = { onPick(node.folder) },
            )
        }
        item(key = "deleted") {
            FieldRow(
                label = stringResource(R.string.library_deleted),
                modifier = Modifier.padding(top = GraphoSpace.spaceLg),
                value = if (deletedCount > 0) deletedCount.toString() else null,
                onClick = onDeleted,
            )
        }
    }
}
