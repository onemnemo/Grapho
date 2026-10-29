package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onemnemo.grapho.ui.theme.CoverLook
import com.onemnemo.grapho.ui.theme.CoverPreset
import com.onemnemo.grapho.ui.theme.FolderColors
import com.onemnemo.grapho.ui.theme.Grapho

/** The art is drawn on a 120 by 160 sheet and scaled to the width, so a thumbnail and a big preview match. */
private const val SHEET_WIDTH = 120f

/** Spine and strap, as a share of the cover width. */
private const val SPINE = 0.09f
private const val STRAP = 0.05f

/** Under this width a cover is a thumbnail and carries no label. */
private const val LABEL_MIN_DP = 64f

private val StrapColor = Color(0xD10E0F12)

/**
 * A notebook cover. It fills its parent, which should be 3:4, and the clip is the parent's. [title] is the
 * handwritten label at the top, left out for thumbnails.
 */
@Composable
fun CoverArt(look: CoverLook, title: String?, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().background(look.base)) {
        Canvas(Modifier.fillMaxSize()) {
            drawPreset(look.preset)
            val spine = size.width * SPINE
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.04f)),
                    startX = 0f,
                    endX = spine,
                ),
                size = Size(spine, size.height),
            )
            drawRect(
                StrapColor,
                topLeft = Offset(size.width * (1f - SPINE - STRAP), 0f),
                size = Size(size.width * STRAP, size.height),
            )
        }
        if (!title.isNullOrBlank() && maxWidth.value >= LABEL_MIN_DP) {
            val shape = RoundedCornerShape(3.dp)
            Text(
                title,
                style = Grapho.type.cover,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = (maxWidth.value * 0.12f).coerceIn(13f, 20f).sp,
                color = FolderColors.ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight * 0.09f)
                    .widthIn(max = maxWidth * 0.78f)
                    .graphicsLayer { rotationZ = -2f }
                    .shadow(2.dp, shape)
                    .background(Color.White, shape)
                    .padding(horizontal = maxWidth * 0.07f, vertical = maxWidth * 0.025f),
            )
        }
    }
}

private fun DrawScope.drawPreset(preset: CoverPreset) {
    val s = size.width / SHEET_WIDTH
    scale(s, s, pivot = Offset.Zero) {
        when (preset) {
            CoverPreset.Calculus -> {
                drawRect(Color(0xFF17181C), Offset(0f, 86f), Size(120f, 3f))
                drawCircle(Color(0xFFE0402E), 22f, Offset(38f, 108f))
                drawRect(Color(0xFF3B3470), Offset(56f, 88f), Size(42f, 40f))
            }

            CoverPreset.Biology -> {
                val ink = Color(0xFF2A1F5C)
                val cell = Color(0xFFD9CFFA)
                val core = Color(0xFF7C5EE0)
                val dots = listOf(
                    Dot(46f, 104f, 30f, cell),
                    Dot(54f, 98f, 10f, core),
                    Dot(88f, 128f, 14f, cell),
                    Dot(92f, 125f, 5f, core),
                )
                for ((x, y, r, fill) in dots) {
                    drawCircle(fill, r, Offset(x, y))
                    drawCircle(ink, r, Offset(x, y), style = Stroke(2.2f))
                }
            }

            CoverPreset.Physics -> {
                drawGrid(Color(0xFF3D3D82), 0.6f, 0.7f)
                val centre = Offset(56f, 100f)
                val orbit = Size(68f, 24f)
                val at = Offset(centre.x - 34f, centre.y - 12f)
                drawOval(Color(0xFFF28A2E), at, orbit, style = Stroke(2.4f))
                rotate(60f, centre) { drawOval(Color(0xFFD9487A), at, orbit, style = Stroke(2.4f)) }
                rotate(-60f, centre) { drawOval(Color(0xFFF6C453), at, orbit, style = Stroke(2.4f)) }
                drawCircle(Color.White, 4f, centre)
            }

            CoverPreset.Journal -> drawGrid(Color(0xFF3A3B78), 0.6f, 1f)

            CoverPreset.Chemistry -> {
                drawCircle(Color(0xFF1F6B4F).copy(alpha = 0.5f), 34f, Offset(56f, 104f))
                drawCircle(Color(0xFF3FB27A).copy(alpha = 0.55f), 20f, Offset(56f, 104f))
                drawCircle(Color(0xFF9ED8F2).copy(alpha = 0.8f), 8f, Offset(56f, 104f))
            }

            CoverPreset.Sketches -> clipRect(0f, 70f, 120f, 160f) {
                val square = Color.White.copy(alpha = 0.85f)
                for (ty in 60 until 160 step 20) {
                    for (tx in 0 until 120 step 20) {
                        drawRect(square, Offset(tx.toFloat(), ty.toFloat()), Size(10f, 10f))
                        drawRect(square, Offset(tx + 10f, ty + 10f), Size(10f, 10f))
                    }
                }
            }

            CoverPreset.Spanish -> clipRect(0f, 70f, 120f, 160f) {
                drawRect(Color(0xFFF2A55E), Offset(0f, 64f), Size(120f, 96f))
                val band = Color.White.copy(alpha = 0.35f)
                for (t in 0 until 120 step 16) drawRect(band, Offset(t.toFloat(), 64f), Size(8f, 96f))
                for (t in 64 until 160 step 16) drawRect(band, Offset(0f, t.toFloat()), Size(120f, 8f))
            }

            CoverPreset.History -> {
                drawCircle(Color(0xFFF6C453), 30f, Offset(70f, 110f))
                drawRect(Color(0xFF1B1936), Offset(20f, 96f), Size(40f, 40f))
            }

            CoverPreset.Moss -> {
                val hill = Path().apply {
                    moveTo(0f, 120f)
                    quadraticBezierTo(30f, 90f, 60f, 112f)
                    quadraticBezierTo(90f, 134f, 120f, 100f)
                    lineTo(120f, 160f)
                    lineTo(0f, 160f)
                    close()
                }
                drawPath(hill, Color(0xFF25604A))
            }

            CoverPreset.Dusk -> {
                drawCircle(Color(0xFFD9487A), 26f, Offset(60f, 100f))
                drawCircle(Color(0xFFF28A2E).copy(alpha = 0.6f), 26f, Offset(68f, 108f))
            }

            CoverPreset.Plain -> Unit
        }
    }
}

private data class Dot(val x: Float, val y: Float, val r: Float, val fill: Color)

private fun DrawScope.drawGrid(color: Color, width: Float, alpha: Float) {
    val line = color.copy(alpha = alpha)
    for (i in 0 until 14) {
        drawLine(line, Offset(i * 9f, 0f), Offset(i * 9f, 160f), width)
        drawLine(line, Offset(0f, i * 12f), Offset(120f, i * 12f), width)
    }
}
