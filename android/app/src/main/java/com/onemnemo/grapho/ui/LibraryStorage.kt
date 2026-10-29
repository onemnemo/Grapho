package com.onemnemo.grapho.ui

import com.onemnemo.grapho.note.CoverRef
import com.onemnemo.grapho.note.Library
import com.onemnemo.grapho.note.LibraryKind
import com.onemnemo.grapho.note.NotebookInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The library file and the notebook files it points at. The library is loaded once, and every change is saved
 * at once with a temporary file and a rename. Everything here must run on [NotebookIo].
 */
class LibraryStorage(private val filesDir: File) {
    private var library: Library? = null
    private var purgedAtOpen = 0

    /** Where the notebook files live. */
    val notebooks: File
        get() = File(filesDir, NOTEBOOKS_DIR).also { it.mkdirs() }

    private val libraryFile: File
        get() = File(filesDir, LIBRARY_FILE)

    /**
     * The library. The first call reads the file, registers notebook files it does not know, drops items whose
     * file is gone, and purges what has been in Trash for too long.
     */
    fun open(): Library {
        library?.let { return it }
        val lib = load()
        try {
            prepare(lib)
        } catch (e: Exception) {
            lib.close()
            throw e
        }
        library = lib
        return lib
    }

    /** How many items and folders the purge at open removed. It is handed out once, then reads 0. */
    fun takePurged(): Int = purgedAtOpen.also { purgedAtOpen = 0 }

    /** Writes the library to disk in one step, so a crash never leaves half a file. */
    @Throws(IOException::class)
    fun save(lib: Library) {
        val target = libraryFile
        val temp = File(filesDir, "$LIBRARY_FILE.tmp")
        FileOutputStream(temp).use { out ->
            out.write(lib.toJson().toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    /** Registers the notebook file [file] as a top level item, or in [folderId]. Returns the item id. */
    fun register(lib: Library, file: File, info: NotebookInfo, folderId: String?): String {
        val modified = file.lastModified()
        return lib.addItem(
            kind = LibraryKind.of(info.infinite, info.hasPdf),
            title = info.title,
            folderId = folderId,
            coverRef = info.coverColor?.let { CoverRef.color(it) },
            pageCount = info.pageIds.size,
            fileRef = file.name,
            createdAt = modified,
            modifiedAt = modified,
            now = System.currentTimeMillis(),
        )
    }

    /** Deletes the notebook files named by [fileRefs] and returns how many could not be deleted. */
    fun deleteFiles(fileRefs: List<String>): Int =
        fileRefs.count { ref -> File(notebooks, ref).let { it.exists() && !it.delete() } }

    private fun load(): Library {
        val file = libraryFile
        if (!file.exists()) return Library.create()
        return try {
            Library.fromJson(file.readText(Charsets.UTF_8))
        } catch (e: IOException) {
            // Keep the unreadable file aside and rebuild from the notebook files, so no notebook is lost.
            file.renameTo(File(filesDir, "$LIBRARY_FILE.unreadable"))
            Library.create()
        }
    }

    private fun prepare(lib: Library) {
        var changed = reconcile(lib)
        val removed = lib.purge(System.currentTimeMillis())
        if (removed.items + removed.folders > 0) {
            purgedAtOpen = removed.items + removed.folders
            // A file that cannot be deleted now shows up again as a new item at the next start, never lost.
            deleteFiles(removed.fileRefs)
            changed = true
        }
        if (changed) {
            try {
                save(lib)
            } catch (e: IOException) {
                // The library in memory is right, and the next change saves it again. Failing here would hide
                // every notebook, and the same work is redone at the next start.
            }
        }
    }

    /** Makes the library match the notebook files. Returns whether it changed. */
    private fun reconcile(lib: Library): Boolean {
        val files = notebooks.listFiles { f -> f.isFile && f.name.endsWith(EXTENSION) } ?: return false
        var changed = lib.dropMissingFiles(files.map { it.name }).isNotEmpty()
        val known = lib.fileRefs().toHashSet()
        for (file in files.sortedBy { it.name }) {
            if (file.name in known) continue
            // A file that cannot be read stays on disk and is tried again at the next start.
            val info = try {
                NotebookInfo.read(file)
            } catch (e: Exception) {
                continue
            }
            register(lib, file, info, null)
            changed = true
        }
        return changed
    }

    companion object {
        const val EXTENSION = ".grapho"
        private const val NOTEBOOKS_DIR = "notebooks"
        private const val LIBRARY_FILE = "library.json"
    }
}
