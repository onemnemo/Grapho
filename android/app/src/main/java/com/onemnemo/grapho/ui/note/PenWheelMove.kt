package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.withTimeoutOrNull

/** A light tick for a detent, a tap on the wheel or a change of drop zone. */
internal fun HapticFeedback.tick() = performHapticFeedback(HapticFeedbackType.TextHandleMove)

/**
 * [local], a point in a node's own px, in the host's px. Asked afresh for every event, because the
 * node is the one being moved and its own space moves with it. The host never moves.
 */
internal fun hostPoint(host: Array<LayoutCoordinates?>, node: Array<LayoutCoordinates?>, local: Offset): Offset {
    val h = host[0]
    val n = node[0]
    return if (h != null && n != null && h.isAttached && n.isAttached) h.localPositionOf(n, local) else local
}

/**
 * Carries the wheel after a hold on it. [onWheel] reads the touch position in the node's px and says
 * whether it is on the wheel; [enabled] is false while a finger already has it. The callbacks report
 * the finger in host px. Once started, every event of the finger is consumed until it lifts, so the
 * release is always heard.
 */
internal fun Modifier.carryGesture(
    host: Array<LayoutCoordinates?>,
    enabled: () -> Boolean,
    onWheel: (Offset) -> Boolean,
    onStart: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
): Modifier = composed {
    val node = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnWheel by rememberUpdatedState(onWheel)
    val latestStart by rememberUpdatedState(onStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestEnd by rememberUpdatedState(onEnd)
    Modifier
        .onGloballyPositioned { node[0] = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!latestEnabled() || !latestOnWheel(down.position)) return@awaitEachGesture
                val slop = viewConfiguration.touchSlop
                var at = down.position
                var settled = false
                val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (!settled) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed || change.isConsumed) {
                            settled = true
                        } else {
                            at = change.position
                            if ((at - down.position).getDistance() > slop) settled = true
                        }
                    }
                } == null
                if (!held) return@awaitEachGesture

                latestStart(hostPoint(host, node, at))
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        // Nothing under the finger reacts while the wheel is carried.
                        event.changes.forEach { it.consume() }
                        if (!change.pressed) break
                        latestMove(hostPoint(host, node, change.position))
                    }
                } finally {
                    latestEnd()
                }
            }
        }
}

/**
 * Drag for a node that moves itself: the finger is followed in the host's px, since the node's
 * own px shift as it moves. [onMove] gets the distance from where the drag began, in dp.
 */
internal fun Modifier.hostDrag(
    host: Array<LayoutCoordinates?>,
    onStart: () -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit = {},
): Modifier = composed {
    val node = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val latestStart by rememberUpdatedState(onStart)
    val latestMove by rememberUpdatedState(onMove)
    val latestEnd by rememberUpdatedState(onEnd)
    Modifier
        .onGloballyPositioned { node[0] = it }
        .pointerInput(Unit) {
            var from = Offset.Zero
            detectDragGestures(
                onDragStart = { o ->
                    from = hostPoint(host, node, o)
                    latestStart()
                },
                onDragEnd = { latestEnd() },
                onDragCancel = { latestEnd() },
                onDrag = { change, _ ->
                    change.consume()
                    latestMove((hostPoint(host, node, change.position) - from) / density)
                },
            )
        }
}
