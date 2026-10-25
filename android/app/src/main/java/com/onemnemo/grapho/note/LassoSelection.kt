package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.atan2

/** Strokes of one page that a render leaves out because the lasso is holding them or an editor has them open. */
class HiddenStrokes(
    val page: Int,
    val pageId: String,
    val ids: Set<StrokeId>,
    /** Items a render leaves out that the lasso does not hold, such as a text box being typed into. */
    val quiet: Set<StrokeId> = emptySet(),
) {
    /** Everything a render leaves out. */
    val all: Set<StrokeId> = if (quiet.isEmpty()) ids else ids + quiet
}

/**
 * Where the selection sits on screen, in view pixels, once nothing is moving. [canColour] is false when it holds only
 * images, which have no colour to change. [canCrop] is true when it is exactly one image. [width] is the stroke
 * width most of the selection is drawn at, in whole points, or 0 when it has none.
 */
class SelectionMenu(
    val bounds: RectF,
    val canColour: Boolean = true,
    val canCrop: Boolean = false,
    val width: Float = 0f,
)

/**
 * The strokes and images the lasso holds, by id. Immutable: the UI thread replaces it as a whole and the render
 * thread reads it. [bounds] is the box of their ink and pictures in page-local units before [matrix], the move, scale
 * or turn being dragged. A [fresh] selection holds items that no render has seen yet, so they are drawn from the
 * scene until one does. [generation] is the edit count they appeared at.
 */
class LassoSelection(
    val page: Int,
    val pageId: String,
    val flat: LongArray,
    val bounds: RectF,
    val generation: Int,
    val fresh: Boolean,
    val matrix: Matrix = Matrix(),
    val dragging: Boolean = false,
    val hidden: HiddenStrokes = HiddenStrokes(page, pageId, idsOf(flat)),
) {
    val ids: Set<StrokeId> get() = hidden.ids
    val moved: Boolean = !matrix.isIdentity

    /** Degrees the drag has turned the selection by, about the middle of [bounds]; 0 for a move or scale. */
    val angle: Float = if (moved) turnOf(matrix) else 0f
    val turned: Boolean = abs(angle) > MIN_TURN

    fun with(matrix: Matrix, dragging: Boolean) =
        LassoSelection(page, pageId, flat, bounds, generation, fresh, matrix, dragging, hidden)

    /**
     * The box in view pixels with the drag applied, grown by [pad]. While the selection is being turned this is the
     * box before the turn, to be drawn rotated by [angle] about its centre.
     */
    fun viewBox(vp: Viewport, pad: Float, out: RectF): RectF {
        out.set(bounds)
        if (!turned) matrix.mapRect(out)
        out.set(
            vp.viewX(page, out.left),
            vp.viewY(page, out.top),
            vp.viewX(page, out.right),
            vp.viewY(page, out.bottom),
        )
        out.inset(-pad, -pad)
        return out
    }

    companion object {
        private const val MIN_TURN = 0.001f

        private fun turnOf(matrix: Matrix): Float {
            val v = FloatArray(9)
            matrix.getValues(v)
            return Math.toDegrees(atan2(v[Matrix.MSKEW_Y].toDouble(), v[Matrix.MSCALE_X].toDouble())).toFloat()
        }

        fun idsOf(flat: LongArray): Set<StrokeId> {
            val out = HashSet<StrokeId>(flat.size)
            var i = 0
            while (i + 1 < flat.size) {
                out.add(StrokeId(flat[i], flat[i + 1]))
                i += 2
            }
            return out
        }
    }
}

/**
 * The lasso being drawn, as page-local points `[x, y, ...]`. One writer (the UI thread) appends; the render
 * thread reads whatever has been written so far.
 */
class LassoTrace(val page: Int) {
    @Volatile var points = FloatArray(256)
        private set

    @Volatile var size = 0
        private set

    /** The outline is drawn shut, back to its first corner, as a rectangle's is. */
    @Volatile var closed = false
        private set

    fun add(x: Float, y: Float) {
        var p = points
        if (size + 2 > p.size) {
            p = p.copyOf(p.size * 2)
            points = p
        }
        p[size] = x
        p[size + 1] = y
        size += 2
    }

    /** Replaces the trace with the four corners of the rectangle from ([x0], [y0]) to ([x1], [y1]). */
    fun setRectangle(x0: Float, y0: Float, x1: Float, y1: Float) {
        closed = true
        size = 0
        add(x0, y0)
        add(x1, y0)
        add(x1, y1)
        add(x0, y1)
    }
}
