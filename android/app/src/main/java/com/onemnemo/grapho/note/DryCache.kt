package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicReference

/**
 * The dry scene as drawn for [viewport], covering the whole view. The bitmap
 * is never drawn into after it is published. [generation] is the edit count
 * the scene was taken at. [hidden] names the strokes it left out because the lasso holds them.
 */
class RenderedDry(val bitmap: Bitmap, val viewport: Viewport, val generation: Int, val hidden: HiddenStrokes? = null)

private class DryRequest(
    val viewport: Viewport,
    val info: NotebookInfo,
    val scenes: Map<String, PageScene>,
    val generation: Int,
    val hidden: HiddenStrokes?,
)

/**
 * Renders the finished pages (desk, paper, images, ink, add target) into a bitmap on
 * a background thread, so the render thread that also draws the wet stroke
 * only has to blit it. PDF pages are left clear, for the screen to draw under it.
 * Requests are coalesced: only the newest is rendered.
 *
 * Everything a request carries is immutable, so rendering never touches
 * state the UI thread is changing.
 */
class DryCache(private val images: ImageBitmaps, private val onPublished: () -> Unit) {

    private val lock = Any()
    private var executor: ExecutorService? = null
    private val painter = DocumentPainter()
    private val latest = AtomicReference<DryRequest?>(null)

    /** The newest finished render, or null before the first one. */
    @Volatile
    var current: RenderedDry? = null
        private set

    // Render thread only. A bitmap is reused two renders after it stopped
    // being published, by which time nobody can still be drawing it.
    private var retiring: Bitmap? = null
    private var spare: Bitmap? = null

    /** Asks for a render of this state. Older requests that have not started are dropped. */
    fun request(
        viewport: Viewport,
        info: NotebookInfo,
        scenes: Map<String, PageScene>,
        generation: Int,
        hidden: HiddenStrokes? = null,
    ) {
        if (viewport.viewWidth < 1f || viewport.viewHeight < 1f) return
        latest.set(DryRequest(viewport, info, scenes, generation, hidden))
        synchronized(lock) {
            // A view can be detached and attached again, so start a fresh thread when needed.
            val ex = executor?.takeIf { !it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executor = it }
            try {
                ex.execute { renderLatest() }
            } catch (_: RejectedExecutionException) {
                // Shut down in between; the next request starts a new thread.
            }
        }
    }

    fun shutdown() {
        synchronized(lock) {
            executor?.shutdownNow()
            executor = null
        }
        latest.set(null)
    }

    private fun renderLatest() {
        val r = latest.getAndSet(null) ?: return // a newer render already covered it
        val w = r.viewport.viewWidth.toInt()
        val h = r.viewport.viewHeight.toInt()
        if (w < 1 || h < 1) return
        val target = spare?.takeIf { !it.isRecycled && it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        spare = null
        target.eraseColor(Color.TRANSPARENT)
        val t0 = EraseLog.nowNs()
        painter.draw(Canvas(target), r.viewport, r.info, r.scenes, r.hidden, pdfBehind = true, images = images)
        EraseLog.slow("dry render", EraseLog.elapsedMs(t0), EraseLog.DRY_MS)
        val previous = current
        current = RenderedDry(target, r.viewport, r.generation, r.hidden)
        spare = retiring
        retiring = previous?.bitmap
        onPublished()
    }
}
