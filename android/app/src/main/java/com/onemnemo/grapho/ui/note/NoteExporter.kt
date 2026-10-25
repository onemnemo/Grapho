package com.onemnemo.grapho.ui.note

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ExportPlan
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import kotlinx.coroutines.launch

/** Opens the export dialog and runs what it asks for. [busy] is true while a file is being written. */
@Stable
class NoteExporter {
    var busy by mutableStateOf(false)
        internal set

    internal var dialogOpen by mutableStateOf(false)

    fun open() {
        if (!busy) dialogOpen = true
    }
}

/**
 * The export dialog is asked for first, then the place to save to, and only then is anything written, so a
 * cancelled picker costs nothing. [view] is read when the dialog opens, for the page in view.
 */
@Composable
fun rememberNoteExporter(
    session: NoteSession,
    view: () -> NoteCanvasView?,
    notices: Notices,
    title: String,
): NoteExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { NoteExporter() }
    var pending by remember { mutableStateOf<ExportChoice?>(null) }
    val untitled = stringResource(R.string.note_untitled)

    fun run(work: suspend () -> Unit) {
        exporter.busy = true
        scope.launch {
            try {
                work()
                notices.show(context.getString(R.string.note_exported))
            } catch (e: Exception) {
                notices.show(e.userMessage(context, R.string.note_export_failed), isError = true)
            } finally {
                exporter.busy = false
            }
        }
    }

    val notebookLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        pending = null
        if (uri != null) run { session.exportTo(context, uri) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val choice = pending
        pending = null
        if (uri != null && choice != null) {
            run { session.withNotebook { NoteExports.pdf(context, it, uri, choice.backgrounds, choice.pages) } }
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        val choice = pending
        pending = null
        if (uri != null && choice != null) {
            run { session.withNotebook { NoteExports.image(context, it, uri, choice.backgrounds, choice.pages[0]) } }
        }
    }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val choice = pending
        pending = null
        if (uri != null && choice != null) {
            run {
                session.withNotebook {
                    NoteExports.images(context, it, uri, choice.backgrounds, choice.pages, title, untitled)
                }
            }
        }
    }

    val info = session.info
    if (exporter.dialogOpen && info != null) {
        val current = remember { view()?.currentPage() ?: 0 }
        ExportSheet(
            pageCount = info.pageIds.size,
            currentPage = current,
            infinite = info.infinite,
            onDismiss = { exporter.dialogOpen = false },
            onConfirm = { choice ->
                exporter.dialogOpen = false
                pending = choice
                val name = ExportPlan.safeName(title, untitled)
                when {
                    choice.format == ExportFormat.Notebook -> notebookLauncher.launch("$name.grapho")

                    choice.format == ExportFormat.Pdf -> pdfLauncher.launch("$name.pdf")

                    choice.pages.size == 1 ->
                        imageLauncher.launch(ExportPlan.imageName(title, choice.pages[0], untitled))

                    else -> folderLauncher.launch(null)
                }
            },
        )
    }
    return exporter
}
