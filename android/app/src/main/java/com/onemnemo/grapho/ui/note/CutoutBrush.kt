package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** How a cut-out is touched up: [Auto] leaves it to the cut, the brushes paint by hand. */
internal enum class CutoutTool { Auto, Erase, Restore }

/**
 * One drag of a brush. [points] are x, y pairs and [radius] is the brush's, both as fractions of the source picture's
 * width and height, so they stay put when the view moves.
 */
internal class BrushStroke(val erase: Boolean, val radius: Float) {
    val points = ArrayList<Float>()
}

/** The maths behind the brushes, kept apart from the screen. */
internal object BrushMath {
    /** The place [local] along a shown extent of [extent] is, as a fraction of the source, for a crop of [from] to [to]. */
    fun fractionOf(local: Float, extent: Float, from: Float, to: Float): Float =
        from + (if (extent > 0f) (local / extent).coerceIn(0f, 1f) else 0f) * (to - from)

    /** The inverse of [fractionOf]: where a source fraction lands along the shown extent. */
    fun localOf(fraction: Float, extent: Float, from: Float, to: Float): Float =
        if (to > from) (fraction - from) / (to - from) * extent else 0f

    /**
     * Paints [strokes] onto [cut] (straight `0xAARRGGBB`, [width] by [height]): an erasing stroke makes what it covers
     * see through, a restoring one takes it from [original], which is the same size.
     */
    fun apply(strokes: List<BrushStroke>, cut: IntArray, original: IntArray, width: Int, height: Int) {
        for (s in strokes) {
            val r = s.radius * width
            var i = 0
            var lastX = 0f
            var lastY = 0f
            while (i + 1 < s.points.size) {
                val x = s.points[i] * width
                val y = s.points[i + 1] * height
                if (i == 0) {
                    stamp(s.erase, cut, original, width, height, x, y, r)
                } else {
                    val steps = max(1, (hypot(x - lastX, y - lastY) / max(1f, r / 2f)).toInt())
                    for (k in 1..steps) {
                        val t = k.toFloat() / steps
                        stamp(
                            s.erase,
                            cut,
                            original,
                            width,
                            height,
                            lastX + (x - lastX) * t,
                            lastY + (y - lastY) * t,
                            r,
                        )
                    }
                }
                lastX = x
                lastY = y
                i += 2
            }
        }
    }

    private fun stamp(
        erase: Boolean,
        cut: IntArray,
        original: IntArray,
        width: Int,
        height: Int,
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        val x0 = max(0, (cx - r).toInt())
        val x1 = min(width - 1, (cx + r).roundToInt())
        val y0 = max(0, (cy - r).toInt())
        val y1 = min(height - 1, (cy + r).roundToInt())
        val r2 = r * r
        for (y in y0..y1) {
            for (x in x0..x1) {
                val dx = x + 0.5f - cx
                val dy = y + 0.5f - cy
                if (dx * dx + dy * dy > r2) continue
                val at = y * width + x
                cut[at] = if (erase) cut[at] and 0x00FFFFFF else original[at]
            }
        }
    }
}

/**
 * The hand brushes for a cut-out: which [tool] is active, how big the brush is, and the strokes painted so far. The
 * strokes are only applied to the picture when the person finishes.
 */
@Stable
internal class CutoutBrush {
    var tool by mutableStateOf(CutoutTool.Auto)
    var size by mutableStateOf(DEFAULT_SIZE)

    /** Counts changes to the strokes, so what draws them knows to draw again. */
    var revision by mutableStateOf(0)
        private set

    val strokes = ArrayList<BrushStroke>()
    private var width = 1f
    private var height = 1f
    private var crop = RectF(0f, 0f, 1f, 1f)

    /** Starts a stroke at ([x], [y]) in a shown area of [areaWidth] by [areaHeight] pixels, [radius] pixels wide. */
    fun begin(x: Float, y: Float, areaWidth: Float, areaHeight: Float, shown: RectF, radius: Float) {
        if (tool == CutoutTool.Auto || areaWidth <= 0f || areaHeight <= 0f) return
        width = areaWidth
        height = areaHeight
        crop = shown
        strokes.add(BrushStroke(tool == CutoutTool.Erase, radius / areaWidth * (shown.right - shown.left)))
        extend(x, y)
    }

    fun extend(x: Float, y: Float) {
        val stroke = strokes.lastOrNull() ?: return
        stroke.points.add(BrushMath.fractionOf(x, width, crop.left, crop.right))
        stroke.points.add(BrushMath.fractionOf(y, height, crop.top, crop.bottom))
        revision++
    }

    /** Leaves the brushes and hands back what was painted. */
    fun finish(): List<BrushStroke> {
        val done = ArrayList(strokes)
        strokes.clear()
        tool = CutoutTool.Auto
        revision++
        return done
    }

    companion object {
        /** The brush's width on screen in dp, and its range. */
        const val DEFAULT_SIZE = 32f
        const val MIN_SIZE = 8f
        const val MAX_SIZE = 96f
    }
}
