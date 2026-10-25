package com.onemnemo.grapho.ui.note

import android.net.Uri
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
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import kotlinx.coroutines.launch

/** Picks a PDF and inserts its pages after the page in view. [busy] is true while the pages are being added. */
@Stable
class PdfInserter {
    var busy by mutableStateOf(false)
        internal set

    internal var launchPicker: () -> Unit = {}

    fun pick() {
        if (!busy) launchPicker()
    }
}

/** [view] is read when the picker returns, since the canvas may have been made again by then. */
@Composable
fun rememberPdfInserter(
    session: NoteSession,
    view: () -> NoteCanvasView?,
    notices: Notices,
    onEdited: () -> Unit,
): PdfInserter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inserter = remember { PdfInserter() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val canvas = view()
        if (uri != null && canvas != null) {
            val at = canvas.currentPage() + 1
            inserter.busy = true
            scope.launch {
                try {
                    val added = session.insertPdf(context, uri, at)
                    if (added > 0) {
                        view()?.structureChanged(at)
                        onEdited()
                        notices.show(context.resources.getQuantityString(R.plurals.note_pdf_pages_added, added, added))
                    } else {
                        notices.show(context.getString(R.string.note_no_pages_added))
                    }
                } catch (e: Exception) {
                    notices.show(e.userMessage(context, R.string.note_insert_pdf_failed), isError = true)
                } finally {
                    inserter.busy = false
                }
            }
        }
    }
    inserter.launchPicker = { launcher.launch(arrayOf("application/pdf")) }
    return inserter
}
