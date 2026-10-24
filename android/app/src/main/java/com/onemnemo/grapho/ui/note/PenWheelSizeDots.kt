package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs

private const val DOT_PAD_DP = 16

/**
 * The wheel's size dots in a row: the same dots, radii and ring as the wheel's size mode, for the size
 * [number] from 1 to 100. A tap picks the nearest dot and a drag sets the size continuously, both
 * reported through [onNumber].
 */
@Composable
internal fun SizeDots(number: Int, onNumber: (Float) -> Unit) {
    val latest by rememberUpdatedState(onNumber)
    val description = stringResource(R.string.wheel_width)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                val pad = DOT_PAD_DP.dp.toPx()
                fun numberAt(x: Float): Float = 1f + ((x - pad) / (size.width - 2f * pad)).coerceIn(0f, 1f) * 99f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val at = numberAt(down.position.x)
                    latest(DOT_NUMBERS.minByOrNull { abs(it - at) } ?: at)
                    drag(down.id) { change ->
                        change.consume()
                        latest(numberAt(change.position.x))
                    }
                }
            },
    ) {
        val pad = DOT_PAD_DP.dp.toPx()
        val u = density
        fun centre(n: Float) = Offset(pad + (n - 1f) / 99f * (size.width - 2f * pad), size.height / 2f)
        for (n in DOT_NUMBERS) {
            drawCircle(GraphoWheel.dot, dotRadius(n) * u, centre(n))
        }
        val selected = DOT_NUMBERS.minByOrNull { abs(it - number) } ?: 1f
        val p = centre(selected)
        drawCircle(GraphoWheel.selectedIndicator, dotRadius(selected) * u, p)
        drawCircle(GraphoWheel.selectedIndicator, (dotRadius(selected) + 5f) * u, p, style = Stroke(width = 2f * u))
    }
}
