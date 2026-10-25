package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.min

/**
 * The PDF pages drawn behind the ink, rendered as bitmaps on one background thread. A [PdfRenderer] is not thread
 * safe and has one page open at a time, so every renderer lives on that thread alone.
 *
 * Two kinds of bitmap are kept: a whole-page one per page at about the resolution of a fitted view (in a
 * memory-bounded LRU), and a detail of the visible part of a page for when the view is zoomed in past that. The
 * UI thread says what is wanted with [update]; the dry render thread draws what exists with [draw]. Bitmaps are
 * drawn, replaced and recycled under one lock, so none is recycled while it is being drawn.
 *
 * [extract] writes an embedded PDF to a file, and is called on the PDF thread, once per source id: the file is
 * kept in [cacheDir] and reused. [onReady] is called on the PDF thread whenever a new bitmap can be drawn.
 */
class PdfBackgrounds(
    private val cacheDir: File,
    private val extract: (sourceId: String, out: File) -> Unit,
    private val onReady: () -> Unit,
) {
    private class Detail(val bitmap: Bitmap, val area: PdfArea, val scale: Float)

    /** The background thread and what only it touches. Replaced as a whole by [shutdown]. */
    private class Worker {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val renderers = HashMap<String, PdfRenderer>()
        val failedSources = HashSet<String>()
        val failedPages = HashSet<PdfRef>()
        val running = AtomicBoolean(false)

        @Volatile var plan: PdfPlan? = null
    }

    private val lock = Any()
    private var worker: Worker? = null
    private val base = LinkedHashMap<PdfRef, Bitmap>(16, 0.75f, true)
    private var baseBytes = 0L
    private val details = HashMap<PdfRef, Detail>()
    private val paint = Paint().apply { isFilterBitmap = true }
    private val dst = RectF()

    /** Says what the view needs now. Starts the background thread if it is not running. */
    fun update(plan: PdfPlan) {
        val w = synchronized(lock) {
            val current = worker ?: Worker().also { worker = it }
            current.plan = plan
            if (plan.settled) dropDetailsNotIn(plan)
            current
        }
        schedule(w)
    }

    /** Draws the best bitmap there is for [ref] into [width] by [height] page points at the canvas origin. */
    fun draw(canvas: Canvas, ref: PdfRef, width: Float, height: Float) {
        synchronized(lock) {
            base[ref]?.let {
                dst.set(0f, 0f, width, height)
                canvas.drawBitmap(it, null, dst, paint)
            }
            details[ref]?.let {
                dst.set(it.area.left, it.area.top, it.area.right, it.area.bottom)
                canvas.drawBitmap(it.bitmap, null, dst, paint)
            }
        }
    }

    /** Stops the thread, closes the renderers and recycles every bitmap. [update] starts it again if needed. */
    fun shutdown() {
        val old = synchronized(lock) {
            val w = worker
            worker = null
            for (b in base.values) b.recycle()
            base.clear()
            baseBytes = 0L
            for (d in details.values) d.bitmap.recycle()
            details.clear()
            w
        } ?: return
        old.plan = null
        try {
            old.executor.execute { closeRenderers(old) }
        } catch (_: RejectedExecutionException) {
            // Already shut down; its cleanup has run.
        }
        old.executor.shutdown()
    }

    private fun closeRenderers(w: Worker) {
        for ((source, renderer) in w.renderers) {
            try {
                renderer.close()
            } catch (e: RuntimeException) {
                Log.w(TAG, "closing the renderer of pdf $source failed", e)
            }
        }
        w.renderers.clear()
    }

    private fun isCurrent(w: Worker) = synchronized(lock) { worker === w }

    private fun schedule(w: Worker) {
        if (!w.running.compareAndSet(false, true)) return
        try {
            w.executor.execute { drain(w) }
        } catch (_: RejectedExecutionException) {
            w.running.set(false)
        }
    }

    private fun drain(w: Worker) {
        try {
            while (isCurrent(w)) {
                val plan = w.plan ?: break
                val job = nextJob(w, plan) ?: break
                val rendered = when (job) {
                    is BaseWant -> renderBase(w, job)
                    is DetailWant -> renderDetail(w, job)
                }
                if (rendered) onReady()
            }
        } finally {
            w.running.set(false)
        }
        // A plan that arrived after the last look but before the flag cleared would otherwise wait for the next one.
        val plan = w.plan
        if (plan != null && isCurrent(w) && nextJob(w, plan) != null) schedule(w)
    }

    /** The most urgent bitmap that is missing, or null. Worker thread only. */
    private fun nextJob(w: Worker, plan: PdfPlan): PdfWant? = synchronized(lock) {
        plan.visible.firstOrNull { it.ref !in base && usable(w, it.ref) }
            ?: plan.details.firstOrNull { !covered(it) && usable(w, it.ref) }
            ?: plan.near.firstOrNull { it.ref !in base && usable(w, it.ref) }
    }

    private fun usable(w: Worker, ref: PdfRef) = ref.source !in w.failedSources && ref !in w.failedPages

    private fun covered(want: DetailWant): Boolean {
        val have = details[want.ref] ?: return false
        return have.scale >= want.scale * DETAIL_REUSE && have.area.contains(want.visible)
    }

    private fun dropDetailsNotIn(plan: PdfPlan) {
        val it = details.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (plan.details.none { d -> d.ref == entry.key }) {
                entry.value.bitmap.recycle()
                it.remove()
            }
        }
    }

    private fun renderBase(w: Worker, want: BaseWant): Boolean {
        val pxW = pixels(want.width * want.scale)
        val pxH = pixels(want.height * want.scale)
        val bitmap = render(w, want.ref, pxW, pxH) { page, bmp ->
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        } ?: return false
        synchronized(lock) {
            if (worker !== w) {
                bitmap.recycle()
                return false
            }
            base.put(want.ref, bitmap)?.let {
                baseBytes -= it.allocationByteCount
                it.recycle()
            }
            baseBytes += bitmap.allocationByteCount
            trimBase(w.plan)
        }
        return true
    }

    /** Drops the least recently drawn whole-page bitmaps, except those on screen, while over budget. */
    private fun trimBase(plan: PdfPlan?) {
        if (baseBytes <= BASE_BUDGET_BYTES) return
        val it = base.entries.iterator()
        while (it.hasNext() && baseBytes > BASE_BUDGET_BYTES) {
            val entry = it.next()
            if (plan != null && plan.visible.any { v -> v.ref == entry.key }) continue
            baseBytes -= entry.value.allocationByteCount
            entry.value.recycle()
            it.remove()
        }
    }

    private fun renderDetail(w: Worker, want: DetailWant): Boolean {
        // Without the margin when that would be too big, and at a lower scale when even the visible part is.
        val area = if (pixelSize(want.area, want.scale) > MAX_DETAIL_SIDE) want.visible else want.area
        val scale = want.scale * min(1f, MAX_DETAIL_SIDE / pixelSize(area, want.scale))
        val pxW = pixels((area.right - area.left) * scale)
        val pxH = pixels((area.bottom - area.top) * scale)
        val bitmap = render(w, want.ref, pxW, pxH) { page, bmp ->
            // Page points to bitmap pixels: stretch like the whole-page bitmap does, then move the area to the origin.
            val m = Matrix()
            m.setScale(scale * want.width / page.width, scale * want.height / page.height)
            m.postTranslate(-area.left * scale, -area.top * scale)
            page.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        } ?: return false
        synchronized(lock) {
            if (worker !== w) {
                bitmap.recycle()
                return false
            }
            details.put(want.ref, Detail(bitmap, area, scale))?.bitmap?.recycle()
        }
        return true
    }

    private fun pixelSize(area: PdfArea, scale: Float): Float =
        maxOf((area.right - area.left) * scale, (area.bottom - area.top) * scale)

    private fun pixels(v: Float): Int = ceil(v).toInt().coerceAtLeast(1)

    /** Renders one page into a new white bitmap, or returns null after logging why not. Worker thread only. */
    private fun render(
        w: Worker,
        ref: PdfRef,
        pxW: Int,
        pxH: Int,
        block: (PdfRenderer.Page, Bitmap) -> Unit,
    ): Bitmap? {
        val renderer = rendererFor(w, ref.source) ?: return null
        if (ref.index !in 0 until renderer.pageCount) {
            Log.w(TAG, "pdf ${ref.source} has no page ${ref.index}")
            w.failedPages.add(ref)
            return null
        }
        val bitmap = try {
            Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "no memory for a ${pxW}x$pxH pdf bitmap", e)
            w.failedPages.add(ref)
            return null
        }
        // PDF pages are transparent, so the paper is painted first.
        bitmap.eraseColor(Color.WHITE)
        try {
            val page = renderer.openPage(ref.index)
            try {
                block(page, bitmap)
            } finally {
                page.close()
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "rendering page ${ref.index} of pdf ${ref.source} failed", e)
            w.failedPages.add(ref)
            bitmap.recycle()
            return null
        }
        return bitmap
    }

    /** The open renderer of a source, extracting the file first when it is not cached. Worker thread only. */
    private fun rendererFor(w: Worker, source: String): PdfRenderer? {
        w.renderers[source]?.let { return it }
        if (source in w.failedSources) return null
        val file = File(cacheDir, "$source.pdf")
        try {
            if (!SOURCE_ID.matches(source)) throw IOException("unexpected pdf id")
            if (!file.isFile || file.length() == 0L) {
                if (!cacheDir.isDirectory && !cacheDir.mkdirs()) throw IOException("couldn't make the pdf cache")
                val temp = File(cacheDir, "$source.pdf.tmp")
                extract(source, temp)
                if (!temp.renameTo(file)) throw IOException("couldn't store the pdf")
            }
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (e: IOException) {
                descriptor.close()
                throw e
            } catch (e: SecurityException) {
                descriptor.close()
                throw e
            }
            w.renderers[source] = renderer
            return renderer
        } catch (e: IOException) {
            return failSource(w, source, file, e)
        } catch (e: RuntimeException) {
            return failSource(w, source, file, e)
        }
    }

    private fun failSource(w: Worker, source: String, file: File, e: Exception): PdfRenderer? {
        Log.w(TAG, "opening pdf $source failed", e)
        w.failedSources.add(source)
        // A damaged cache file is extracted again the next time the note opens.
        file.delete()
        return null
    }

    private companion object {
        const val TAG = "GraphoPdf"

        /** Memory for whole-page bitmaps. A page at the size cap is about 12 MB. */
        const val BASE_BUDGET_BYTES = 64L * 1024 * 1024

        /** Longest side of a detail bitmap, in pixels. */
        const val MAX_DETAIL_SIDE = 4096f

        /** A detail rendered at this fraction of the wanted scale or better is kept. */
        const val DETAIL_REUSE = 0.9f

        val SOURCE_ID = Regex("[0-9A-Za-z-]{1,64}")
    }
}
