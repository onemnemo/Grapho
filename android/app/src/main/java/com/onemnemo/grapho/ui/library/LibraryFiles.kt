package com.onemnemo.grapho.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ExportPlan
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import kotlinx.coroutines.launch

/** The file pickers of the library: import a notebook or a PDF, export a notebook as .grapho or PDF. */
@Stable
class LibraryFiles(
    val importNotebook: () -> Unit,
    val importPdf: () -> Unit,
    val export: (NotebookEntry) -> Unit,
    val exportPdf: (NotebookEntry) -> Unit,
    /** The text of the busy overlay while a PDF is read or written, or null. */
    @StringRes val busy: Int?,
)

/** Imports go into [folderId], or the top level when it is null. */
@Composable
fun rememberLibraryFiles(
    store: NotebookStore,
    notices: Notices,
    onOpen: (File) -> Unit,
    reload: () -> Unit,
    folderId: String? = null,
): LibraryFiles {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val untitled = context.getString(R.string.note_untitled)
    var exportTarget by remember { mutableStateOf<NotebookEntry?>(null) }
    var exportPdfTarget by remember { mutableStateOf<NotebookEntry?>(null) }
    var importingPdf by remember { mutableStateOf(false) }
    var exportingPdf by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    store.import(uri, folderId)
                    reload()
                    notices.show(context.getString(R.string.library_imported))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.library_import_failed), isError = true)
                }
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            importingPdf = true
            scope.launch {
                try {
                    onOpen(store.importPdf(uri, folderId))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.library_import_pdf_failed), isError = true)
                } finally {
                    importingPdf = false
                }
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val target = exportTarget
        exportTarget = null
        if (uri != null && target != null) {
            scope.launch {
                try {
                    store.export(target.file, uri)
                    notices.show(context.getString(R.string.note_exported))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.library_export_failed), isError = true)
                }
            }
        }
    }
    val exportPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri: Uri? ->
        val target = exportPdfTarget
        exportPdfTarget = null
        if (uri != null && target != null) {
            exportingPdf = true
            scope.launch {
                try {
                    store.exportPdf(target.file, uri)
                    notices.show(context.getString(R.string.note_exported))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.note_export_failed), isError = true)
                } finally {
                    exportingPdf = false
                }
            }
        }
    }

    return LibraryFiles(
        importNotebook = { importLauncher.launch(arrayOf("*/*")) },
        importPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
        export = {
            exportTarget = it
            exportLauncher.launch("${ExportPlan.safeName(it.title, untitled)}.grapho")
        },
        exportPdf = {
            exportPdfTarget = it
            exportPdfLauncher.launch("${ExportPlan.safeName(it.title, untitled)}.pdf")
        },
        busy = when {
            importingPdf -> R.string.library_importing_pdf
            exportingPdf -> R.string.note_exporting
            else -> null
        },
    )
}
