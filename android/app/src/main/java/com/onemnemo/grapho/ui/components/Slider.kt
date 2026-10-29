package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Keys and the screen reader move the value by this fraction of the range. */
private const val KEY_STEPS = 20

/**
 * A horizontal slider: a thin track, a fill to the knob, and a knob that wears a halo on hover and drag.
 * [label] names it, and [valueText] is what a screen reader says for the current value.
 * [onValueChangeFinished] runs when a drag or key press ends, a good moment to save an undo step.
 */
@Composable
fun GraphoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    valueText: String? = null,
) {
    val c = Grapho.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    var dragging by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val current by rememberUpdatedState(value)
    val knob = Grapho.metrics.sizeThumb
    val fraction = sliderFraction(value, valueRange)
    val step = (valueRange.endInclusive - valueRange.start) / KEY_STEPS

    fun nudge(delta: Float) {
        change((current + delta).coerceIn(valueRange.start, valueRange.endInclusive))
        finished?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Grapho.metrics.sizeHitMin)
            .semantics {
                contentDescription = label
                if (valueText != null) stateDescription = valueText
                if (!enabled) disabled()
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                setProgress { target ->
                    change(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
            .hoverable(source, enabled)
            .focusable(enabled, source)
            .onKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight, Key.DirectionUp -> nudge(step)
                    Key.DirectionLeft, Key.DirectionDown -> nudge(-step)
                    Key.MoveHome -> nudge(valueRange.start - current)
                    Key.MoveEnd -> nudge(valueRange.endInclusive - current)
                    else -> return@onKeyEvent false
                }
                true
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val inset = knob.toPx() / 2f
                    val width = size.width - 2f * inset
                    val down = awaitFirstDown()
                    dragging = true
                    change(sliderValueAt(down.position.x - inset, width, valueRange))
                    down.consume()
                    drag(down.id) { move ->
                        change(sliderValueAt(move.position.x - inset, width, valueRange))
                        move.consume()
                    }
                    dragging = false
                    finished?.invoke()
                }
            }
            .drawBehind {
                val radius = knob.toPx() / 2f
                val trackHeight = GraphoSpace.sizeTrack.toPx()
                val left = radius
                val trackWidth = size.width - 2f * radius
                val centerY = size.height / 2f
                val corner = CornerRadius(trackHeight / 2f)
                val x = left + trackWidth * fraction
                drawRoundRect(
                    if (enabled) c.progressTrack else c.stateDisabledBg,
                    Offset(left, centerY - trackHeight / 2f),
                    Size(trackWidth, trackHeight),
                    corner,
                )
                drawRoundRect(
                    if (enabled) c.progressFill else c.stateDisabledFg,
                    Offset(left, centerY - trackHeight / 2f),
                    Size(trackWidth * fraction, trackHeight),
                    corner,
                )
                val center = Offset(x, centerY)
                if (enabled && (dragging || hovered)) {
                    val halo = if (dragging) 9.dp.toPx() else 7.dp.toPx()
                    drawCircle(c.pressFill, radius + halo, center)
                }
                if (focused) {
                    val ring = GraphoSpace.borderWidthFocus.toPx()
                    drawCircle(c.borderFocusGap, radius + ring, center)
                    drawCircle(c.borderFocus, radius + 1.5f * ring, center, style = Stroke(ring))
                }
                drawCircle(c.shadowThumb, radius + 1.dp.toPx(), center + Offset(0f, 1.dp.toPx()))
                drawCircle(if (enabled) c.textDefault else c.stateDisabledFg, radius, center)
            },
    )
}

/** Where along the track, 0 to 1, [value] sits. A zero-length range reads as the start. */
internal fun sliderFraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

/** The value under [x] pixels from the start of a track [trackWidth] pixels wide, held inside [range]. */
internal fun sliderValueAt(x: Float, trackWidth: Float, range: ClosedFloatingPointRange<Float>): Float {
    if (trackWidth <= 0f) return range.start
    val fraction = (x / trackWidth).coerceIn(0f, 1f)
    return range.start + fraction * (range.endInclusive - range.start)
}
