package com.onemnemo.grapho.note

import com.onemnemo.grapho.engine.NativeLibrary
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** What a package export wrote or an import added. [skipped] counts notebooks whose file could not be read. */
data class PackageReport(val folders: Int, val items: Int, val skipped: Int, val fileRefs: List<String>)

/**
 * Library packages: many notebooks, canvases and folders in one `.grapholib` file, see `docs/library-package.md`.
 * A single notebook stays a plain `.grapho` file.
 */
object LibraryPackage {
    const val EXTENSION = ".grapholib"

    /**
     * Writes the live [folderIds] with everything below them and the live [itemIds] to [out]. The notebook files
     * are read from [dir]. Throws [LibraryException], with [LibraryErrors.EMPTY] when nothing picked is live.
     */
    fun export(lib: Library, folderIds: List<String>, itemIds: List<String>, dir: File, out: File, now: Long) = report(
        lib.withHandle { handle ->
            val args = JSONObject()
                .put("folderIds", JSONArray(folderIds))
                .put("itemIds", JSONArray(itemIds))
                .put("dir", dir.absolutePath)
                .put("out", out.absolutePath)
                .put("now", now)
            NativeLibrary.nativeExportPackage(handle, args.toString())
        },
    )

    /**
     * Adds the package [file] under [folderId] (null for the top level), writing its notebooks into [dir]. The
     * library in memory only changes when the whole import works; the caller saves it.
     */
    fun import(lib: Library, file: File, dir: File, folderId: String?, now: Long) = report(
        lib.withHandle { handle ->
            val args = JSONObject()
                .put("path", file.absolutePath)
                .put("dir", dir.absolutePath)
                .put("folderId", folderId ?: JSONObject.NULL)
                .put("now", now)
            NativeLibrary.nativeImportPackage(handle, args.toString())
        },
    )

    /** Whether [file] is a library package rather than one notebook. */
    fun isPackage(file: File): Boolean = NativeLibrary.nativeIsPackage(file.absolutePath)

    private fun report(json: String): PackageReport {
        val o = JSONObject(json)
        val refs = o.getJSONArray("fileRefs")
        return PackageReport(
            folders = o.getInt("folders"),
            items = o.getInt("items"),
            skipped = o.getInt("skipped"),
            fileRefs = List(refs.length()) { refs.getString(it) },
        )
    }
}
