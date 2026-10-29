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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.onemnemo.grapho.ui.theme.FolderColors
import com.onemnemo.grapho.ui.theme.GraphoColors
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private const val RATIO = 1.6f

/**
 * A small picture of the library drawn with the real colours of a theme, so a tile can never drift from the app.
 * Every size is a share of the width, so it scales to any tile. The unit [u] is one percent of the width.
 */
@Composable
fun ThemePreview(colors: GraphoColors, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(RATIO)
            .clip(GraphoShapes.item)
            .border(GraphoSpace.borderWidthHairline, colors.borderSubtle, GraphoShapes.item),
    ) {
        val u = size.width / 100f
        drawRect(colors.surfaceRaised)
        drawHead(colors, u)
        drawFolders(u)
        drawNotes(colors, u)
        drawControls(colors, u)
    }
}

private const val PAD = 5f
private const val GAP = 2.6f

private fun DrawScope.pill(color: Color, x: Float, y: Float, w: Float, h: Float, u: Float) =
    drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(h * u / 2f))

private fun DrawScope.block(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float, u: Float) =
    drawRoundRect(color, Offset(x * u, y * u), Size(w * u, h * u), CornerRadius(r * u))

private fun DrawScope.drawHead(c: GraphoColors, u: Float) {
    val h = 7.4f
    block(c.textDefault, PAD, PAD + (h - 3.2f) / 2f, 22f, 3.2f, 1.6f, u)
    pill(c.actionPrimaryBg, 100f - PAD - 14f, PAD, 14f, h, u)
    pill(c.surfaceControl, 100f - PAD - 14f - GAP - 26f, PAD, 26f, h, u)
}

private const val FOLDERS_TOP = 16.8f
private const val FOLDER_HEIGHT = 11f

private fun DrawScope.drawFolders(u: Float) {
    val tones = listOf(FolderColors.lilac, FolderColors.pink, FolderColors.teal)
    val w = (100f - 2f * PAD - 2f * GAP) / 3f
    tones.forEachIndexed { i, tone -> block(tone, PAD + i * (w + GAP), FOLDERS_TOP, w, FOLDER_HEIGHT, 2f, u) }
}

private const val NOTES_TOP = FOLDERS_TOP + FOLDER_HEIGHT + 4.4f
private const val NOTES_HEIGHT = 16.5f
private const val CHOSEN = 1

private fun DrawScope.drawNotes(c: GraphoColors, u: Float) {
    val w = (100f - 2f * PAD - 3f * GAP) / 4f
    for (i in 0 until 4) {
        val x = PAD + i * (w + GAP)
        if (i == CHOSEN) {
            // A gap in the screen colour, then a ring in the text colour, as a chosen note shows in the app.
            drawRoundRect(
                c.surfaceRaised,
                Offset((x - 0.45f) * u, (NOTES_TOP - 0.45f) * u),
                Size((w + 0.9f) * u, (NOTES_HEIGHT + 0.9f) * u),
                CornerRadius(2f * u),
                style = Stroke(0.9f * u),
            )
            drawRoundRect(
                c.textDefault,
                Offset((x - 1.2f) * u, (NOTES_TOP - 1.2f) * u),
                Size((w + 2.4f) * u, (NOTES_HEIGHT + 2.4f) * u),
                CornerRadius(2.4f * u),
                style = Stroke(0.6f * u),
            )
        }
        block(c.canvasPage, x, NOTES_TOP, w, NOTES_HEIGHT, 1.6f, u)
        val left = x + 2.2f
        val middle = NOTES_TOP + NOTES_HEIGHT / 2f
        block(c.canvasInkDefault.copy(alpha = 0.7f), left, middle - 3f, (w - 4.4f) * 0.6f, 1f, 0.5f, u)
        block(c.canvasRule, left, middle - 0.35f, w - 4.4f, 0.7f, 0.35f, u)
        block(c.canvasRule, left, middle + 2.5f, w - 4.4f, 0.7f, 0.35f, u)
    }
}

private fun DrawScope.drawControls(c: GraphoColors, u: Float) {
    val h = 4.4f
    val y = 100f / RATIO - PAD - h
    pill(c.progressFill, PAD, y, 8f, h, u)
    drawCircle(c.surfaceRaised, 1.6f * u, Offset((PAD + 8f - 0.6f - 1.6f) * u, (y + h / 2f) * u))
    val track = 30f
    val x = PAD + 8f + 3f
    pill(c.surfaceStrong, x, y + (h - 0.9f) / 2f, track, 0.9f, u)
    pill(c.progressFill, x, y + (h - 0.9f) / 2f, track * 0.58f, 0.9f, u)
}
