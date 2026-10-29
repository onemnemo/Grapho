package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.floatingSurface
import com.onemnemo.grapho.ui.components.motionCalm
import com.onemnemo.grapho.ui.components.motionEnterEasing
import com.onemnemo.grapho.ui.components.motionTween
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

/** The bars that ride on the page start this far down, a little smaller, and rise into place. */
private const val RISE_START_SCALE = 0.96f
private val RISE_DISTANCE = GraphoSpace.spaceSm

/**
 * The quiet pill that the selection, paste and crop actions sit in, placed near [anchor] in view pixels and kept
 * inside [insets], so a floating note bar does not cover it. A pill wider than the window scrolls sideways.
 */
@Composable
internal fun CardSurface(anchor: RectF, insets: EdgeInsets, content: @Composable RowScope.() -> Unit) {
    val density = LocalDensity.current
    val gap = with(density) { GraphoSpace.spaceMd.roundToPx() }
    val room = with(density) {
        IntInsets(
            insets.left.dp.roundToPx(),
            insets.top.dp.roundToPx(),
            insets.right.dp.roundToPx(),
            insets.bottom.dp.roundToPx(),
        )
    }
    Row(
        modifier = Modifier
            .placedNear(anchor, gap, room)
            .riseIn()
            .floatingSurface(GraphoShapes.pill)
            .pointerInput(Unit) { detectTapGestures { } }
            .horizontalScroll(rememberScrollState())
            .padding(GraphoSpace.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The single pill that offers to paste, above the tapped point ([x], [y] in view pixels). */
@Composable
internal fun PasteCard(x: Float, y: Float, insets: EdgeInsets, onPaste: () -> Unit) {
    CardSurface(RectF(x, y, x, y), insets) {
        GraphoButton(
            text = stringResource(R.string.lasso_paste),
            onClick = onPaste,
            kind = ButtonKind.Quiet,
            size = ButtonSize.Small,
            iconRes = R.drawable.ic_clipboard,
        )
    }
}

/** The pill shown while an image is being cropped. Reset and Cancel are quiet text and Done is the one solid button. */
@Composable
internal fun CropCard(
    bounds: RectF,
    insets: EdgeInsets,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    CardSurface(bounds, insets) {
        GraphoButton(
            stringResource(R.string.crop_reset),
            onClick = onReset,
            kind = ButtonKind.Quiet,
            size = ButtonSize.Small,
        )
        GraphoButton(
            stringResource(R.string.action_cancel),
            onClick = onCancel,
            kind = ButtonKind.Quiet,
            size = ButtonSize.Small,
        )
        GraphoButton(
            stringResource(R.string.crop_done),
            onClick = onDone,
            kind = ButtonKind.Primary,
            size = ButtonSize.Small,
        )
    }
}

private class IntInsets(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Puts the card above [anchor], or below it when there is no room, always inside the parent and the [room] kept clear. */
private fun Modifier.placedNear(anchor: RectF, gap: Int, room: IntInsets): Modifier =
    layout { measurable, constraints ->
        val card = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val xLow = minOf(room.left, maxOf(0, w - card.width))
        val xHigh = maxOf(xLow, w - card.width - room.right)
        val x = (anchor.centerX() - card.width / 2f).roundToInt().coerceIn(xLow, xHigh)
        val above = (anchor.top - gap - card.height).roundToInt()
        val low = minOf(room.top, maxOf(0, h - card.height))
        val high = maxOf(low, h - card.height - room.bottom)
        val y = (if (above >= room.top) above else (anchor.bottom + gap).roundToInt()).coerceIn(low, high)
        layout(w, h) { card.place(x, y) }
    }

@Composable
private fun Modifier.riseIn(): Modifier {
    val calm = motionCalm()
    val progress = remember { Animatable(if (calm) 1f else 0f) }
    val spec = motionTween<Float>(GraphoMotion.motionDurationBaseMs, motionEnterEasing())
    LaunchedEffect(Unit) { progress.animateTo(1f, spec) }
    return graphicsLayer {
        val left = 1f - progress.value
        val scale = 1f - (1f - RISE_START_SCALE) * left
        alpha = progress.value
        translationY = RISE_DISTANCE.toPx() * left
        scaleX = scale
        scaleY = scale
    }
}
