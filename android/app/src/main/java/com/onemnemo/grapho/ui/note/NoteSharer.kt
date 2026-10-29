package com.onemnemo.grapho.ui.note

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ExportPlan
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import kotlinx.coroutines.launch

/** Shares the note as a PDF through the system share sheet. [busy] is true while the PDF is being written. */
@Stable
class NoteSharer {
    var busy by mutableStateOf(false)
        internal set

    internal var start: () -> Unit = {}

    fun share() {
        if (!busy) start()
    }
}

/** Cache folder the shared copies are written to, matching the share path in `file_paths.xml`. */
private const val SHARE_DIR = "share"

/**
 * Writes the whole note to a PDF in the cache and hands it to the share sheet. Copies from earlier shares are
 * cleared first, so the cache never holds more than the one being sent.
 */
@Composable
fun rememberNoteSharer(session: NoteSession, notices: Notices, title: String): NoteSharer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharer = remember { NoteSharer() }
    val untitled = stringResource(R.string.note_untitled)
    val chooser = stringResource(R.string.note_share)
    sharer.start = {
        sharer.busy = true
        scope.launch {
            try {
                val dir = File(context.cacheDir, SHARE_DIR)
                val file = session.withNotebook { notebook ->
                    dir.deleteRecursively()
                    dir.mkdirs()
                    File(dir, ExportPlan.pdfName(title, untitled)).also { notebook.exportPdf(it, true, null) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, ExportPlan.safeName(title, untitled))
                    clipData = ClipData.newRawUri(file.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, chooser))
            } catch (e: ActivityNotFoundException) {
                notices.show(context.getString(R.string.note_share_failed), isError = true)
            } catch (e: Exception) {
                notices.show(e.userMessage(context, R.string.note_share_failed), isError = true)
            } finally {
                sharer.busy = false
            }
        }
    }
    return sharer
}
