package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSheet
import com.onemnemo.grapho.ui.components.GraphoTextField

/**
 * New folder: a name and the Location, which starts at [parentId]. A folder has no cover or colour of its own
 * yet, so there is no look to pick. Create needs a name.
 */
@Composable
fun FolderSheet(
    targets: List<MoveTarget>,
    parentId: String?,
    onCreate: (name: String, parentId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var parent by remember { mutableStateOf(parentId) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val ready = name.text.isNotBlank()
    val create = { if (ready) onCreate(name.text.trim(), parent) }
    GraphoSheet(
        title = stringResource(R.string.library_new_folder),
        onDismiss = onDismiss,
        dismissOnScrim = name.text.isEmpty(),
        footer = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(R.string.library_create_folder),
                onClick = create,
                size = ButtonSize.Large,
                enabled = ready,
            )
        },
    ) {
        GraphoTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.library_folder_name),
            focusRequester = focus,
            onDone = create,
        )
        if (targets.size > 1) LocationRow(targets, parent) { parent = it }
    }
}
