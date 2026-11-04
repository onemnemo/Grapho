package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoMenuLabel

/**
 * Insert, the one menu for adding to a note: pictures first, pages second. Opens under the parent it is declared
 * in, which is the Image tool when the bar has it, else the insert button or More. An infinite canvas has no pages,
 * so that group is left out. Paste image shows only when the clipboard holds a picture.
 */
@Composable
internal fun InsertMenu(expanded: Boolean, onDismiss: () -> Unit, infinite: Boolean, actions: NoteActions) {
    fun run(action: () -> Unit) {
        onDismiss()
        action()
    }
    // Not modal: a tap on another button of the bar closes it and does what that button does, in one tap.
    GraphoMenu(expanded = expanded, onDismiss = onDismiss, modal = false) {
        GraphoMenuLabel(stringResource(R.string.note_insert_group_image))
        GraphoMenuItem(
            stringResource(R.string.note_image_photo),
            icon = R.drawable.ic_image,
            onClick = { run(actions.onInsertPhoto) },
        )
        GraphoMenuItem(
            stringResource(R.string.note_image_take_photo),
            icon = R.drawable.ic_camera,
            onClick = { run(actions.onTakePhoto) },
        )
        if (actions.canPasteImage()) {
            GraphoMenuItem(
                stringResource(R.string.note_image_paste),
                icon = R.drawable.ic_clipboard,
                onClick = { run(actions.onPasteImage) },
            )
        }
        if (!infinite) {
            GraphoMenuLabel(stringResource(R.string.note_insert_group_pages))
            GraphoMenuItem(
                stringResource(R.string.note_add_blank_page),
                icon = R.drawable.ic_add_page,
                onClick = { run(actions.onAddBlankPage) },
            )
            GraphoMenuItem(
                stringResource(R.string.note_insert_pdf),
                icon = R.drawable.ic_pdf,
                onClick = { run(actions.onInsertPdf) },
            )
        }
    }
}
