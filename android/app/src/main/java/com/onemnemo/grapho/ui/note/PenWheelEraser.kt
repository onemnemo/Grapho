package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** The eraser's size dots sit on the rim where the pen's size dots do, in the left-docked layout. */
internal const val ERASER_FROM = -86f
internal const val ERASER_TO = -18f

/** Where the two mode buttons sit on the rim, below the sizes: whole strokes, then part of a stroke. */
private const val WHOLE_AT = 40f
private const val PART_AT = 74f

/** The eraser sizes the dots stand for, in points, roughly even on a log scale from the smallest to the largest. */
private val ERASER_STEPS = listOf(4f, 7f, 12f, 20f, 35f, 60f)

private fun stepAngle(index: Int): Float = ERASER_FROM + (ERASER_TO - ERASER_FROM) * index / (ERASER_STEPS.size - 1)

/** Radius in dp of the dot at [index]: small to large like the pen's size dots. */
private fun stepRadius(index: Int): Float = dotRadius(1f + 99f * index / (ERASER_STEPS.size - 1))

private fun nearestStep(pt: Float): Int {
    var best = 0
    var bestDistance = Float.MAX_VALUE
    for (i in ERASER_STEPS.indices) {
        val d = abs(ln(pt / ERASER_STEPS[i]))
        if (d < bestDistance) {
            best = i
            bestDistance = d
        }
    }
    return best
}

/** The eraser diameter as the whole number the bubble shows. */
internal fun eraserNumber(state: ToolbarState): Int = state.eraserPt.roundToInt()

/** The size at [degrees] on the rim while dragging: continuous, on a log scale between the smallest and the largest. */
internal fun eraserPtAt(degrees: Float): Float {
    val t = ((degrees - ERASER_FROM) / (ERASER_TO - ERASER_FROM)).coerceIn(0f, 1f)
    return ERASER_MIN_PT * (ERASER_MAX_PT / ERASER_MIN_PT).pow(t)
}

/** The size of the dot closest to [degrees] on the rim, or null when none is near. */
internal fun eraserStepAt(degrees: Float): Float? {
    var best: Float? = null
    var bestDistance = 7f
    for (i in ERASER_STEPS.indices) {
        val d = abs(stepAngle(i) - degrees)
        if (d <= bestDistance) {
            best = ERASER_STEPS[i]
            bestDistance = d
        }
    }
    return best
}

/** The eraser's size dots along the rim with the current one ringed, drawn like the pen's size dots. */
internal fun DrawScope.drawEraserSizes(geo: Geo, c: Offset, u: Float, pt: Float) {
    fun at(index: Int): Offset {
        val p = polar(geo.rimR, geo.edge.toScreen(stepAngle(index)))
        return Offset(c.x + p.x * u, c.y + p.y * u)
    }
    for (i in ERASER_STEPS.indices) {
        drawCircle(GraphoWheel.dot, stepRadius(i) * u, at(i))
    }
    val selected = nearestStep(pt)
    val sp = at(selected)
    drawCircle(GraphoWheel.selectedIndicator, stepRadius(selected) * u, sp)
    drawCircle(GraphoWheel.selectedIndicator, (stepRadius(selected) + 5f) * u, sp, style = Stroke(width = 2f * u))
}

/** The two buttons that choose between taking whole strokes and cutting only the part passed over. */
@Composable
internal fun EraserModeButtons(geo: Geo, state: ToolbarState) {
    for (area in listOf(false, true)) {
        val p = polar(geo.rimR, geo.edge.toScreen(if (area) PART_AT else WHOLE_AT))
        WheelButton(
            cx = geo.cx + p.x,
            cy = geo.cy + p.y,
            label = stringResource(if (area) R.string.wheel_eraser_part else R.string.wheel_eraser_whole),
            onClick = { state.eraserArea = area },
            active = state.eraserArea == area,
        ) {
            EraserModeGlyph(area = area, selected = state.eraserArea == area)
        }
    }
}

/** A squiggle: whole for taking the stroke, with a gap for cutting part of it. Ringed when chosen. */
@Composable
private fun EraserModeGlyph(area: Boolean, selected: Boolean) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val ink = if (selected) GraphoWheel.text else GraphoWheel.icon
        if (area) {
            val left = Path().apply {
                moveTo(w * 0.08f, h * 0.6f)
                cubicTo(w * 0.18f, h * 0.2f, w * 0.3f, h * 0.2f, w * 0.38f, h * 0.45f)
            }
            val right = Path().apply {
                moveTo(w * 0.62f, h * 0.6f)
                cubicTo(w * 0.7f, h * 0.85f, w * 0.82f, h * 0.8f, w * 0.92f, h * 0.4f)
            }
            drawPath(left, ink, style = line)
            drawPath(right, ink, style = line)
        } else {
            val whole = Path().apply {
                moveTo(w * 0.08f, h * 0.6f)
                cubicTo(w * 0.18f, h * 0.2f, w * 0.32f, h * 0.2f, w * 0.5f, h * 0.52f)
                cubicTo(w * 0.66f, h * 0.84f, w * 0.82f, h * 0.8f, w * 0.92f, h * 0.4f)
            }
            drawPath(whole, ink, style = line)
        }
    }
}
