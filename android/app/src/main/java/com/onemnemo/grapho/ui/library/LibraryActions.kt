package com.onemnemo.grapho.ui.library

import android.content.Context
import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.RestoreMode
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The changes the library screens make, each with the toast that goes with it. A change that can be reversed
 * happens at once and its toast carries Undo, see [trash] and [move]. [scope] should outlive the screen, so an
 * Undo still works after a notebook was opened. [reload] reads the library again.
 */
class LibraryActions(
    context: Context,
    private val store: NotebookStore,
    private val notices: Notices,
    private val scope: CoroutineScope,
    private val reload: () -> Unit,
) {
    private val context: Context = context.applicationContext

    private fun failed(e: Exception, @StringRes fallback: Int) =
        notices.show(e.userMessage(context, fallback), isError = true)

    private fun undoLabel(): String = context.getString(R.string.note_undo)

    /** Sends notebooks and folders to Recently deleted and offers Undo. [onDone] runs when that is done. */
    fun trash(entries: List<NotebookEntry>, folders: List<LibraryFolder>, onDone: () -> Unit = {}) {
        scope.launch {
            val gone = ArrayList<NotebookEntry>()
            val goneFolders = ArrayList<LibraryFolder>()
            val failure: Exception? = try {
                for (folder in folders) {
                    store.deleteFolder(folder.id)
                    goneFolders.add(folder)
                }
                for (entry in entries) {
                    store.delete(entry.file)
                    gone.add(entry)
                }
                null
            } catch (e: Exception) {
                e
            }
            onDone()
            reload()
            if (failure != null) failed(failure, R.string.library_delete_failed)
            if (gone.isNotEmpty() || goneFolders.isNotEmpty()) {
                notices.show(
                    deletedText(gone, goneFolders),
                    actionLabel = undoLabel(),
                    onAction = { undoTrash(gone, goneFolders) },
                )
            }
        }
    }

    private fun deletedText(entries: List<NotebookEntry>, folders: List<LibraryFolder>): String {
        val count = entries.size + folders.size
        return when {
            entries.size == 1 && folders.isEmpty() ->
                context.getString(R.string.library_deleted_one, entries.first().nameIn(context))

            folders.size == 1 && entries.isEmpty() ->
                context.getString(R.string.library_deleted_one, folders.first().name)

            else -> context.resources.getQuantityString(R.plurals.library_deleted_many, count, count)
        }
    }

    private fun undoTrash(entries: List<NotebookEntry>, folders: List<LibraryFolder>) {
        scope.launch {
            val failure: Exception? = try {
                for (folder in folders) store.restoreFolder(folder.id)
                for (entry in entries) store.restore(entry)
                null
            } catch (e: Exception) {
                e
            }
            reload()
            if (failure != null) failed(failure, R.string.library_save_failed)
        }
    }

    /** Moves notebooks and folders to [target] (null for the top level), named [targetName], and offers Undo. */
    fun move(entries: List<NotebookEntry>, folders: List<LibraryFolder>, target: String?, targetName: String) {
        scope.launch {
            val failure: Exception? = try {
                if (entries.isNotEmpty()) store.moveItems(entries, target)
                for (folder in folders) store.moveFolder(folder.id, target)
                null
            } catch (e: Exception) {
                e
            }
            reload()
            if (failure != null) {
                failed(failure, R.string.library_save_failed)
            } else {
                notices.show(
                    context.getString(R.string.library_moved_to, targetName),
                    actionLabel = undoLabel(),
                    onAction = { undoMove(entries, folders) },
                )
            }
        }
    }

    private fun undoMove(entries: List<NotebookEntry>, folders: List<LibraryFolder>) {
        scope.launch {
            val failure: Exception? = try {
                for ((from, group) in entries.groupBy { it.folderId }) store.moveItems(group, from)
                for (folder in folders) store.moveFolder(folder.id, folder.parentId)
                null
            } catch (e: Exception) {
                e
            }
            reload()
            if (failure != null) failed(failure, R.string.library_save_failed)
        }
    }

    fun setStarred(entries: List<NotebookEntry>, starred: Boolean) {
        scope.launch {
            try {
                store.setStarred(entries, starred)
                reload()
            } catch (e: Exception) {
                failed(e, R.string.library_save_failed)
            }
        }
    }

    fun createFolder(parentId: String?, name: String) {
        scope.launch {
            try {
                store.createFolder(parentId, name)
                reload()
            } catch (e: Exception) {
                failed(e, R.string.library_save_failed)
            }
        }
    }

    fun renameFolder(id: String, name: String) {
        scope.launch {
            try {
                store.renameFolder(id, name)
                reload()
            } catch (e: Exception) {
                failed(e, R.string.library_rename_failed)
            }
        }
    }

    /**
     * Brings a deleted notebook back. When its folder is in Recently deleted too, [ask] is called so the person
     * can choose, and [restore] finishes the job with their answer.
     */
    fun restoreEntry(entry: NotebookEntry, ask: (NotebookEntry) -> Unit) {
        scope.launch {
            try {
                if (store.restoreNeedsChoice(entry)) ask(entry) else restoreNow(entry, RestoreMode.Auto)
            } catch (e: Exception) {
                failed(e, R.string.library_save_failed)
            }
        }
    }

    fun restore(entry: NotebookEntry, mode: RestoreMode) {
        scope.launch {
            try {
                restoreNow(entry, mode)
            } catch (e: Exception) {
                failed(e, R.string.library_save_failed)
            }
        }
    }

    private suspend fun restoreNow(entry: NotebookEntry, mode: RestoreMode) {
        store.restore(entry, mode)
        reload()
        notices.show(context.getString(R.string.library_restored_one, entry.nameIn(context)))
    }

    fun restoreFolder(folder: LibraryFolder) {
        scope.launch {
            try {
                store.restoreFolder(folder.id)
                reload()
                notices.show(context.getString(R.string.library_restored_one, folder.name))
            } catch (e: Exception) {
                failed(e, R.string.library_save_failed)
            }
        }
    }

    /** Restores everything in [rows]. Folders come first, so a notebook goes back to its folder. */
    fun restoreAll(rows: List<TrashRow>) {
        scope.launch {
            val failure: Exception? = try {
                for (row in rows) if (row is TrashRow.Folder) store.restoreFolder(row.folder.id)
                for (row in rows) if (row is TrashRow.Item) store.restore(row.entry)
                null
            } catch (e: Exception) {
                e
            }
            reload()
            if (failure != null) {
                failed(failure, R.string.library_save_failed)
            } else {
                val count = trashCount(rows)
                notices.show(context.resources.getQuantityString(R.plurals.library_restored_many, count, count))
            }
        }
    }

    /** Removes [rows] and their files for good. The person has been asked. */
    fun deleteForever(rows: List<TrashRow>) {
        scope.launch {
            val failure: Exception? = try {
                for (row in rows) {
                    when (row) {
                        is TrashRow.Folder -> store.deleteFolderForever(row.folder.id)
                        is TrashRow.Item -> store.deleteForever(row.entry)
                    }
                }
                null
            } catch (e: Exception) {
                e
            }
            reload()
            if (failure != null) failed(failure, R.string.library_delete_failed)
        }
    }

    /** Empties Recently deleted for good. The person has been asked. */
    fun deleteAll() {
        scope.launch {
            try {
                store.emptyTrash()
            } catch (e: Exception) {
                failed(e, R.string.library_delete_failed)
            }
            reload()
        }
    }
}
