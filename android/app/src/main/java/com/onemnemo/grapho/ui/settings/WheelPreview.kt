package com.onemnemo.grapho.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import com.onemnemo.grapho.ui.note.DockEdge
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private const val RATIO = 1.6f
private const val RULE_STEP = 0.2f
private const val DISC_RADIUS = 0.3f

/** A ruled page with the pen wheel peeking in as a half disc from the [edge] it is docked on. */
@Composable
internal fun WheelPreview(edge: DockEdge, modifier: Modifier = Modifier) {
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
        val r = h * DISC_RADIUS
        val centre = when (edge) {
            DockEdge.Left -> Offset(0f, h / 2f)
            DockEdge.Right -> Offset(w, h / 2f)
            DockEdge.Top -> Offset(w / 2f, 0f)
            DockEdge.Bottom -> Offset(w / 2f, h)
        }
        drawCircle(c.surfaceOverlay, r, centre)
        drawCircle(c.borderStrong, r, centre, style = Stroke(hairline.toPx()))
        drawCircle(c.borderStrong, r * 0.35f, centre, style = Stroke(hairline.toPx()))
    }
}
