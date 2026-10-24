package com.onemnemo.grapho.note

import android.util.Log

/** Warnings for the slow steps of erasing, so a tablet that stutters can say where. */
object EraseLog {
    const val TAG = "GraphoErase"

    /** One erase input event, handled on the UI thread. */
    const val INPUT_MS = 8L

    /** One background geometry refresh. */
    const val REFRESH_MS = 50L

    /** One render of the finished pages. */
    const val DRY_MS = 50L

    fun nowNs(): Long = System.nanoTime()

    fun elapsedMs(startNs: Long): Long = (System.nanoTime() - startNs) / NS_PER_MS

    fun slow(what: String, ms: Long, limitMs: Long) {
        if (ms > limitMs) Log.w(TAG, "$what took $ms ms")
    }

    private const val NS_PER_MS = 1_000_000L
}
