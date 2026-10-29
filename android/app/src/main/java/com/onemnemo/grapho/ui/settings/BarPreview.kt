package com.onemnemo.grapho.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.onemnemo.grapho.ui.note.BarPosition
import com.onemnemo.grapho.ui.note.NoteBarStyle
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private const val RATIO = 1.6f
private const val RULE_STEP = 0.2f

private const val PILL_THICKNESS = 0.17f
private const val PILL_LENGTH = 0.56f
private const val PILL_GAP = 0.1f
private const val LINE_THICKNESS = 0.16f

/**
 * A ruled page with the note bar drawn on it in one style, against the edge [position] names, as the picture of
 * that choice.
 */
@Composable
internal fun BarPreview(style: NoteBarStyle, modifier: Modifier = Modifier, position: BarPosition = BarPosition.Top) {
    val c = Grapho.colors
    val hairline = GraphoSpace.borderWidthHairline
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(RATIO)
            .clip(GraphoShapes.item)
            .border(hairline, c.borderSubtle, GraphoShapes.item),
    ) {
        val w = size.width
        val h = size.height
        drawRect(c.canvasPage)
        var y = h * RULE_STEP
        while (y < h) {
            drawLine(c.canvasRule, Offset(0f, y), Offset(w, y), hairline.toPx())
            y += h * RULE_STEP
        }
        val stroke = hairline.toPx()
        if (style == NoteBarStyle.Floating) {
            val pill = floatingRect(position, w, h)
            val corner = CornerRadius(minOf(pill.width, pill.height) / 2f)
            drawRoundRect(c.surfaceOverlay, pill.topLeft, pill.size, corner)
            drawRoundRect(c.borderStrong, pill.topLeft, pill.size, corner, style = Stroke(stroke))
        } else {
            val bar = lineRect(position, w, h)
            drawRect(c.surfaceRaised, bar.topLeft, bar.size)
            val (from, to) = lineEdge(position, bar)
            drawLine(c.borderStrong, from, to, stroke)
        }
    }
}

/** The floating pill against the [position] edge of a [w] by [h] page, centred along it. */
private fun floatingRect(position: BarPosition, w: Float, h: Float): Rect {
    val thick = h * PILL_THICKNESS
    val gap = h * PILL_GAP
    return when (position) {
        BarPosition.Top -> Rect(Offset((w - w * PILL_LENGTH) / 2f, gap), Size(w * PILL_LENGTH, thick))
        BarPosition.Bottom -> Rect(Offset((w - w * PILL_LENGTH) / 2f, h - gap - thick), Size(w * PILL_LENGTH, thick))
        BarPosition.Left -> Rect(Offset(gap, (h - h * PILL_LENGTH) / 2f), Size(thick, h * PILL_LENGTH))
        BarPosition.Right -> Rect(Offset(w - gap - thick, (h - h * PILL_LENGTH) / 2f), Size(thick, h * PILL_LENGTH))
    }
}

/** The flat bar along the whole [position] edge of a [w] by [h] page. */
private fun lineRect(position: BarPosition, w: Float, h: Float): Rect {
    val thick = h * LINE_THICKNESS
    return when (position) {
        BarPosition.Top -> Rect(Offset.Zero, Size(w, thick))
        BarPosition.Bottom -> Rect(Offset(0f, h - thick), Size(w, thick))
        BarPosition.Left -> Rect(Offset.Zero, Size(thick, h))
        BarPosition.Right -> Rect(Offset(w - thick, 0f), Size(thick, h))
    }
}

/** The two ends of the line along the flat bar's side that faces the page. */
private fun lineEdge(position: BarPosition, bar: Rect): Pair<Offset, Offset> = when (position) {
    BarPosition.Top -> bar.bottomLeft to bar.bottomRight
    BarPosition.Bottom -> bar.topLeft to bar.topRight
    BarPosition.Left -> bar.topRight to bar.bottomRight
    BarPosition.Right -> bar.topLeft to bar.bottomLeft
}
