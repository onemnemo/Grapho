package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem

/** What the menu of one folder can do. [moveTargets] is only called when Move to opens. */
class FolderActions(
    val onOpen: () -> Unit,
    val onRename: () -> Unit,
    val onSelect: () -> Unit,
    val onDelete: () -> Unit,
    val moveTargets: () -> List<MoveTarget>,
    val onMoveTo: (String?) -> Unit,
)

/** The menu behind a folder's dots. Like a note's menu, Move to swaps the rows for the list of places. */
@Composable
fun FolderMenu(expanded: Boolean, onDismiss: () -> Unit, actions: FolderActions) {
    var moving by remember { mutableStateOf(false) }
    val dismiss = {
        moving = false
        onDismiss()
    }
    fun run(action: () -> Unit): () -> Unit = {
        dismiss()
        action()
    }
    GraphoMenu(expanded = expanded, onDismiss = dismiss) {
        if (moving) {
            MovePage(actions.moveTargets(), onBack = { moving = false }) { id ->
                dismiss()
                actions.onMoveTo(id)
            }
        } else {
            GraphoMenuItem(
                stringResource(R.string.library_open),
                icon = R.drawable.ic_folder,
                onClick = run(actions.onOpen),
            )
            GraphoMenuItem(
                stringResource(R.string.note_rename),
                icon = R.drawable.ic_pencil,
                onClick = run(actions.onRename),
            )
            GraphoMenuItem(
                stringResource(R.string.library_move_to),
                icon = R.drawable.ic_folder_move,
                onClick = { moving = true },
            )
            GraphoMenuItem(
                stringResource(R.string.library_select),
                icon = R.drawable.ic_select,
                onClick = run(actions.onSelect),
            )
            GraphoMenuDivider()
            GraphoMenuItem(
                stringResource(R.string.library_delete_folder),
                icon = R.drawable.ic_trash,
                danger = true,
                onClick = run(actions.onDelete),
            )
        }
    }
}

/** The dots button of a folder, with its menu. */
@Composable
fun FolderMoreButton(actions: FolderActions, modifier: Modifier = Modifier, onMedia: Boolean = false) {
    MoreButton(modifier, onMedia) { open, close ->
        FolderMenu(expanded = open, onDismiss = close, actions = actions)
    }
}
