package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.onemnemo.grapho.ui.theme.Grapho

/** How strongly a brush stroke tints the picture under it. */
private const val TINT_ALPHA = 0.45f

/**
 * Catches pen and finger drags over the picture while a brush is active and tints what they covered: red where the
 * cut-out is being erased, the accent where the original is coming back. [crop] is the part of the source the picture
 * shows, as fractions. The strokes only reach the picture when the person finishes.
 */
@Composable
internal fun PictureBrushOverlay(brush: CutoutBrush, rect: State<RectF?>, crop: RectF) {
    val dp = LocalDensity.current.density
    val eraseTint = Grapho.colors.stateDangerSolid
    val restoreTint = Grapho.colors.actionPrimaryBg
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .pinnedTo(rect)
                .pointerInput(brush, crop) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val radius = brush.size * dp / 2f
                        brush.begin(
                            down.position.x,
                            down.position.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                            crop,
                            radius,
                        )
                        down.consume()
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            brush.extend(change.position.x, change.position.y)
                            change.consume()
                        }
                    }
                }
                .drawBehind {
                    // Reading the revision draws again whenever a stroke grows.
                    brush.revision
                    for (s in brush.strokes) {
                        val color = (if (s.erase) eraseTint else restoreTint).copy(alpha = TINT_ALPHA)
                        val width = 2f * s.radius / (crop.right - crop.left) * size.width
                        val path = Path()
                        var i = 0
                        var firstX = 0f
                        var firstY = 0f
                        while (i + 1 < s.points.size) {
                            val x = BrushMath.localOf(s.points[i], size.width, crop.left, crop.right)
                            val y = BrushMath.localOf(s.points[i + 1], size.height, crop.top, crop.bottom)
                            if (i == 0) {
                                path.moveTo(x, y)
                                firstX = x
                                firstY = y
                            } else {
                                path.lineTo(x, y)
                            }
                            i += 2
                        }
                        if (s.points.size == 2) {
                            drawCircle(color, width / 2f, Offset(firstX, firstY))
                        } else {
                            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                },
        )
    }
}
