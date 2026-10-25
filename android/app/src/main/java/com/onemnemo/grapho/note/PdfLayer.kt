package com.onemnemo.grapho.note

import android.view.View
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Connects a canvas to its [PdfBackgrounds]: tells it what the view needs as the view moves and settles, and
 * turns "a bitmap arrived" from the PDF thread into one call on the UI thread, however many arrive. Called on
 * the UI thread only, except for the arrival callback.
 */
class PdfLayer(
    private val host: View,
    notebook: Notebook,
    cacheDir: File,
    private val viewport: () -> Viewport,
    private val info: () -> NotebookInfo,
    private val onReady: () -> Unit,
) {
    val backgrounds = PdfBackgrounds(cacheDir, notebook::writePdf, ::arrived)

    private val arrivedPosted = AtomicBoolean(false)
    private var tickQueued = false
    private val tick = Runnable {
        tickQueued = false
        update(false)
    }

    /** The view is panning or zooming: whole pages that came into view are rendered at once, details wait. */
    fun moved() {
        if (tickQueued || !info().hasPdf) return
        tickQueued = true
        host.postDelayed(tick, MOVE_TICK_MS)
    }

    /** The view has come to rest, or the document changed: details of zoomed-in pages are rendered too. */
    fun settled() = update(true)

    fun shutdown() {
        host.removeCallbacks(tick)
        tickQueued = false
        backgrounds.shutdown()
    }

    private fun update(settled: Boolean) {
        PdfPlanner.plan(viewport(), info(), settled)?.let { backgrounds.update(it) }
    }

    private fun arrived() {
        if (arrivedPosted.compareAndSet(false, true)) {
            host.post {
                arrivedPosted.set(false)
                onReady()
            }
        }
    }

    private companion object {
        /** How often newly visible pages are asked for while the view moves. */
        const val MOVE_TICK_MS = 60L
    }
}
