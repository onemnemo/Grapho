package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

enum class PenKind { Pen, Highlighter }

/**
 * A standing pen drawn in the 16 x 44 box from the design. The tip is filled
 * with [color], so it always shows what you would write with.
 */
@Composable
fun PenGlyph(kind: PenKind, color: Color, outline: Color, modifier: Modifier = Modifier, outlineWidth: Float = 0.9f) {
    Canvas(modifier) {
        val s = size.height / 44f
        withTransform({
            translate(left = (size.width - 16f * s) / 2f, top = 0f)
            scale(s, s, Offset.Zero)
        }) {
            val bodyColor = if (kind == PenKind.Pen) Color(0xFFFFFFFF) else Color(0xFFFFF7D6)
            val corner = CornerRadius(2f, 2f)
            val bodyTopLeft = Offset(3.5f, 10f)
            val bodySize = Size(9f, 36f)
            drawRoundRect(bodyColor, bodyTopLeft, bodySize, corner)
            drawRect(color, Offset(3.5f, 16f), Size(9f, 3f))

            val tip = Path()
            if (kind == PenKind.Pen) {
                tip.moveTo(5f, 10.5f)
                tip.lineTo(8f, 3f)
                tip.lineTo(11f, 10.5f)
            } else {
                tip.moveTo(3.5f, 10.5f)
                tip.lineTo(3.5f, 4.5f)
                tip.lineTo(12.5f, 1.5f)
                tip.lineTo(12.5f, 10.5f)
            }
            tip.close()
            drawPath(tip, color)
            drawPath(tip, outline, style = Stroke(width = outlineWidth))
            drawRoundRect(outline, bodyTopLeft, bodySize, corner, style = Stroke(width = outlineWidth))
        }
    }
}
