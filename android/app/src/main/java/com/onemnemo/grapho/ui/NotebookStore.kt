package com.onemnemo.grapho.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.CoverRef
import com.onemnemo.grapho.note.Library
import com.onemnemo.grapho.note.LibraryErrors
import com.onemnemo.grapho.note.LibraryException
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.LibraryItem
import com.onemnemo.grapho.note.LibraryKind
import com.onemnemo.grapho.note.LibraryPackage
import com.onemnemo.grapho.note.MoveVerdict
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.NotebookInfo
import com.onemnemo.grapho.note.PackageReport
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.note.RestoreMode
import com.onemnemo.grapho.note.UserIOException
import com.onemnemo.grapho.ui.note.NoteExports
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

/**
 * All notebook file work runs here, one task at a time. That way a save can
 * never overlap a close, a rename or a delete of the same file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
val NotebookIo: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

/**
 * A failure that is fine to show to the person. [messageRes] is the text; [detail] replaces it when a lower layer
 * already worded the problem.
 */
class StoreException(
    @StringRes val messageRes: Int,
    cause: Throwable? = null,
    val detail: String? = null,
    val args: List<Any> = emptyList(),
) : Exception(detail, cause)

/** Keeps the worded failure of the lower layer, or falls back to [fallback] with the lower layer's own text. */
fun IOException.asStoreException(@StringRes fallback: Int): StoreException = if (this is UserIOException) {
    StoreException(
        messageRes,
        this,
        args = args,
    )
} else {
    StoreException(fallback, this, detail = message)
}

fun Throwable.userMessage(context: Context, @StringRes fallback: Int): String =
    (this as? StoreException)?.let { it.detail ?: context.getString(it.messageRes, *it.args.toTypedArray()) }
        ?: context.getString(fallback)

/** What the library shows for one notebook. */
data class NotebookEntry(
    val file: File,
    val title: String,
    val infinite: Boolean,
    /** ARGB, or null for the default cover. */
    val coverColor: Int?,
    val modifiedMs: Long,
    val pages: Int,
    /** True when some page shows an imported PDF page. */
    val hasPdf: Boolean,
    /** The paper, which a canvas tile draws as its own look. */
    val background: PageBackground,
    /** The library id of the item, 26 characters. */
    val id: String = "",
    /** The folder the notebook is in, or null for the top level. */
    val folderId: String? = null,
    val starred: Boolean = false,
    /** The cover content ref, such as `color:#RRGGBB`, or null for the default cover. */
    val coverRef: String? = null,
    /** When the notebook went to Trash, or null while it is live. */
    val deletedAt: Long? = null,
    /** The folder a deleted notebook was in when it was deleted. */
    val deletedFromFolderId: String? = null,
    /** Shared with the folder that went to Trash with this notebook, or null if it was deleted alone. */
    val deleteBatchId: String? = null,
)

/** Everything the library screens show, read in one pass. */
data class LibrarySnapshot(
    /** Live notebooks in every folder. */
    val entries: List<NotebookEntry>,
    /** Every live folder, parents before their children, siblings in their manual order. */
    val folders: List<LibraryFolder>,
    /** Notebooks in Trash, the one deleted last first. */
    val trash: List<NotebookEntry>,
    /** The deleted folders that are the roots of what was deleted, oldest first. */
    val trashFolders: List<LibraryFolder>,
)

/** What the new notebook sheet produces. [paper] is 0 A4, 1 A5, 2 Letter. */
data class NewNotebookSpec(
    val title: String,
    val infinite: Boolean,
    val pattern: Int,
    val paper: Int,
    val landscape: Boolean,
    val coverColor: Int,
    val paperColor: Int = PaperColors.WHITE.argb,
    /** Line, grid or dot spacing in millimetres. */
    val spacingMm: Float = spacingFor(pattern),
)

/** Default line, grid or dot spacing in millimetres. */
fun spacingFor(pattern: Int): Float = if (pattern == 2) 5f else 8f

/** Copies [file] into the document the person picked. */
fun copyFileToUri(context: Context, file: File, uri: Uri) {
    try {
        val out = context.contentResolver.openOutputStream(uri, "w")
            ?: throw StoreException(R.string.store_write_location)
        out.use { o -> file.inputStream().use { it.copyTo(o) } }
    } catch (e: StoreException) {
        throw e
    } catch (e: Exception) {
        throw StoreException(R.string.store_write_location, e)
    }
}

class NotebookStore(context: Context) {
    private val appContext = context.applicationContext
    private val storage = LibraryStorage(appContext.filesDir)

    private val dir: File
        get() = storage.notebooks

    /** Live notebooks, newest first. Files that can't be read are left out. */
    suspend fun list(): List<NotebookEntry> = withContext(NotebookIo) {
        val lib = storage.open()
        entriesOf(lib, lib.all().items).sortedByDescending { it.modifiedMs }
    }

    /** The live notebooks and folders and what is in Trash, from one read of the library. */
    suspend fun snapshot(): LibrarySnapshot = withContext(NotebookIo) {
        val lib = storage.open()
        val folders = ArrayList<LibraryFolder>()
        val queue = ArrayDeque<String?>()
        queue.add(null)
        while (queue.isNotEmpty()) {
            for (folder in lib.subfolders(queue.removeFirst())) {
                folders.add(folder)
                queue.add(folder.id)
            }
        }
        LibrarySnapshot(
            entries = entriesOf(lib, lib.all().items).sortedByDescending { it.modifiedMs },
            folders = folders,
            trash = entriesOf(lib, lib.trash().items),
            trashFolders = lib.trashFolders(),
        )
    }

    /** How many items and folders the purge at the first open of the library removed. It reads 0 after that. */
    suspend fun takePurged(): Int = withContext(NotebookIo) {
        storage.open()
        storage.takePurged()
    }

    /** Live notebooks directly in [folderId] (null for the top level), newest first. */
    suspend fun listFolder(folderId: String?): List<NotebookEntry> = withContext(NotebookIo) {
        val lib = storage.open()
        entriesOf(lib, lib.inFolder(folderId).items)
    }

    /** Notebooks that have been opened, the one opened last first. */
    suspend fun listRecent(): List<NotebookEntry> = withContext(NotebookIo) {
        val lib = storage.open()
        entriesOf(lib, lib.recent().items)
    }

    suspend fun listStarred(): List<NotebookEntry> = withContext(NotebookIo) {
        val lib = storage.open()
        entriesOf(lib, lib.starred().items)
    }

    /** Notebooks in Trash, the one deleted last first. */
    suspend fun listTrash(): List<NotebookEntry> = withContext(NotebookIo) {
        val lib = storage.open()
        entriesOf(lib, lib.trash().items)
    }

    /** The live folders directly under [parentId] (null for the top level), in their manual order. */
    suspend fun folders(parentId: String? = null): List<LibraryFolder> = withContext(NotebookIo) {
        storage.open().subfolders(parentId)
    }

    /** The deleted folders that are the roots of what was deleted, oldest first. */
    suspend fun trashFolders(): List<LibraryFolder> = withContext(NotebookIo) {
        storage.open().trashFolders()
    }

    suspend fun create(spec: NewNotebookSpec, folderId: String? = null): File = withContext(NotebookIo) {
        val file = File(dir, "${UUID.randomUUID()}${LibraryStorage.EXTENSION}")
        try {
            val notebook = Notebook.create(
                file = file,
                title = spec.title,
                infinite = spec.infinite,
                paper = spec.paper,
                landscape = spec.landscape,
                background = PageBackground(spec.pattern, spec.spacingMm, spec.paperColor),
                coverColor = spec.coverColor,
            )
            notebook.close()
            register(file, folderId)
        } catch (e: Exception) {
            file.delete()
            throw StoreException(R.string.library_create_failed, e)
        }
        file
    }

    /**
     * Copies a picked file in: one notebook, or every notebook and folder of a library package, which keeps its
     * folder tree under [folderId]. Anything else is refused.
     */
    suspend fun import(uri: Uri, folderId: String? = null): PackageReport = withContext(NotebookIo) {
        val temp = File(dir, "import-${UUID.randomUUID()}.tmp")
        try {
            val input = appContext.contentResolver.openInputStream(uri)
                ?: throw StoreException(R.string.store_open_file)
            input.use { i -> temp.outputStream().use { o -> i.copyTo(o) } }
            val single = try {
                NotebookInfo.read(temp)
                true
            } catch (e: Exception) {
                if (!LibraryPackage.isPackage(temp)) throw StoreException(R.string.store_not_notebook, e)
                false
            }
            if (!single) return@withContext importPackage(temp, folderId)
            val target = File(dir, "${UUID.randomUUID()}${LibraryStorage.EXTENSION}")
            if (!temp.renameTo(target)) throw StoreException(R.string.store_add_notebook)
            try {
                register(target, folderId)
            } catch (e: Exception) {
                target.delete()
                throw StoreException(R.string.store_add_notebook, e)
            }
            PackageReport(folders = 0, items = 1, skipped = 0, fileRefs = listOf(target.name))
        } catch (e: StoreException) {
            throw e
        } catch (e: Exception) {
            throw StoreException(R.string.store_read_file, e)
        } finally {
            temp.delete()
        }
    }

    /**
     * Writes the picked [folders], with everything in them, and the picked [entries] as one library package to the
     * document at [uri].
     */
    suspend fun exportPackage(folders: List<String>, entries: List<NotebookEntry>, uri: Uri): PackageReport =
        withContext(NotebookIo) {
            val temp = File(appContext.cacheDir, "export-${UUID.randomUUID()}${LibraryPackage.EXTENSION}")
            try {
                val report = try {
                    LibraryPackage.export(storage.open(), folders, entries.map { it.id }, dir, temp, now())
                } catch (e: LibraryException) {
                    val why = if (e.kind == LibraryErrors.EMPTY) {
                        R.string.library_package_nothing
                    } else {
                        R.string.library_package_export_failed
                    }
                    throw StoreException(why, e)
                }
                copyFileToUri(appContext, temp, uri)
                report
            } finally {
                temp.delete()
            }
        }

    /** Makes a notebook from the PDF at [uri], one page per PDF page. The message of a failure is fit to show. */
    suspend fun importPdf(uri: Uri, folderId: String? = null): File = withContext(NotebookIo) {
        val name = displayName(appContext, uri)
        val pdf = copyUriToCache(appContext, uri)
        val target = File(dir, "${UUID.randomUUID()}${LibraryStorage.EXTENSION}")
        try {
            Notebook.importPdf(target, pdf, titleOf(name), name).close()
            register(target, folderId)
        } catch (e: IOException) {
            target.delete()
            throw e.asStoreException(R.string.library_import_pdf_failed)
        } catch (e: Exception) {
            target.delete()
            throw StoreException(R.string.library_import_pdf_failed, e)
        } finally {
            pdf.delete()
        }
        target
    }

    suspend fun export(file: File, uri: Uri) = withContext(NotebookIo) {
        copyFileToUri(appContext, file, uri)
    }

    /** Writes the notebook, backgrounds and all, as a PDF to the document the person picked. */
    suspend fun exportPdf(file: File, uri: Uri) = withContext(NotebookIo) {
        val notebook = try {
            Notebook.open(file)
        } catch (e: Exception) {
            throw StoreException(R.string.store_open_notebook, e)
        }
        try {
            NoteExports.pdf(appContext, notebook, uri, backgrounds = true, pages = null)
        } finally {
            notebook.close()
        }
    }

    /** Moves the notebook to Trash. The file stays until it is deleted for good or purged. */
    suspend fun delete(file: File) = withContext(NotebookIo) {
        change(R.string.library_delete_failed) { lib ->
            val item = lib.itemByFile(file.name)
            if (item != null && item.deletedAt == null) lib.trashItems(listOf(item.id), now())
        }
    }

    /** Saves a new [title] and paper in one pass, for the edit sheet. */
    suspend fun editLook(file: File, title: String, background: PageBackground) = withContext(NotebookIo) {
        try {
            val notebook = Notebook.open(file)
            try {
                notebook.setTitle(title)
                notebook.setBackground(background)
                notebook.save()
            } finally {
                notebook.close()
            }
        } catch (e: Exception) {
            throw StoreException(R.string.library_save_failed, e)
        }
        retitle(file, title, R.string.library_save_failed)
    }

    suspend fun rename(file: File, title: String) = withContext(NotebookIo) {
        try {
            val notebook = Notebook.open(file)
            try {
                notebook.setTitle(title)
                notebook.save()
            } finally {
                notebook.close()
            }
        } catch (e: Exception) {
            throw StoreException(R.string.library_rename_failed, e)
        }
        retitle(file, title, R.string.library_rename_failed)
    }

    /**
     * Records that the notebook in [file] was opened, which Recent sorts by. Returns false when that could not be
     * saved; opening goes ahead either way.
     */
    suspend fun markOpened(file: File): Boolean = withContext(NotebookIo) {
        try {
            change(R.string.library_save_failed) { lib ->
                lib.itemByFile(file.name)?.let { lib.touchOpened(it.id, now()) }
            }
            true
        } catch (e: StoreException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    /** Makes a folder under [parentId] (null for the top level). A clashing name gets " 2", " 3" and so on. */
    suspend fun createFolder(parentId: String?, name: String): LibraryFolder = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.createFolder(parentId, name, now()) }
    }

    /** Renames a folder and returns the name it got. */
    suspend fun renameFolder(id: String, name: String): String = withContext(NotebookIo) {
        change(R.string.library_rename_failed) { it.renameFolder(id, name, now()) }
    }

    /** Whether a folder may move under [targetId] (null for the top level), and if not, why. */
    suspend fun canMoveFolder(id: String, targetId: String?): MoveVerdict = withContext(NotebookIo) {
        storage.open().canMove(id, targetId)
    }

    /** Moves a folder and returns its name there. */
    suspend fun moveFolder(id: String, targetId: String?): String = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.moveFolder(id, targetId, now()) }
    }

    /** Moves a folder and every notebook in it to Trash. */
    suspend fun deleteFolder(id: String) = withContext(NotebookIo) {
        change(R.string.library_delete_failed) { it.trashFolder(id, now()) }
    }

    /** Brings a deleted folder back with the notebooks that were deleted with it. */
    suspend fun restoreFolder(id: String) = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.restoreFolder(id, now()) }
    }

    /** Removes a deleted folder, everything in it and their files for good. */
    suspend fun deleteFolderForever(id: String) = withContext(NotebookIo) {
        val removed = change(R.string.library_delete_failed) { it.deleteFolderForever(id) }
        removeFiles(removed.fileRefs)
    }

    suspend fun moveItems(entries: List<NotebookEntry>, folderId: String?) = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.moveItems(entries.map { e -> e.id }, folderId) }
    }

    suspend fun setStarred(entries: List<NotebookEntry>, starred: Boolean) = withContext(NotebookIo) {
        change(R.string.library_save_failed) { lib -> entries.forEach { lib.setStarred(it.id, starred) } }
    }

    /** Sets the cover to the ARGB colour [color], or back to the notebook's own cover when it is null. */
    suspend fun setCover(entry: NotebookEntry, color: Int?) = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.setCover(entry.id, color?.let { c -> CoverRef.color(c) }, now()) }
    }

    /** True when restoring [entry] needs a choice between [RestoreMode.WithFolder] and [RestoreMode.ToLibrary]. */
    suspend fun restoreNeedsChoice(entry: NotebookEntry): Boolean = withContext(NotebookIo) {
        storage.open().restoreNeedsChoice(entry.id)
    }

    /** Takes a notebook out of Trash and returns the folder it went to, or null for the top level. */
    suspend fun restore(entry: NotebookEntry, mode: RestoreMode = RestoreMode.Auto): String? = withContext(NotebookIo) {
        change(R.string.library_save_failed) { it.restoreItem(entry.id, mode, now()) }
    }

    /** Removes a notebook from Trash and deletes its file for good. */
    suspend fun deleteForever(entry: NotebookEntry) = withContext(NotebookIo) {
        val removed = change(R.string.library_delete_failed) { it.deleteItemForever(entry.id) }
        removeFiles(removed.fileRefs)
    }

    /** Deletes everything in Trash, files included, for good. */
    suspend fun emptyTrash() = withContext(NotebookIo) {
        val removed = change(R.string.library_delete_failed) { it.emptyTrash() }
        removeFiles(removed.fileRefs)
    }

    /** Deletes what has been in Trash for more than 30 days, files included. Returns how many notebooks went. */
    suspend fun purge(): Int = withContext(NotebookIo) {
        val removed = change(R.string.library_delete_failed) { it.purge(now()) }
        removeFiles(removed.fileRefs)
        removed.items
    }

    private fun now(): Long = System.currentTimeMillis()

    /** Runs [block] on the library and saves the result. [failure] words a rejected change. */
    private fun <T> change(@StringRes failure: Int, block: (Library) -> T): T {
        val lib = storage.open()
        val result = try {
            block(lib)
        } catch (e: LibraryException) {
            throw StoreException(failure, e)
        }
        try {
            storage.save(lib)
        } catch (e: IOException) {
            throw StoreException(R.string.library_save_failed, e)
        }
        return result
    }

    /** Adds the package [file] under [folderId]. The library only changes, and is saved, if the whole import works. */
    private fun importPackage(file: File, folderId: String?): PackageReport {
        val lib = storage.open()
        val report = try {
            LibraryPackage.import(lib, file, dir, folderId, now())
        } catch (e: LibraryException) {
            val why = if (e.kind == LibraryErrors.UNSUPPORTED_VERSION) {
                R.string.library_package_newer
            } else {
                R.string.library_import_failed
            }
            throw StoreException(why, e)
        }
        try {
            storage.save(lib)
        } catch (e: IOException) {
            // The notebooks are on disk and in the library in memory, and the next change saves it. Even after a
            // restart they come back as top level items, never lost.
        }
        return report
    }

    private fun register(file: File, folderId: String?) {
        val info = NotebookInfo.read(file)
        change(R.string.library_save_failed) { storage.register(it, file, info, folderId) }
    }

    private fun retitle(file: File, title: String, @StringRes failure: Int) {
        change(failure) { lib ->
            lib.itemByFile(file.name)?.let { lib.renameItem(it.id, title, file.lastModified()) }
        }
    }

    private fun removeFiles(fileRefs: List<String>) {
        if (storage.deleteFiles(fileRefs) > 0) throw StoreException(R.string.library_delete_failed)
    }

    /**
     * Entries for [items], leaving out those whose file can't be read. Live items also catch up with their file
     * here: the time it was saved, its page count and what kind of notebook it now is.
     */
    private fun entriesOf(lib: Library, items: List<LibraryItem>): List<NotebookEntry> {
        var synced = false
        val entries = items.mapNotNull { item ->
            val file = File(dir, item.fileRef)
            val info = try {
                NotebookInfo.read(file)
            } catch (e: Exception) {
                return@mapNotNull null
            }
            var current = item
            if (item.deletedAt == null) {
                val saved = file.lastModified()
                val pages = info.pageIds.size
                val kind = LibraryKind.of(info.infinite, info.hasPdf)
                if ((saved > 0 && saved != item.modifiedAt) || pages != item.pageCount) {
                    lib.touchModified(item.id, if (saved > 0) saved else item.modifiedAt, pages)
                    current = current.copy(modifiedAt = if (saved > 0) saved else item.modifiedAt, pageCount = pages)
                    synced = true
                }
                if (kind != item.kind) {
                    lib.setKind(item.id, kind)
                    synced = true
                }
            }
            NotebookEntry(
                file = file,
                title = current.title,
                infinite = info.infinite,
                coverColor = CoverRef.colorOf(current.coverRef) ?: info.coverColor,
                modifiedMs = current.modifiedAt,
                pages = current.pageCount,
                hasPdf = info.hasPdf,
                background = info.background,
                id = current.id,
                folderId = current.folderId,
                starred = current.starred,
                coverRef = current.coverRef,
                deletedAt = current.deletedAt,
                deletedFromFolderId = current.deletedFromFolderId,
                deleteBatchId = current.deleteBatchId,
            )
        }
        if (synced) {
            try {
                storage.save(lib)
            } catch (e: IOException) {
                // The times are catching up with files, so the next listing does the same work again.
            }
        }
        return entries
    }
}
