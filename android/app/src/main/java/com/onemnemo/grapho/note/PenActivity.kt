package com.onemnemo.grapho.note

import android.os.SystemClock

/** The last time the pen touched or hovered over the note, so a palm that follows it can be told from a finger. */
object PenActivity {
    @Volatile
    var lastMs: Long = Long.MIN_VALUE / 2

    fun note(timeMs: Long = SystemClock.uptimeMillis()) {
        lastMs = timeMs
    }
}
