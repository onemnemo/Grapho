package com.onemnemo.grapho.note

import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Refreshes pages of a [PageCache] on a background thread, for the eraser,
 * which replaces strokes with new pieces on every frame. At most one refresh
 * per page runs and one more waits; a refresh always reads the strokes as
 * they are when it starts. Results are published on the UI thread through
 * [post], and one built from a scene that has since been replaced is dropped.
 *
 * New pieces get quick geometry so the picture keeps up. Once asked to
 * [upgrade], the loader swaps them for the exact outline a few at a time, so
 * the finished page looks the same as one drawn without erasing.
 *
 * Everything but the background work is called on the UI thread.
 */
class GeometryLoader(
    private val cache: PageCache,
    private val post: (Runnable) -> Unit,
    private val onPublished: () -> Unit,
) {
    private val gate = RefreshGate<String>()
    private var executor: ExecutorService? = null

    // Bumped by shutdown, so work from before it is ignored when it comes back.
    private var epoch = 0

    /** Queues a refresh of one page. [upgrade] also rebuilds quick strokes exactly. */
    fun request(pageId: String, pageIndex: Int, upgrade: Boolean) {
        if (gate.submit(pageId, upgrade)) start(pageId, pageIndex, upgrade)
    }

    /** Asks for the exact outline of any quick strokes the page still has. */
    fun settle(pageId: String, pageIndex: Int) {
        if (cache.scenes[pageId]?.hasQuick == true) request(pageId, pageIndex, true)
    }

    fun shutdown() {
        epoch++
        gate.clear()
        executor?.shutdownNow()
        executor = null
    }

    private fun start(pageId: String, pageIndex: Int, upgrade: Boolean) {
        val base = cache.scenes[pageId]
        val myEpoch = epoch
        val ex = executor?.takeIf { !it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executor = it }
        try {
            ex.execute { work(pageId, pageIndex, base, upgrade, myEpoch) }
        } catch (_: RejectedExecutionException) {
            // Shut down in between; the next request starts a new thread.
            gate.complete(pageId)
        }
    }

    private fun work(pageId: String, pageIndex: Int, base: PageScene?, upgrade: Boolean, myEpoch: Int) {
        val t0 = EraseLog.nowNs()
        val load = try {
            cache.compute(pageIndex, base, if (upgrade) UPGRADE_CHUNK else 0)
        } catch (e: RuntimeException) {
            Log.e(EraseLog.TAG, "geometry refresh failed", e)
            null
        }
        if (load != null) {
            val ms = EraseLog.elapsedMs(t0)
            if (ms > EraseLog.REFRESH_MS) {
                Log.w(EraseLog.TAG, "geometry refresh took $ms ms (${load.loaded} strokes, ${load.vertices} vertices)")
            }
        }
        post(Runnable { finish(pageId, pageIndex, base, load, upgrade, myEpoch) })
    }

    private fun finish(
        pageId: String,
        pageIndex: Int,
        base: PageScene?,
        load: PageLoad?,
        upgrade: Boolean,
        myEpoch: Int,
    ) {
        if (myEpoch != epoch) return
        var changed = false
        if (load != null) {
            changed = cache.publish(pageId, base, load.scene)
            // Whether published or not, an upgrade is not finished while quick strokes remain.
            if (upgrade && cache.scenes[pageId]?.hasQuick == true) gate.submit(pageId, true)
        }
        val next = gate.complete(pageId)
        if (next != null) start(pageId, pageIndex, next)
        if (changed) onPublished()
    }

    private companion object {
        /** Quick strokes rebuilt exactly per background pass, so a pass stays short and progress is kept. */
        const val UPGRADE_CHUNK = 32
    }
}
