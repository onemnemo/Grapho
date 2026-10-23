package com.onemnemo.grapho.ui.note

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.NotebookInfo
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.NotebookIo
import com.onemnemo.grapho.ui.StoreException
import com.onemnemo.grapho.ui.asStoreException
import com.onemnemo.grapho.ui.copyFileToUri
import com.onemnemo.grapho.ui.copyUriToCache
import com.onemnemo.grapho.ui.displayName
import com.onemnemo.grapho.ui.spacingFor
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One open note: loads it, saves it after edits and closes it.
 *
 * Every native call that touches the file (open, save, export, close) runs on
 * [NotebookIo], which runs one task at a time. So a save can never run after
 * the close, and a note opened again right after leaving waits for the
 * previous one to be saved and closed.
 */
class NoteSession(private val file: File) {
    private val scope = CoroutineScope(SupervisorJob() + NotebookIo)
    private val main = Handler(Looper.getMainLooper())

    /** Set once the file is open, and cleared as soon as the person leaves. */
    var notebook by mutableStateOf<Notebook?>(null)
        private set
    var info by mutableStateOf<NotebookInfo?>(null)
        private set

    @get:StringRes
    var openError by mutableStateOf<Int?>(null)
        private set

    /** True once the canvas has drawn the first page. Until then the loading screen stays up. */
    var canvasShown by mutableStateOf(false)
        private set

    private val startedAt = SystemClock.elapsedRealtime()
    private var openedAt = 0L

    /** Called by the canvas after its first page is drawn. */
    fun onCanvasShown() {
        if (canvasShown) return
        canvasShown = true
        val now = SystemClock.elapsedRealtime()
        Log.i("GraphoOpen", "file open ${openedAt - startedAt} ms, first page ${now - openedAt} ms after that")
    }

    /** Shown until the next save works or the person dismisses it. */
    var saveError by mutableStateOf<Int?>(null)

    // Touched only on NotebookIo.
    private var handle: Notebook? = null
    private var savedRevision = 0L

    // Touched only on the main thread.
    private var debounce: Job? = null
    private var dirtySince = 0L
    private var finished = false

    fun open() {
        scope.launch {
            try {
                val n = Notebook.open(file)
                handle = n
                savedRevision = n.revision
                post {
                    if (!finished) {
                        openedAt = SystemClock.elapsedRealtime()
                        notebook = n
                        refreshInfo()
                    }
                }
            } catch (e: Exception) {
                post { openError = R.string.note_open_failed }
            }
        }
    }

    fun refreshInfo() {
        val n = notebook ?: return
        try {
            info = n.info()
        } catch (e: Exception) {
            // The note was closed while this was queued.
        }
    }

    /** Called by the canvas after any edit. */
    fun onEdited() {
        refreshInfo()
        scheduleSave()
    }

    fun rename(title: String) {
        notebook?.setTitle(title)
        onEdited()
    }

    fun setPaper(pattern: Int) {
        val n = notebook ?: return
        val color = info?.background?.color ?: 0xFFFFFFFF.toInt()
        n.setBackground(PageBackground(pattern, spacingFor(pattern), color))
        onEdited()
    }

    fun setPaperColor(argb: Int) {
        val n = notebook ?: return
        val bg = info?.background ?: return
        n.setBackground(bg.copy(color = argb))
        onEdited()
    }

    /** Saves about 1.5 s after the last change, and at least every 15 s while writing. */
    fun scheduleSave() {
        if (finished) return
        val now = System.currentTimeMillis()
        if (debounce?.isActive != true) dirtySince = now
        val wait = if (now - dirtySince > 15_000) 0L else 1_500L
        debounce?.cancel()
        debounce = scope.launch {
            delay(wait)
            saveIfChanged()
        }
    }

    /** Saves now, without waiting. For when the app goes to the background. */
    fun saveSoon() {
        if (finished) return
        debounce?.cancel()
        scope.launch { saveIfChanged() }
    }

    /**
     * Inserts the pages of the PDF at [uri] at page index [at] and returns how many were added. Runs off the main
     * thread, so the caller keeps touches away from the canvas until it returns. Throws [StoreException].
     */
    suspend fun insertPdf(context: Context, uri: Uri, at: Int): Int = withContext(NotebookIo) {
        val n = handle ?: throw StoreException(R.string.store_note_closed)
        val name = displayName(context, uri)
        val pdf = copyUriToCache(context, uri)
        try {
            n.insertPdf(at, pdf, name)
        } catch (e: IOException) {
            throw e.asStoreException(R.string.note_insert_pdf_failed)
        } finally {
            pdf.delete()
        }
    }

    /** Runs [block] with the open note on [NotebookIo], where nothing can close it meanwhile. Throws [StoreException]. */
    suspend fun <T> withNotebook(block: (Notebook) -> T): T = withContext(NotebookIo) {
        block(handle ?: throw StoreException(R.string.store_note_closed))
    }

    /** Writes a copy to [uri] for export. Throws [StoreException]. */
    suspend fun exportTo(context: Context, uri: Uri) {
        withContext(NotebookIo) {
            val n = handle ?: throw StoreException(R.string.store_note_closed)
            val temp = File(context.cacheDir, "export-${UUID.randomUUID()}.grapho")
            try {
                try {
                    n.save(temp)
                } catch (e: Exception) {
                    throw StoreException(R.string.note_export_failed, e)
                }
                copyFileToUri(context, temp, uri)
            } finally {
                temp.delete()
            }
        }
    }

    /**
     * Leaves the note: one last save, then close. [onClosed] runs on the main
     * thread with an error message if that last save failed.
     */
    fun finish(onClosed: (Int?) -> Unit) {
        finished = true
        notebook = null
        debounce?.cancel()
        scope.launch {
            val error = saveIfChanged()
            handle?.close()
            handle = null
            post { onClosed(error) }
            scope.cancel()
        }
    }

    /** Returns the string resource of an error message, or null. Runs on [NotebookIo]. */
    @StringRes
    private fun saveIfChanged(): Int? {
        val n = handle ?: return null
        val revision = n.revision
        if (revision == savedRevision) return null
        return try {
            n.save()
            savedRevision = revision
            post { saveError = null }
            null
        } catch (e: Exception) {
            val message = R.string.note_save_failed
            post { saveError = message }
            message
        }
    }

    private fun post(block: () -> Unit) {
        main.post { block() }
    }
}
