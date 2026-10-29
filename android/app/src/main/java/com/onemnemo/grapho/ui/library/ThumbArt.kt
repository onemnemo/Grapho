package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.note.PaperChrome
import com.onemnemo.grapho.note.isDarkPaper

private const val SHEET_WIDTH = 120f

/** Pattern spacing on the 120 wide sheet for the default 8 mm, so a tile shows the paper's own rhythm. */
private const val SHEET_GAP = 12f
private const val DEFAULT_SPACING_MM = 8f

/**
 * A whiteboard drawn from its own paper: colour, pattern and spacing, with a few strokes on it so it reads as
 * written on, unless [sketch] is off. It fills its parent, which should be 3:4.
 */
@Composable
fun CanvasArt(background: PageBackground, modifier: Modifier = Modifier, sketch: Boolean = true) {
    Canvas(modifier.fillMaxSize().background(Color(background.color))) {
        val s = size.width / SHEET_WIDTH
        val mark = Color(PaperChrome.pattern(background.color))
        val gap = SHEET_GAP * s * (background.spacingMm / DEFAULT_SPACING_MM).coerceIn(0.5f, 2f)
        drawPaperPattern(background.pattern, mark, gap, 1f * s)
        if (sketch) scale(s, s, pivot = Offset.Zero) { drawSketch(isDarkPaper(background.color)) }
    }
}

/** The paper pattern: 1 lined, 2 grid, 3 dotted, anything else blank. */
internal fun DrawScope.drawPaperPattern(pattern: Int, color: Color, gap: Float, line: Float) {
    if (pattern == 0 || gap < 2f) return
    var y = gap
    while (y < size.height) {
        when (pattern) {
            1, 2 -> drawLine(color, Offset(0f, y), Offset(size.width, y), line)

            3 -> {
                var x = gap
                while (x < size.width) {
                    drawCircle(color, line * 1.1f, Offset(x, y))
                    x += gap
                }
            }
        }
        y += gap
    }
    if (pattern == 2) {
        var x = gap
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), line)
            x += gap
        }
    }
}

private fun DrawScope.drawSketch(onDark: Boolean) {
    val round = Stroke(2.4f, cap = StrokeCap.Round)
    val wave = Path().apply {
        moveTo(16f, 50f)
        quadraticBezierTo(26f, 36f, 38f, 50f)
    }
    drawPath(wave, Color(0xFFF28A2E), style = round)
    drawCircle(Color(0xFFD9487A), 13f, Offset(82f, 46f), style = round)
    val zig = Path().apply {
        moveTo(18f, 90f)
        lineTo(44f, 74f)
        lineTo(66f, 92f)
        lineTo(100f, 72f)
    }
    drawPath(zig, Color(0xFF5C8DF0), style = round)
    val ink = if (onDark) Color(0xFFEDECE8) else Color(0xFF17181C)
    drawLine(ink, Offset(20f, 118f), Offset(46f, 118f), 2.4f, StrokeCap.Round)
    drawLine(ink, Offset(20f, 130f), Offset(40f, 130f), 2.4f, StrokeCap.Round)
    drawRoundRect(Color(0xFFF6C453), Offset(76f, 108f), Size(24f, 24f), CornerRadius(5f), style = round)
}

/** The first page of an imported PDF: a title bar and lines of text on a pale page with a folded corner. */
@Composable
fun PdfArt(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().background(Color(0xFFF4F3EF))) {
        val w = size.width
        val h = size.height
        val left = w * 0.12f
        fun bar(top: Float, width: Float, height: Float, color: Color) =
            drawRect(color, Offset(left, h * top), Size(w * width, h * height))
        bar(0.12f, 0.38f, 0.05f, Color(0xFF23242A))
        val text = Color(0xFFC9C7BF)
        bar(0.24f, 0.76f, 0.034f, text)
        bar(0.30f, 0.62f, 0.034f, text)
        bar(0.36f, 0.76f, 0.034f, text)
        bar(0.46f, 0.52f, 0.05f, Color(0xCCF6C453))
        bar(0.56f, 0.76f, 0.034f, text)
        bar(0.64f, 0.60f, 0.034f, text)
        val fold = w * 0.18f
        drawRect(Color(0xFFD5D3CC), Offset(w - fold, 0f), Size(fold, fold))
        val corner = Path().apply {
            moveTo(w - fold, 0f)
            lineTo(w, 0f)
            lineTo(w, fold)
            close()
        }
        drawPath(corner, Color(0xFF17181C))
    }
}
