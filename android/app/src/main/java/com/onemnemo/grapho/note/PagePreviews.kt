package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Process
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A small render of one page, stretched over the screen wherever the sharp render does not reach yet. [area] is
 * the page-local area it shows, in document units. The bitmap is never drawn into after it is published. The other
 * fields say what it was drawn from, so a page that changed is drawn again.
 */
class PagePreview(
    val bitmap: Bitmap,
    val area: RectF,
    val scene: PageScene?,
    val paper: Int,
    val pdf: Boolean,
    val hidden: Set<StrokeId>?,
    val imageEpoch: Int,
)

private class PreviewRequest(
    val viewport: Viewport,
    val info: NotebookInfo,
    val scenes: Map<String, PageScene>,
    val hidden: HiddenStrokes?,
    val imageEpoch: Int,
)

/**
 * Low-resolution renders of the pages in and around the view, made on a background thread. When a zoom out or a
 * pan shows part of the document the sharp render ([DryCache]) has not covered yet, the screen stretches these over
 * it, so the ink is there at once, slightly soft, instead of an empty page or the desk.
 *
 * A page preview holds the paper, its pattern and the ink; on a PDF page only the ink, since the screen draws the
 * PDF under it. An infinite canvas has one preview over all its content, without the pattern.
 *
 * Requests are throttled and coalesced: the newest one is worked through page by page, most needed first, and a
 * newer request takes over between pages.
 */
class PagePreviews(private val images: ImageBitmaps, private val onPublished: () -> Unit) {

    private val lock = Any()
    private var executor: ScheduledExecutorService? = null
    private val painter = DocumentPainter()
    private val latest = AtomicReference<PreviewRequest?>(null)
    private val scheduled = AtomicBoolean(false)

    /** The previews by page id. Replaced as a whole, never changed in place. */
    @Volatile
    var current: Map<String, PagePreview> = emptyMap()
        private set

    // Render thread only.
    private val dst = RectF()

    /** Asks for previews of the pages around [viewport]. Pages that have not changed are kept. */
    fun request(
        viewport: Viewport,
        info: NotebookInfo,
        scenes: Map<String, PageScene>,
        hidden: HiddenStrokes?,
        imageEpoch: Int,
    ) {
        if (viewport.viewWidth < 1f || viewport.viewHeight < 1f) return
        latest.set(PreviewRequest(viewport, info, scenes, hidden, imageEpoch))
        if (!scheduled.compareAndSet(false, true)) return
        synchronized(lock) {
            val ex = executor?.takeIf { !it.isShutdown } ?: newExecutor().also { executor = it }
            try {
                ex.schedule(Runnable { work() }, DELAY_MS, TimeUnit.MILLISECONDS)
            } catch (_: RejectedExecutionException) {
                scheduled.set(false)
            }
        }
    }

    fun shutdown() {
        synchronized(lock) {
            executor?.shutdownNow()
            executor = null
        }
        latest.set(null)
        scheduled.set(false)
        current = emptyMap()
    }

    /**
     * Draws the previews of the visible pages at [vp], leaving out [covered], the part of the view the sharp render
     * covers. The paper and any PDF page under a preview are the caller's to draw first.
     */
    fun draw(canvas: Canvas, vp: Viewport, info: NotebookInfo, covered: RectF, paint: Paint) {
        val map = current
        if (map.isEmpty()) return
        canvas.save()
        canvas.clipOutRect(covered)
        if (vp.infinite) {
            val p = info.pageIds.firstOrNull()?.let { map[it] }
            if (p != null) {
                val s = vp.scale
                dst.set(
                    vp.offsetX + p.area.left * s,
                    vp.offsetY + p.area.top * s,
                    vp.offsetX + p.area.right * s,
                    vp.offsetY + p.area.bottom * s,
                )
                canvas.drawBitmap(p.bitmap, null, dst, paint)
            }
        } else {
            for (i in vp.visiblePages()) {
                val p = info.pageIds.getOrNull(i)?.let { map[it] } ?: continue
                canvas.drawBitmap(p.bitmap, null, vp.pageRect(i, dst), paint)
            }
        }
        canvas.restore()
    }

    private fun newExecutor(): ScheduledExecutorService = ScheduledThreadPoolExecutor(1) { r ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            r.run()
        }, "GraphoPreviews")
    }

    private fun work() {
        // Cleared first, so a request that arrives while this runs schedules another pass.
        scheduled.set(false)
        val r = latest.getAndSet(null) ?: return
        val vp = r.viewport
        val ids = r.info.pageIds
        val wanted = if (vp.infinite) {
            listOfNotNull(ids.firstOrNull()?.let { 0 to it })
        } else {
            PreviewPlan.order(vp.visiblePages(), vp.centrePage(), vp.pageCount).mapNotNull { i ->
                ids.getOrNull(i)?.let { i to it }
            }
        }
        val keep = wanted.mapTo(HashSet()) { it.second }
        var map = current.filterKeys { it in keep }
        if (map.size != current.size) current = map
        for ((index, id) in wanted) {
            val old = map[id]
            if (old != null && fresh(old, r, index, id)) continue
            val t0 = EraseLog.nowNs()
            val next = render(r, index, id)
            EraseLog.slow("page preview", EraseLog.elapsedMs(t0), EraseLog.DRY_MS)
            map = if (next == null) map - id else map + (id to next)
            current = map
            onPublished()
            // A newer request is already scheduled; it picks up from here with the newest pages and view.
            if (latest.get() != null) return
        }
    }

    private fun hiddenOn(r: PreviewRequest, id: String): Set<StrokeId>? = r.hidden?.takeIf { it.pageId == id }?.all

    private fun fresh(p: PagePreview, r: PreviewRequest, index: Int, id: String): Boolean {
        val scene = r.scenes[id]
        if (p.scene !== scene || p.paper != r.info.background.color) return false
        if (p.pdf != (r.info.pdfRefs.getOrNull(index) != null) || p.hidden != hiddenOn(r, id)) return false
        if (scene != null && scene.images.isNotEmpty() && p.imageEpoch != r.imageEpoch) return false
        if (r.viewport.infinite) return true
        return p.area.width() == r.viewport.pages.width(index) && p.area.height() == r.viewport.pages.height(index)
    }

    private fun render(r: PreviewRequest, index: Int, id: String): PagePreview? {
        val bg = r.info.background
        val scene = r.scenes[id]
        val pdf = r.info.pdfRefs.getOrNull(index) != null
        val hidden = r.hidden?.takeIf { it.pageId == id }
        val area = if (r.viewport.infinite) {
            contentBounds(scene) ?: return null
        } else {
            RectF(0f, 0f, r.viewport.pages.width(index), r.viewport.pages.height(index))
        }
        val side = if (r.viewport.infinite) PreviewPlan.CANVAS_SIDE else PreviewPlan.PAGE_SIDE
        val k = PreviewPlan.scale(area.width(), area.height(), side)
        if (k <= 0f) return null
        val (w, h) = PreviewPlan.pixels(area.width(), area.height(), k)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paged = !r.viewport.infinite
        if (paged && !pdf) canvas.drawColor(bg.color)
        canvas.scale(w / area.width(), h / area.height())
        canvas.translate(-area.left, -area.top)
        canvas.clipRect(area)
        painter.drawPage(canvas, bg, paged && !pdf, id, r.scenes, area, k, hidden, images)
        // Starts the upload to the GPU now, rather than on the frame that first needs it.
        bitmap.prepareToDraw()
        return PagePreview(bitmap, area, scene, bg.color, pdf, hidden?.all, r.imageEpoch)
    }

    /** Everything on an infinite canvas, padded a little, or null when it is empty. */
    private fun contentBounds(scene: PageScene?): RectF? {
        scene ?: return null
        val out = RectF()
        var any = false
        fun add(b: RectF) {
            if (any) out.union(b) else out.set(b)
            any = true
        }
        scene.strokes.forEach { add(it.bounds) }
        scene.images.forEach { add(it.bounds) }
        scene.texts.forEach { add(it.bounds) }
        if (!any) return null
        out.inset(-PAD, -PAD)
        return out
    }

    private companion object {
        /** How long a request waits, so a burst of them (a zoom, a stroke) becomes one pass. */
        const val DELAY_MS = 120L

        /** Room around an infinite canvas's content, in document units, for antialiased edges. */
        const val PAD = 8f
    }
}
