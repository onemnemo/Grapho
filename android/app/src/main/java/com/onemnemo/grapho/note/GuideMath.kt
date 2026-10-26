package com.onemnemo.grapho.note

import kotlin.math.IEEErem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal enum class GuideKind { RULER, PROTRACTOR }

/**
 * Where a guide sits on the content, in points. [turn] is the angle the fingers have turned it to, in radians, and
 * [angle] is that turn rounded for display and for drawing against. [label] is the ruler's angle in whole degrees.
 */
internal data class GuidePlace(val x: Float, val y: Float, val turn: Float, val angle: Float, val label: Int)

/** The guides in use. Immutable, so the render thread can keep one while input builds the next. */
internal class GuideSet(val ruler: GuidePlace? = null, val protractor: GuidePlace? = null) {
    val isEmpty: Boolean get() = ruler == null && protractor == null

    fun place(kind: GuideKind): GuidePlace? = if (kind == GuideKind.RULER) ruler else protractor

    fun with(kind: GuideKind, place: GuidePlace?): GuideSet =
        if (kind == GuideKind.RULER) GuideSet(place, protractor) else GuideSet(ruler, place)
}

/** Sizes and geometry of the ruler and protractor, all in points so they match the paper at any zoom. */
internal object GuideMath {
    const val RULER_LENGTH = 300f * NotebookInfo.PT_PER_MM
    const val RULER_THICKNESS = 32f * NotebookInfo.PT_PER_MM
    const val PROTRACTOR_RADIUS = 70f * NotebookInfo.PT_PER_MM

    /** Where the close button sits: along the ruler from its middle, or up the protractor from its centre. */
    const val RULER_CLOSE = 2.2f * RULER_THICKNESS
    const val PROTRACTOR_CLOSE = 0.45f * PROTRACTOR_RADIUS

    private const val KIND_RULER = 0f
    private const val KIND_PROTRACTOR = 1f

    /** Floats per packed guide, matching grapho-core. */
    const val STRIDE = 6

    /**
     * The guides packed for the engine in view pixels, for a view mapping content points with [scale], [offsetX] and
     * [offsetY].
     */
    fun pack(set: GuideSet, scale: Float, offsetX: Float, offsetY: Float): FloatArray {
        val out = ArrayList<Float>(2 * STRIDE)
        set.ruler?.let {
            addGuide(out, KIND_RULER, it, scale, offsetX, offsetY, RULER_LENGTH * scale, RULER_THICKNESS * scale)
        }
        set.protractor?.let {
            addGuide(out, KIND_PROTRACTOR, it, scale, offsetX, offsetY, PROTRACTOR_RADIUS * scale, 0f)
        }
        return out.toFloatArray()
    }

    private fun addGuide(
        out: MutableList<Float>,
        kind: Float,
        p: GuidePlace,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        a: Float,
        b: Float,
    ) {
        out.add(kind)
        out.add(p.x * scale + offsetX)
        out.add(p.y * scale + offsetY)
        out.add(p.angle)
        out.add(a)
        out.add(b)
    }

    /**
     * Puts the view point ([x], [y]) in [out] as `[u, v]` pixels from the guide's centre, along its own axes: u runs
     * along the ruler or the protractor's base, and v is across it, positive on the side the protractor's disc is not.
     */
    fun local(p: GuidePlace, scale: Float, offsetX: Float, offsetY: Float, x: Float, y: Float, out: FloatArray) {
        val dx = x - (p.x * scale + offsetX)
        val dy = y - (p.y * scale + offsetY)
        val c = cos(p.angle)
        val s = sin(p.angle)
        out[0] = dx * c + dy * s
        out[1] = -dx * s + dy * c
    }

    /** True when the view point is on the guide's body, or within [marginPx] of it. */
    fun hits(
        kind: GuideKind,
        p: GuidePlace,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        x: Float,
        y: Float,
        marginPx: Float,
        scratch: FloatArray,
    ): Boolean {
        local(p, scale, offsetX, offsetY, x, y, scratch)
        val (u, v) = scratch
        return when (kind) {
            GuideKind.RULER ->
                abs(u) <= RULER_LENGTH * scale / 2 + marginPx &&
                    abs(v) <= RULER_THICKNESS * scale / 2 + marginPx

            GuideKind.PROTRACTOR -> {
                val r = PROTRACTOR_RADIUS * scale + marginPx
                v <= marginPx && hypot(u, v) <= r
            }
        }
    }

    /** True when the view point is within [radiusPx] of the guide's close button. */
    fun hitsClose(
        kind: GuideKind,
        p: GuidePlace,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        x: Float,
        y: Float,
        radiusPx: Float,
        scratch: FloatArray,
    ): Boolean {
        local(p, scale, offsetX, offsetY, x, y, scratch)
        val (u, v) = scratch
        val (cu, cv) = if (kind == GuideKind.RULER) {
            RULER_CLOSE * scale to 0f
        } else {
            0f to -PROTRACTOR_CLOSE * scale
        }
        return hypot(u - cu, v - cv) <= radiusPx
    }

    /**
     * Moves a guide the way the fingers moved. [from] and [to] hold the pointers' view positions as `[x0, y0, x1,
     * y1]`, [count] of them (1 or 2) counted. One finger drags; two drag by their middle and turn by the angle
     * between them, about that middle. Puts the new centre in content points and the new turn in [out] as
     * `[x, y, turn]`.
     */
    fun move(
        p: GuidePlace,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        from: FloatArray,
        to: FloatArray,
        count: Int,
        out: FloatArray,
    ) {
        if (count < 2) {
            out[0] = p.x + (to[0] - from[0]) / scale
            out[1] = p.y + (to[1] - from[1]) / scale
            out[2] = p.turn
            return
        }
        val oldAngle = atan2(from[3] - from[1], from[2] - from[0])
        val newAngle = atan2(to[3] - to[1], to[2] - to[0])
        val turn = (newAngle - oldAngle).IEEErem(2 * PI.toFloat())
        val oldX = (from[0] + from[2]) / 2
        val oldY = (from[1] + from[3]) / 2
        val newX = (to[0] + to[2]) / 2
        val newY = (to[1] + to[3]) / 2
        val vx = p.x * scale + offsetX - oldX
        val vy = p.y * scale + offsetY - oldY
        val c = cos(turn)
        val s = sin(turn)
        out[0] = (newX + vx * c - vy * s - offsetX) / scale
        out[1] = (newY + vx * s + vy * c - offsetY) / scale
        out[2] = p.turn + turn
    }
}
