package com.onemnemo.grapho.ui.note

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A squiggle: whole for taking the stroke, with a gap for cutting only part of it. Shared by the wheel and the card. */
internal fun DrawScope.drawEraserModeGlyph(area: Boolean, ink: Color) {
    val w = size.width
    val h = size.height
    val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
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

/** A dashed loop for the freehand lasso, a dashed box for the rectangle. Shared by the wheel and the card. */
internal fun DrawScope.drawLassoModeGlyph(rect: Boolean, ink: Color) {
    val dash = Stroke(
        width = 2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )
    val w = size.width
    val h = size.height
    if (rect) {
        drawRect(ink, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.6f), style = dash)
    } else {
        val loop = Path().apply {
            moveTo(w * 0.15f, h * 0.55f)
            cubicTo(w * 0.1f, h * 0.1f, w * 0.7f, h * 0.05f, w * 0.85f, h * 0.4f)
            cubicTo(w * 0.95f, h * 0.75f, w * 0.35f, h * 0.95f, w * 0.15f, h * 0.55f)
        }
        drawPath(loop, ink, style = dash)
    }
}
