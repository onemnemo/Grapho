package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.onemnemo.grapho.ui.theme.FolderColors

/** Width over height of every folder, as in the design. */
const val FOLDER_RATIO = 228f / 140f

/** The folder colours a folder can take. A folder keeps no colour of its own yet, so its id picks one. */
fun folderColour(id: String): Color {
    val palette = FolderColors.all
    return palette[(id.hashCode() and Int.MAX_VALUE) % palette.size].second
}

private const val BACK_SHADE = 0.18f
private const val TAB_WIDTH = 0.38f
private const val TAB_HEIGHT = 0.2f
private const val BODY_TOP = 0.08f
private const val FRONT_TOP = 0.3f
private const val CORNER = 0.09f
private const val PAPER_COUNT = 2
private const val PAPER_ALPHA = 0.95f
private const val PAPER_FADE = 0.15f

/**
 * A folder with two papers peeking out. It fills the width it is given and keeps its ratio. [content] draws over
 * the front, such as the name and the dots button.
 */
@Composable
fun FolderArt(colour: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.aspectRatio(FOLDER_RATIO)) {
        Canvas(Modifier.fillMaxSize()) { drawFolder(colour) }
        content()
    }
}

private fun DrawScope.drawFolder(colour: Color) {
    val w = size.width
    val h = size.height
    val round = CornerRadius(h * CORNER)
    val back = lerp(colour, Color.Black, BACK_SHADE)
    drawRoundRect(back, Offset.Zero, Size(w * TAB_WIDTH, h * TAB_HEIGHT), round)
    drawRoundRect(back, Offset(0f, h * BODY_TOP), Size(w, h * (1f - BODY_TOP)), round)
    for (i in 0 until PAPER_COUNT) {
        val left = w * (0.1f + 0.14f * i)
        val paper = Size(w * 0.5f, h * 0.6f)
        val top = h * (0.14f + 0.03f * i)
        rotate(if (i == 0) -3f else 2f, Offset(left + paper.width / 2f, top + paper.height / 2f)) {
            drawRoundRect(
                Color.White.copy(alpha = PAPER_ALPHA - PAPER_FADE * i),
                Offset(left, top),
                paper,
                CornerRadius(h * 0.04f),
            )
        }
    }
    drawRoundRect(colour, Offset(0f, h * FRONT_TOP), Size(w, h * (1f - FRONT_TOP)), round)
}
