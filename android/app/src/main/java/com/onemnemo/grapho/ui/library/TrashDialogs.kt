package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoDialog
import com.onemnemo.grapho.ui.theme.Grapho

/**
 * Asks before something is removed for good. The danger button names the verb and how many things go, as in
 * "Delete 3 forever". [title] names what is asked about.
 */
@Composable
fun DeleteForeverDialog(title: String, count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val label = LocalContext.current.resources.getQuantityString(R.plurals.library_delete_forever_count, count, count)
    GraphoDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(label, onClick = onConfirm, kind = ButtonKind.Danger, size = ButtonSize.Large)
        },
    ) {
        Text(stringResource(R.string.delete_permanent), style = Grapho.type.body, color = Grapho.colors.textMuted)
    }
}

/**
 * A notebook whose folder is in Recently deleted too: bring the folder back with it, or put it in Library. The two
 * answers are full width buttons in the body, because three buttons in a row do not fit a narrow dialog.
 */
@Composable
fun RestoreChoiceDialog(
    entry: NotebookEntry,
    onWithFolder: () -> Unit,
    onToLibrary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    GraphoDialog(
        title = context.getString(R.string.library_restore_title, entry.nameIn(context)),
        onDismiss = onDismiss,
        actions = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
        },
    ) {
        Text(
            stringResource(R.string.library_restore_choice_text),
            style = Grapho.type.body,
            color = Grapho.colors.textMuted,
        )
        GraphoButton(
            stringResource(R.string.library_restore_with_folder),
            onClick = onWithFolder,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Large,
        )
        GraphoButton(
            stringResource(R.string.library_restore_to_library),
            onClick = onToLibrary,
            modifier = Modifier.fillMaxWidth(),
            kind = ButtonKind.Secondary,
            size = ButtonSize.Large,
        )
    }
}
