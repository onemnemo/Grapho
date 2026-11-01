package com.onemnemo.grapho.ui.library

import android.content.Context
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
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.LibraryPackage
import com.onemnemo.grapho.note.PackageReport
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * The file pickers of the library: import a notebook, a library package or a PDF, export a notebook as .grapho or
 * PDF, and export a choice of folders and notebooks as one library package.
 */
@Stable
class LibraryFiles(
    val importNotebook: () -> Unit,
    val importPdf: () -> Unit,
    val export: (NotebookEntry) -> Unit,
    val exportPdf: (NotebookEntry) -> Unit,
    /** Exports the folders and notebooks, named by the one folder when that is all there is. */
    val exportPackage: (folders: List<LibraryFolder>, entries: List<NotebookEntry>) -> Unit,
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
    var packageTarget by remember { mutableStateOf<Pair<List<String>, List<NotebookEntry>>?>(null) }
    var importingPdf by remember { mutableStateOf(false) }
    var exportingPdf by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var exportingPackage by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            importing = true
            scope.launch {
                try {
                    val report = store.import(uri, folderId)
                    reload()
                    val text = if (report.folders == 0 && report.items == 1 && report.skipped == 0) {
                        context.getString(R.string.library_imported)
                    } else {
                        context.getString(R.string.library_package_imported, packageCounts(context, report))
                    }
                    notices.show(withSkipped(context, text, report))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.library_import_failed), isError = true)
                } finally {
                    importing = false
                }
            }
        }
    }
    val packageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val target = packageTarget
        packageTarget = null
        if (uri != null && target != null) {
            exportingPackage = true
            scope.launch {
                try {
                    val report = store.exportPackage(target.first, target.second, uri)
                    val text = context.getString(R.string.library_package_exported, packageCounts(context, report))
                    notices.show(withSkipped(context, text, report))
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.library_package_export_failed), isError = true)
                } finally {
                    exportingPackage = false
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
        exportPackage = { folders, entries ->
            packageTarget = folders.map { it.id } to entries
            val name = folders.singleOrNull()?.takeIf { entries.isEmpty() }?.name
                ?: context.getString(R.string.library_package_name, LocalDate.now().toString())
            packageLauncher.launch("${ExportPlan.safeName(name, untitled)}${LibraryPackage.EXTENSION}")
        },
        busy = when {
            importingPdf -> R.string.library_importing_pdf
            importing -> R.string.library_importing
            exportingPdf || exportingPackage -> R.string.note_exporting
            else -> null
        },
    )
}

/** Such as "3 notebooks, 2 folders", leaving out the folders when there are none. */
private fun packageCounts(context: Context, report: PackageReport): String {
    val res = context.resources
    val items = res.getQuantityString(R.plurals.library_package_notebooks, report.items, report.items)
    if (report.folders == 0) return items
    val folders = res.getQuantityString(R.plurals.library_package_folders, report.folders, report.folders)
    return context.getString(R.string.library_package_counts, items, folders)
}

/** Adds how many notebooks could not be read, when any. */
private fun withSkipped(context: Context, text: String, report: PackageReport): String {
    if (report.skipped == 0) return text
    val skipped = context.resources.getQuantityString(R.plurals.library_package_skipped, report.skipped, report.skipped)
    return "$text. $skipped"
}
