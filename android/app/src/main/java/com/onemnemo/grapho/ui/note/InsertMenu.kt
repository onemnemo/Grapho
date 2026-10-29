package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoMenuLabel

/**
 * Insert: pictures first, pages second. Opens under the parent it is declared in. An infinite canvas has no
 * pages, so that group is left out, and so is it when [pages] is off, for the Image tool on the bar. With the Image
 * tool on the bar, [images] is off and the pictures are left to it. Paste image shows only when the clipboard holds
 * a picture.
 */
@Composable
internal fun InsertMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    infinite: Boolean,
    actions: NoteActions,
    pages: Boolean = true,
    images: Boolean = true,
) {
    fun run(action: () -> Unit) {
        onDismiss()
        action()
    }
    GraphoMenu(expanded = expanded, onDismiss = onDismiss) {
        if (images) {
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
        }
        if (pages && !infinite) {
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

/** Whether Insert holds anything: with the pictures left to the Image tool, an infinite canvas has nothing else. */
internal fun insertHasItems(imageTool: Boolean, infinite: Boolean): Boolean = !imageTool || !infinite
