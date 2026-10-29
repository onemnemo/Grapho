package com.onemnemo.grapho.note

import kotlin.math.hypot

/**
 * Tells a tap with several fingers apart from a pan, a pinch or a resting hand. Positions and sizes are view pixels
 * and times milliseconds. Every finger has to land within [LAND_MS] of the first, before any of them lifts, none
 * may travel further than [slop] or touch wider than [maxContact], all of them must fit within [maxSpread] of each
 * other, no pen may touch, nothing may be called off as a palm, and the last finger has to lift within [TAP_MS] of
 * the first landing. UI thread only.
 */
internal class FingerTap(
    private val slop: Float,
    private val maxContact: Float = Float.POSITIVE_INFINITY,
    private val maxSpread: Float = Float.POSITIVE_INFINITY,
) {
    private val ids = IntArray(MAX_FINGERS)
    private val startX = FloatArray(MAX_FINGERS)
    private val startY = FloatArray(MAX_FINGERS)
    private var count = 0
    private var down = 0
    private var firstMs = 0L
    private var lifting = false
    private var failed = true

    /** A finger or pen lands, [contact] wide; the first one down starts a new tap. */
    fun down(id: Int, x: Float, y: Float, timeMs: Long, pen: Boolean, contact: Float = 0f) {
        if (down == 0) {
            count = 0
            firstMs = timeMs
            lifting = false
            failed = false
        }
        down++
        if (pen || lifting || count == MAX_FINGERS || timeMs - firstMs > LAND_MS || contact > maxContact) {
            failed = true
            return
        }
        // Fingers of one hand land close together; a palm and a fingertip are far apart.
        for (i in 0 until count) {
            if (hypot(x - startX[i], y - startY[i]) > maxSpread) failed = true
        }
        ids[count] = id
        startX[count] = x
        startY[count] = y
        count++
    }

    /** A finger is at ([x], [y]), [contact] wide; moving too far makes a pan or a pinch, growing wide a palm. */
    fun move(id: Int, x: Float, y: Float, contact: Float = 0f) {
        if (failed) return
        if (contact > maxContact) failed = true
        val i = ids.indexOf(id)
        if (i in 0 until count && hypot(x - startX[i], y - startY[i]) > slop) failed = true
    }

    /**
     * A finger lifts, or the system [canceled] it as a palm. Returns how many fingers tapped once the last one is
     * up, or 0 when this was no tap.
     */
    fun up(timeMs: Long, canceled: Boolean = false): Int {
        if (canceled) failed = true
        lifting = true
        down = (down - 1).coerceAtLeast(0)
        if (down > 0) return 0
        val fingers = if (failed || timeMs - firstMs > TAP_MS) 0 else count
        failed = true
        return if (fingers >= MIN_FINGERS) fingers else 0
    }

    /** The touch was taken away; nothing counts until every finger is up and a new tap begins. */
    fun cancel() {
        down = 0
        failed = true
    }

    companion object {
        const val MIN_FINGERS = 2
        const val MAX_FINGERS = 5
        const val LAND_MS = 150L
        const val TAP_MS = 350L
    }
}
