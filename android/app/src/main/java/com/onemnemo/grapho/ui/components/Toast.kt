package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.abs
import kotlin.math.roundToInt

private val TOAST_MAX_WIDTH = 480.dp

/** How far a swipe must travel, as a share of the toast's width, to send it away. */
private const val SWIPE_DISMISS_SHARE = 0.4f

private const val MS_PER_RING_STEP = 1_000

/**
 * One line that says what just happened, with at most one action. With an [actionLabel], a ring drains over
 * [durationMs] to show how long the action stays good, and waits while the toast is held. [onHold] reports
 * whether a pointer or focus is on the toast, so its owner can stop the clock. A swipe sideways, or the dismiss
 * action for a screen reader, calls [onDismiss].
 */
@Composable
fun GraphoToast(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    durationMs: Int = 0,
    onHold: (Boolean) -> Unit = {},
) {
    val c = Grapho.colors
    val dismissLabel = stringResource(R.string.action_dismiss)
    var dragX by remember { mutableFloatStateOf(0f) }
    var focusHeld by remember { mutableStateOf(false) }
    var pointerHeld by remember { mutableStateOf(false) }
    val held = focusHeld || pointerHeld
    LaunchedEffect(held) { onHold(held) }
    Row(
        modifier = modifier
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .widthIn(max = TOAST_MAX_WIDTH)
            .floatingSurface()
            .onFocusChanged { focusHeld = it.hasFocus }
            .trackPointerHold { pointerHeld = it }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragX) > size.width * SWIPE_DISMISS_SHARE) onDismiss() else dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                ) { change, amount ->
                    change.consume()
                    dragX += amount
                }
            }
            .semantics {
                liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
                dismiss(dismissLabel) {
                    onDismiss()
                    true
                }
            }
            .padding(start = GraphoSpace.spaceLg, end = GraphoSpace.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = Grapho.type.label,
            color = if (isError) c.stateDangerFg else c.textDefault,
            modifier = Modifier.weight(1f, fill = false).padding(vertical = GraphoSpace.spaceMd),
        )
        if (actionLabel != null && onAction != null) {
            GraphoButton(actionLabel, onAction, kind = ButtonKind.Quiet, size = ButtonSize.Small)
            if (durationMs > 0) DrainRing(durationMs, held, Modifier.padding(end = GraphoSpace.spaceMd))
        }
    }
}

@Composable
private fun DrainRing(durationMs: Int, paused: Boolean, modifier: Modifier) {
    val c = Grapho.colors
    val left = remember { Animatable(1f) }
    val calm = motionCalm()
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        val ms = (left.value * durationMs).toInt()
        // Calm motion still counts down, in whole steps, so nothing slides.
        val easing: Easing = if (calm) Easing { steppedFraction(it, ms / MS_PER_RING_STEP) } else LinearEasing
        left.animateTo(0f, tween(ms, easing = easing))
    }
    Canvas(modifier.size(Grapho.metrics.sizeIconSmall)) {
        val stroke = GraphoSpace.borderWidthEmphasis.toPx()
        val inset = stroke / 2f
        val box = Size(size.width - stroke, size.height - stroke)
        drawArc(c.borderSubtle, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
        drawArc(
            c.textDefault,
            -90f,
            360f * left.value,
            false,
            Offset(inset, inset),
            box,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}
