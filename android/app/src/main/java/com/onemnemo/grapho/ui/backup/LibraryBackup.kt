package com.onemnemo.grapho.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.onemnemo.grapho.ui.NotebookStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a backup run did. */
enum class BackupResult { Done, NothingNew, Empty, Failed }

/**
 * Backs the whole library up as one `.grapholib` file in the folder the person picked. Each run adds a new
 * file and the oldest ones beyond [KEEP] are removed. A run that finds nothing changed since the last one
 * does nothing.
 */
class LibraryBackup(private val context: Context, private val store: NotebookStore) {
    private val prefs = BackupPrefs(context)

    /** Takes the right to keep using the picked [folder] after the app restarts. */
    fun rememberFolder(folder: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(folder, flags)
        prefs.folder = folder
    }

    /** The folder's name for the settings row, or null if none is set. */
    fun folderName(): String? {
        val tree = prefs.folder ?: return null
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return try {
            val cols = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            context.contentResolver.query(doc, cols, null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    /** Whether the daily run should go now. */
    fun due(now: Long): Boolean = prefs.auto && prefs.folder != null && now - prefs.lastBackupMs >= DAY_MS

    /** Runs a backup. [force] copies even if nothing changed. Only one run goes at a time. */
    suspend fun run(force: Boolean): BackupResult = withContext(Dispatchers.IO) {
        val tree = prefs.folder ?: return@withContext BackupResult.Failed
        if (!running.compareAndSet(false, true)) return@withContext BackupResult.NothingNew
        try {
            val newest = store.list().maxOfOrNull { it.modifiedMs } ?: return@withContext BackupResult.Empty
            if (!force && newest <= prefs.lastCoveredMs) {
                prefs.lastBackupMs = System.currentTimeMillis()
                return@withContext BackupResult.NothingNew
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            val name = "$PREFIX$stamp.grapholib"
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val target = DocumentsContract.createDocument(context.contentResolver, parent, MIME, name)
                ?: return@withContext BackupResult.Failed
            try {
                store.exportWholeLibrary(target)
            } catch (e: Exception) {
                DocumentsContract.deleteDocument(context.contentResolver, target)
                return@withContext BackupResult.Failed
            }
            prefs.lastBackupMs = System.currentTimeMillis()
            prefs.lastCoveredMs = newest
            prune(tree)
            BackupResult.Done
        } catch (e: Exception) {
            BackupResult.Failed
        } finally {
            running.set(false)
        }
    }

    /** Removes the oldest backup files, keeping the newest [KEEP]. Files that are not ours are left alone. */
    private fun prune(tree: Uri) {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
        val doc = DocumentsContract.Document.COLUMN_DOCUMENT_ID
        val cols = arrayOf(doc, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val ours = ArrayList<Pair<String, String>>()
        context.contentResolver.query(children, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.startsWith(PREFIX) && name.endsWith(".grapholib")) ours.add(c.getString(0) to name)
            }
        }
        // The names carry the date, so sorting them sorts by age.
        for ((id, _) in ours.sortedByDescending { it.second }.drop(KEEP)) {
            try {
                val old = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                DocumentsContract.deleteDocument(context.contentResolver, old)
            } catch (e: Exception) {
                // An old copy that stays is harmless.
            }
        }
    }

    companion object {
        const val KEEP = 5
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val PREFIX = "grapho-backup-"
        private const val MIME = "application/octet-stream"
        private val running = AtomicBoolean(false)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Runs the daily backup in the background if it is on and due. It outlives the screen. */
        fun runIfDue(context: Context, store: NotebookStore) {
            val backup = LibraryBackup(context.applicationContext, store)
            if (backup.due(System.currentTimeMillis())) scope.launch { backup.run(force = false) }
        }
    }
}
