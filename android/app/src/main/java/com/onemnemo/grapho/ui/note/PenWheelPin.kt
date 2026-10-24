package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs

private const val GRIP_W = 30f
private const val PIN_W = 32f
private const val HANDLE_H = 30f
private const val HANDLE_W = GRIP_W + PIN_W + 1f
private const val HANDLE_GAP = 2f

/** Travel in dp before a press on the grip becomes a carry. */
private const val GRIP_SLOP = 4f

/**
 * Where the handle's top left goes, in dp: tight to the rim of a wheel of [radius] centred on
 * [centre], toward the page and up. The free circle ([edge] null) puts it at the top right.
 */
internal fun handleTopLeft(centre: Offset, radius: Float, edge: DockEdge?): Offset {
    val u = polar(1f, edge?.toScreen(-45f) ?: -45f)
    // How far the pill reaches along the direction away from the wheel, so its nearest side meets the gap.
    val reach = HANDLE_W / 2f * abs(u.x) + HANDLE_H / 2f * abs(u.y)
    val p = centre + u * (radius + HANDLE_GAP + reach)
    return Offset(p.x - HANDLE_W / 2f, p.y - HANDLE_H / 2f)
}

/**
 * The pill on the wheel's rim: a grip that carries the wheel, and the pin that keeps it open when
 * the page is touched. It is a child of the host at its origin and rides the wheel by layer
 * translation alone: its top left is the wheel's centre plus the offset for the shape the wheel
 * is in, blended between the disc's and the circle's while one turns into the other. The grip
 * reports the finger in host px ([onCarryStart], [onCarryMove], [onCarryEnd]).
 */
@Composable
internal fun WheelHandle(
    motion: WheelMotion,
    host: Array<LayoutCoordinates?>,
    pinned: Boolean,
    onToggle: () -> Unit,
    onGripTap: () -> Unit,
    onCarryStart: (Offset) -> Unit,
    onCarryMove: (Offset) -> Unit,
    onCarryEnd: () -> Unit,
) {
    val ink = GraphoWheel.textMuted
    val moveLabel = stringResource(R.string.wheel_move)
    val pinLabel = stringResource(if (pinned) R.string.wheel_unpin else R.string.wheel_pin)
    val density = LocalDensity.current.density
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val latestStart by rememberUpdatedState(onCarryStart)
    val latestMove by rememberUpdatedState(onCarryMove)
    val latestEnd by rememberUpdatedState(onCarryEnd)
    val latestTap by rememberUpdatedState(onGripTap)
    Row(
        Modifier
            .graphicsLayer {
                val m = motion.shape
                val c = motion.centre
                val onDisc = handleTopLeft(Offset.Zero, DISC_R, motion.edge)
                val grown = mix(DOCKED_SCALE, 1f, m) * (1f + CARRY_SCALE * motion.lift)
                val onCircle = handleTopLeft(Offset.Zero, POPUP_R * grown, null)
                translationX = c.x + mix(onDisc.x, onCircle.x, m) * density
                translationY = c.y + mix(onDisc.y, onCircle.y, m) * density
            }
            .height(HANDLE_H.dp)
            .clip(RoundedCornerShape((HANDLE_H / 2f).dp))
            .background(GraphoWheel.body),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = GRIP_W.dp, height = HANDLE_H.dp)
                .semantics { contentDescription = moveLabel }
                .onGloballyPositioned { coords[0] = it }
                .pointerInput(Unit) {
                    val slop = GRIP_SLOP * density
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // A press that never travels is not a carry but a tap.
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                            if (change.isConsumed) return@awaitEachGesture
                            if (!change.pressed) {
                                latestTap()
                                return@awaitEachGesture
                            }
                            if ((change.position - down.position).getDistance() > slop) {
                                change.consume()
                                latestStart(hostPoint(host, coords, change.position))
                                break
                            }
                        }
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                if (!change.pressed) break
                                latestMove(hostPoint(host, coords, change.position))
                            }
                        } finally {
                            latestEnd()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(width = 10.dp, height = 14.dp)) {
                for (col in 0..1) {
                    for (row in 0..2) {
                        drawCircle(ink, 1.6f.dp.toPx(), Offset(col * 6.dp.toPx() + 2.dp.toPx(), row * 5.dp.toPx() + 2.dp.toPx()))
                    }
                }
            }
        }
        Box(Modifier.width(1.dp).height(16.dp).background(GraphoWheel.plate))
        Box(
            Modifier
                .size(width = PIN_W.dp, height = HANDLE_H.dp)
                .pressable(onClick = onToggle)
                .semantics { contentDescription = pinLabel },
            contentAlignment = Alignment.Center,
        ) {
            // One push pin: leaning and outlined when loose, upright and filled when pinned.
            GraphoIcon(
                if (pinned) R.drawable.ic_pin_filled else R.drawable.ic_pin,
                null,
                if (pinned) GraphoWheel.text else ink,
                modifier = Modifier.graphicsLayer { rotationZ = if (pinned) 0f else 45f },
                size = 18.dp,
            )
        }
    }
}
