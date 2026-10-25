package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.onemnemo.grapho.R
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * Draws pages of a notebook as PNG images with the same code the screen uses, pictures included. Not thread safe: one
 * instance does all its work on the thread that calls it, which also owns the [PdfRenderer]s it opens. [close] closes
 * them and removes the PDF files that were written to [workDir].
 *
 * Without [backgrounds] a page is ink on white. With them the paper colour and pattern are drawn, and a PDF page
 * shows its original content.
 */
class PageImages(private val notebook: Notebook, private val workDir: File, private val backgrounds: Boolean) :
    Closeable {
    private val info = notebook.info()
    private val cache = PageCache(notebook).also {
        it.setPaper(if (backgrounds) info.background.color else Color.WHITE)
    }
    private val painter = DocumentPainter()
    private val renderers = HashMap<String, PdfRenderer>()
    private val opened = ArrayList<File>()
    private val area = RectF()

    // Decoded at the longest side a page can be exported at, so a picture is never softer than the page.
    private val images = ExportImages(notebook, ExportPlan.MAX_SIDE)

    /** Draws the zero-based [page] and writes it to [out] as a PNG. Throws [IOException]. */
    fun write(page: Int, out: File) {
        val bitmap = render(page)
        try {
            out.outputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream)) {
                    throw UserIOException(R.string.export_error_write)
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun render(page: Int): Bitmap {
        val pageId = info.pageIds.getOrNull(page) ?: throw UserIOException(R.string.export_error_no_page)
        cache.refresh(pageId, page)
        val scene = cache.scenes[pageId]
        if (info.infinite) {
            if (!inkBounds(scene)) throw UserIOException(R.string.export_error_empty)
        } else {
            val points = info.pageSizes.getOrNull(page) ?: throw UserIOException(R.string.export_error_no_page)
            area.set(0f, 0f, points.width, points.height)
        }
        val size = ExportPlan.imageSize(area.width(), area.height())
        val bitmap = try {
            Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            throw UserIOException(R.string.export_error_memory, cause = e)
        }
        try {
            val ref = info.pdfRefs.getOrNull(page)
            // A PDF page is white whatever the paper colour is.
            val paper = if (backgrounds && ref == null) info.background.color else Color.WHITE
            bitmap.eraseColor(paper)
            if (backgrounds && ref != null) drawPdf(bitmap, ref, page)
            val canvas = Canvas(bitmap)
            canvas.scale(size.width / area.width(), size.height / area.height())
            canvas.translate(-area.left, -area.top)
            painter.drawPage(
                canvas,
                info.background,
                backgrounds && ref == null,
                pageId,
                cache.scenes,
                area,
                size.scale,
                images = images,
            )
        } catch (e: Throwable) {
            bitmap.recycle()
            throw e
        } finally {
            // A page can hold pictures of tens of megabytes each, so none is kept for the next page.
            images.close()
        }
        return bitmap
    }

    /** Sets [area] to the ink and images of an infinite canvas plus a margin. Returns false when there are none. */
    private fun inkBounds(scene: PageScene?): Boolean {
        if (scene == null) return false
        var found = false
        for (box in scene.strokes.map { it.bounds } + scene.images.map { it.bounds } + scene.texts.map { it.bounds }) {
            if (found) area.union(box) else area.set(box)
            found = true
        }
        if (found) area.inset(-ExportPlan.INK_MARGIN, -ExportPlan.INK_MARGIN)
        return found
    }

    private fun drawPdf(bitmap: Bitmap, ref: PdfRef, pageNumber: Int) {
        val renderer = rendererFor(ref.source)
        if (ref.index !in 0 until renderer.pageCount) {
            throw UserIOException(R.string.export_error_draw_page, listOf(pageNumber + 1))
        }
        try {
            val pdfPage = renderer.openPage(ref.index)
            try {
                val m = Matrix()
                m.setScale(bitmap.width.toFloat() / pdfPage.width, bitmap.height.toFloat() / pdfPage.height)
                pdfPage.render(bitmap, null, m, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            } finally {
                pdfPage.close()
            }
        } catch (e: RuntimeException) {
            throw UserIOException(R.string.export_error_draw_page, listOf(pageNumber + 1), e)
        }
    }

    private fun rendererFor(source: String): PdfRenderer {
        renderers[source]?.let { return it }
        val file = File(workDir, "source-${opened.size}.pdf")
        opened.add(file)
        notebook.writePdf(source, file)
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: IOException) {
            descriptor.close()
            throw e
        } catch (e: SecurityException) {
            descriptor.close()
            throw UserIOException(R.string.export_error_open_pdf, cause = e)
        }
        renderers[source] = renderer
        return renderer
    }

    override fun close() {
        var failure: IOException? = null
        for (renderer in renderers.values) {
            try {
                renderer.close()
            } catch (e: RuntimeException) {
                failure = UserIOException(R.string.export_error_close_pdf, cause = e)
            }
        }
        renderers.clear()
        for (file in opened) file.delete()
        opened.clear()
        failure?.let { throw it }
    }

    private companion object {
        /** PNG ignores quality; the parameter is required. */
        const val PNG_QUALITY = 100
    }
}
