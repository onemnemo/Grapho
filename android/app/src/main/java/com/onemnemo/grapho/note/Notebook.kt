package com.onemnemo.grapho.note

import com.onemnemo.grapho.engine.NativeNotebook
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Page size in document units (points, 1/72 inch), or null for an infinite canvas. */
data class PageSize(val width: Float, val height: Float)

/** A page of an embedded PDF: [source] is the id of the PDF stored in the notebook, [index] its zero-based page. */
data class PdfRef(val source: String, val index: Int)

/** A PDF stored in the notebook. [name] is the original file name. */
data class PdfSourceInfo(val id: String, val name: String, val pages: Int)

/** A notebook's background. [color] is ARGB. [spacingMm] is ignored for blank paper. */
data class PageBackground(val pattern: Int, val spacingMm: Float, val color: Int)

/** What the document looks like right now, parsed from the engine's JSON. */
data class NotebookInfo(
    val title: String,
    val infinite: Boolean,
    /** The canvas paper size, which pages without a size of their own use. Null for infinite canvases. */
    val pageSize: PageSize?,
    val background: PageBackground,
    /** ARGB, or null if the file has no cover. */
    val coverColor: Int?,
    val pageIds: List<String>,
    /** One size per page (empty for infinite canvases): the page's own, or the canvas paper size. */
    val pageSizes: List<PageSize>,
    /** One entry per page: the PDF page drawn behind the ink, or null for a page with paper. */
    val pdfRefs: List<PdfRef?>,
    val pdfs: List<PdfSourceInfo>,
    val canUndo: Boolean,
    val canRedo: Boolean,
) {
    /** True when some page shows a PDF page behind its ink. */
    val hasPdf: Boolean = pdfRefs.any { it != null }

    companion object {
        /** Points per millimetre. Document units are points, like PDF. */
        const val PT_PER_MM = 72f / 25.4f

        fun parse(json: String): NotebookInfo {
            val o = JSONObject(json)
            val canvas = o.getJSONObject("canvas")
            val infinite = canvas.getString("kind") == "infinite"
            val bg = canvas.getJSONObject("background")
            val pat = bg.getJSONObject("pattern")
            val pattern = when (pat.getString("kind")) {
                "lined" -> 1
                "grid" -> 2
                "dotted" -> 3
                else -> 0
            }
            val pageSize = if (infinite) {
                null
            } else {
                val paper = canvas.getJSONObject("paper")
                val (w, h) = when (paper.getString("name")) {
                    "a3" -> 297f to 420f
                    "a5" -> 148f to 210f
                    "letter" -> 215.9f to 279.4f
                    "custom" -> paper.getDouble("width_mm").toFloat() to paper.getDouble("height_mm").toFloat()
                    else -> 210f to 297f
                }
                val landscape = canvas.optString("orientation") == "landscape"
                val (pw, ph) = if (landscape) h to w else w to h
                PageSize(pw * PT_PER_MM, ph * PT_PER_MM)
            }
            val pages = o.getJSONArray("pages")
            return NotebookInfo(
                title = o.optString("title"),
                infinite = infinite,
                pageSize = pageSize,
                background = PageBackground(
                    pattern = pattern,
                    spacingMm = pat.optDouble("spacing_mm", 8.0).toFloat(),
                    color = rgbaToArgb(bg.getLong("color")),
                ),
                coverColor = o.optJSONObject("cover")?.let { rgbaToArgb(it.getLong("color")) },
                pageIds = List(pages.length()) { pages.getJSONObject(it).getString("id") },
                pageSizes = if (pageSize == null) {
                    emptyList()
                } else {
                    List(pages.length()) { parsePageSize(pages.getJSONObject(it), pageSize) }
                },
                pdfRefs = List(pages.length()) { parsePdfRef(pages.getJSONObject(it)) },
                pdfs = parsePdfs(o.optJSONArray("pdfs")),
                canUndo = o.optBoolean("can_undo"),
                canRedo = o.optBoolean("can_redo"),
            )
        }

        private fun parsePageSize(page: JSONObject, fallback: PageSize): PageSize {
            val size = page.optJSONObject("size") ?: return fallback
            return PageSize(size.getDouble("width").toFloat(), size.getDouble("height").toFloat())
        }

        private fun parsePdfRef(page: JSONObject): PdfRef? {
            val pdf = page.optJSONObject("pdf") ?: return null
            return PdfRef(pdf.getString("source"), pdf.getInt("index"))
        }

        private fun parsePdfs(list: JSONArray?): List<PdfSourceInfo> {
            if (list == null) return emptyList()
            return List(list.length()) {
                val p = list.getJSONObject(it)
                PdfSourceInfo(p.getString("id"), p.optString("name"), p.optInt("pages"))
            }
        }

        private fun rgbaToArgb(rgba: Long): Int {
            val v = rgba.toInt()
            return (v ushr 8) or (v shl 24)
        }

        /** Reads a file's structure without loading its ink. */
        fun read(file: File): NotebookInfo = parse(NativeNotebook.nativeReadInfo(file.path))
    }
}

/**
 * An open notebook: the native document plus where it is saved.
 *
 * Edits happen on the UI thread. [save] may run on a background thread; the
 * engine copies the document under its lock and writes without it. Call
 * [close] once, after the last save.
 */
class Notebook private constructor(private var handle: Long, val file: File) {

    val nativeHandle: Long get() = check(handle != 0L) { "notebook is closed" }.let { handle }

    /** Bumped on every change, so callers can tell whether a save is needed. */
    @Volatile var revision = 0L
        private set

    fun markChanged() {
        revision++
    }

    fun info(): NotebookInfo = NotebookInfo.parse(NativeNotebook.nativeInfo(nativeHandle))

    fun setTitle(title: String) {
        NativeNotebook.nativeSetTitle(nativeHandle, title)
        markChanged()
    }

    fun setBackground(bg: PageBackground) {
        NativeNotebook.nativeSetBackground(nativeHandle, bg.pattern, bg.spacingMm, bg.color)
        markChanged()
    }

    fun save() = save(file)

    /** Writes to [target], e.g. a cache file that is then copied out for export. */
    fun save(target: File) {
        NativeNotebook.nativeSave(nativeHandle, target.path)
    }

    /** Writes the embedded PDF [sourceId] to [out], for rendering page backgrounds. Safe off the UI thread. */
    @Synchronized
    fun writePdf(sourceId: String, out: File) {
        check(handle != 0L) { "notebook is closed" }
        NativeNotebook.nativeWritePdf(handle, sourceId, out.path)
    }

    /** The encoded bytes of the embedded image [source], or null if none has that id. Safe off the UI thread. */
    @Synchronized
    fun imageBytes(source: String): ByteArray? {
        check(handle != 0L) { "notebook is closed" }
        return NativeNotebook.nativeImageBytes(handle, source)
    }

    /**
     * Embeds a PNG or JPEG on the active layer of [page] at the rectangle ([x], [y], [width], [height]) in page-local
     * units, as one undo step. Returns the image's `[hi, lo]` id, or an empty array if the layer is hidden or locked
     * or the bytes are not a picture. Safe off the UI thread.
     */
    fun insertImage(page: Int, bytes: ByteArray, x: Float, y: Float, width: Float, height: Float): LongArray {
        val id = NativeNotebook.nativeInsertImage(nativeHandle, page, bytes, x, y, width, height)
        if (id.isNotEmpty()) markChanged()
        return id
    }

    /**
     * Swaps the image [id] on [page] for a copy with its background removed, as one undo step. [pixels] is the whole
     * decoded source, [strength] (0 to 100) is how much counts as background and [outline] is 0 for none, 1 for thin
     * and 2 for thick. Returns the new image's `[hi, lo]` id, or an empty array on failure. Safe off the UI thread.
     */
    fun removeImageBackground(
        page: Int,
        id: LongArray,
        pixels: DecodedPixels,
        strength: Int,
        outline: Int,
    ): LongArray {
        val ids = NativeNotebook.nativeRemoveImageBackground(
            nativeHandle,
            page,
            id,
            pixels.width,
            pixels.height,
            pixels.argb,
            strength,
            outline,
        )
        if (ids.isNotEmpty()) markChanged()
        return ids
    }

    /**
     * Swaps the image [id] on [page] for a copy showing [pixels], which must be the size of its source, as one undo
     * step. Returns the new image's `[hi, lo]` id, or an empty array on failure. Safe off the UI thread.
     */
    fun replaceImagePixels(page: Int, id: LongArray, pixels: DecodedPixels): LongArray {
        val ids = NativeNotebook.nativeReplaceImagePixels(
            nativeHandle,
            page,
            id,
            pixels.width,
            pixels.height,
            pixels.argb,
        )
        if (ids.isNotEmpty()) markChanged()
        return ids
    }

    /**
     * Swaps the image [id] on [page] for pen strokes traced from it, as one undo step. Returns the strokes' ids flat
     * as `[hi, lo, ...]`, or an empty array when nothing was found or on failure. Safe off the UI thread.
     */
    fun imageToInk(page: Int, id: LongArray, pixels: DecodedPixels, startedAtMs: Long): LongArray {
        val ids = NativeNotebook.nativeImageToInk(
            nativeHandle,
            page,
            id,
            pixels.width,
            pixels.height,
            pixels.argb,
            startedAtMs,
        )
        if (ids.isNotEmpty()) markChanged()
        return ids
    }

    /**
     * Writes a vector PDF to [out]. [pages] are zero-based page indices, or null for every page. Safe off the UI
     * thread: the engine copies the document under its lock. Throws [java.io.IOException].
     */
    fun exportPdf(out: File, backgrounds: Boolean, pages: List<Int>?) {
        NativeNotebook.nativeExportPdf(nativeHandle, out.path, backgrounds, pages?.toIntArray())
    }

    /** Inserts a blank page at [at] and returns its index, or -1. One undo step. */
    fun insertPage(at: Int): Int {
        val index = NativeNotebook.nativeInsertPage(nativeHandle, at)
        if (index >= 0) markChanged()
        return index
    }

    /** Inserts every page of the PDF at [pdf] at [at] and returns how many were added. One undo step. */
    fun insertPdf(at: Int, pdf: File, name: String): Int {
        val added = NativeNotebook.nativeInsertPdf(nativeHandle, at, pdf.path, name)
        if (added > 0) markChanged()
        return added
    }

    @Synchronized
    fun close() {
        if (handle != 0L) {
            NativeNotebook.nativeFree(handle)
            handle = 0L
        }
    }

    companion object {
        fun open(file: File) = Notebook(NativeNotebook.nativeOpen(file.path), file)

        /** Makes a notebook at [file] from the PDF at [pdf], one page per PDF page. */
        fun importPdf(file: File, pdf: File, title: String, name: String): Notebook {
            val notebook = Notebook(NativeNotebook.nativeImportPdf(pdf.path, title, name), file)
            try {
                notebook.save()
            } catch (e: Exception) {
                notebook.close()
                throw e
            }
            return notebook
        }

        fun create(
            file: File,
            title: String,
            infinite: Boolean,
            paper: Int,
            landscape: Boolean,
            background: PageBackground,
            coverColor: Int,
        ): Notebook {
            val h = NativeNotebook.nativeCreate(
                title,
                infinite,
                paper,
                landscape,
                background.pattern,
                background.spacingMm,
                background.color,
                coverColor,
            )
            return Notebook(h, file).also { it.save() }
        }
    }
}
