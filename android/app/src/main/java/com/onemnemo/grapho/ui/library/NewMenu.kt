package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoMenuLabel
import com.onemnemo.grapho.ui.components.MenuPlate

/** What New can start. Each one maps to a sheet or a file picker in the screen. */
enum class NewKind { Notebook, Canvas, Folder, Pdf, File }

/**
 * What you can make, then what you can bring in. Both groups share one row shape so icons and labels line up.
 * The import rows are quieter by their bare plate, not by smaller text.
 */
@Composable
fun NewMenu(expanded: Boolean, onDismiss: () -> Unit, onChoose: (NewKind) -> Unit) {
    fun row(kind: NewKind): () -> Unit = {
        onDismiss()
        onChoose(kind)
    }
    GraphoMenu(expanded = expanded, onDismiss = onDismiss) {
        GraphoMenuItem(
            stringResource(R.string.library_kind_notebook),
            icon = R.drawable.ic_notebook,
            plate = MenuPlate.Tinted,
            onClick = row(NewKind.Notebook),
        )
        GraphoMenuItem(
            stringResource(R.string.library_kind_canvas),
            icon = R.drawable.ic_canvas,
            plate = MenuPlate.Tinted,
            onClick = row(NewKind.Canvas),
        )
        GraphoMenuItem(
            stringResource(R.string.library_kind_folder),
            icon = R.drawable.ic_folder,
            plate = MenuPlate.Tinted,
            onClick = row(NewKind.Folder),
        )
        GraphoMenuDivider()
        GraphoMenuLabel(stringResource(R.string.library_import))
        GraphoMenuItem(
            stringResource(R.string.library_kind_pdf),
            icon = R.drawable.ic_pdf,
            plate = MenuPlate.Bare,
            onClick = row(NewKind.Pdf),
        )
        GraphoMenuItem(
            stringResource(R.string.library_import_file),
            icon = R.drawable.ic_file_down,
            plate = MenuPlate.Bare,
            onClick = row(NewKind.File),
        )
    }
}
