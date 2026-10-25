package com.onemnemo.grapho.note

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Tells a swipe that pulls the system bars or the taskbar in from the top or bottom edge apart from a stroke that
 * merely starts there. Positions are view pixels and times milliseconds. A touch that lands above [top] or below
 * [bottom] is held: moving quickly and mostly straight away from its edge makes it a swipe, anything else (moving
 * sideways or slowly, resting, lifting) makes it a stroke. Writing near the edges keeps working; only its first
 * [DECIDE_MS] wait for the answer.
 */
internal class EdgeGuard(private val slop: Float) {
    enum class Verdict { WAIT, DRAW, SWIPE }

    private var top = Float.NEGATIVE_INFINITY
    private var bottom = Float.POSITIVE_INFINITY
    private var startX = 0f
    private var startY = 0f
    private var startMs = 0L
    private var fromTop = false

    /** Sets the guarded bands; pass negative infinity or positive infinity to leave an edge unguarded. */
    fun setBands(top: Float, bottom: Float) {
        this.top = top
        this.bottom = bottom
    }

    /** Starts guarding a touch that lands in a band; false if it is clear of both. */
    fun begin(x: Float, y: Float, timeMs: Long): Boolean {
        if (y >= top && y <= bottom) return false
        startX = x
        startY = y
        startMs = timeMs
        fromTop = y < top
        return true
    }

    fun move(x: Float, y: Float, timeMs: Long): Verdict {
        val dx = x - startX
        val dy = y - startY
        val quick = timeMs - startMs < DECIDE_MS
        if (hypot(dx, dy) <= slop) return if (quick) Verdict.WAIT else Verdict.DRAW
        val inward = if (fromTop) dy > 0 else dy < 0
        return if (quick && inward && abs(dy) > STEEPNESS * abs(dx)) Verdict.SWIPE else Verdict.DRAW
    }

    companion object {
        /** A swipe must leave the touch slop within this long; it is also the longest a held touch waits. */
        const val DECIDE_MS = 180L

        /** How much more a swipe moves across the edge than along it. */
        const val STEEPNESS = 2f
    }
}
