package com.onemnemo.grapho.note

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

/**
 * Notices when the UI thread stops answering and logs where every thread
 * is, so a freeze can be traced from its log. Runs while a note is open.
 */
class StallWatch {
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var lastBeat = SystemClock.uptimeMillis()

    @Volatile private var running = false
    private var thread: Thread? = null

    private val beat = Runnable { lastBeat = SystemClock.uptimeMillis() }

    fun start() {
        if (running) return
        running = true
        lastBeat = SystemClock.uptimeMillis()
        thread = Thread(::watch, "GraphoStallWatch").also {
            it.isDaemon = true
            it.start()
        }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun watch() {
        var reported = 0L
        while (running) {
            main.post(beat)
            try {
                Thread.sleep(CHECK_MS)
            } catch (_: InterruptedException) {
                return
            }
            val stalled = SystemClock.uptimeMillis() - lastBeat
            if (stalled >= STALL_MS && lastBeat != reported) {
                reported = lastBeat
                Log.e(EraseLog.TAG, "UI thread stalled for $stalled ms")
                dumpThreads()
            } else if (stalled < CHECK_MS * 2 && reported != 0L) {
                Log.w(EraseLog.TAG, "UI thread answers again")
                reported = 0L
            }
        }
    }

    private fun dumpThreads() {
        for ((t, stack) in Thread.getAllStackTraces()) {
            if (stack.isEmpty()) continue
            val lines = stack.take(MAX_FRAMES).joinToString("\n") { "    at $it" }
            Log.e(EraseLog.TAG, "thread ${t.name} (${t.state}):\n$lines")
        }
    }

    private companion object {
        const val CHECK_MS = 500L
        const val STALL_MS = 2_000L
        const val MAX_FRAMES = 25
    }
}
