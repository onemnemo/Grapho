package com.onemnemo.grapho.ui.note

import android.os.SystemClock
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** How often a pen resting over the bar's edge keeps a called back bar from leaving. */
private const val HOVER_KEEP_MS = 500L

/** Whether a pen hovering at ([x], [y]) in a box of [width] by [height] is within [band] of the bar's edge. */
internal fun penNearBar(position: BarPosition, x: Float, y: Float, width: Float, height: Float, band: Float): Boolean =
    when (position) {
        BarPosition.Top -> y <= band
        BarPosition.Bottom -> y >= height - band
        BarPosition.Left -> x <= band
        BarPosition.Right -> x >= width - band
    }

/**
 * Calls a hidden bar back when the pen hovers near its edge, and keeps it while the pen stays there. It hears the
 * pen from the screen's box ([watchPenHover]) and from the canvas, which takes hover events over the page itself.
 * The screen sets [active], [position], [band] and the two callbacks on each composition.
 */
internal class PenHover {
    var active = false
    var position = BarPosition.Top
    var band = 0f
    var onNear: () -> Unit = {}
    var onStay: () -> Unit = {}

    /** The screen's box in window pixels. */
    var bounds = Rect.Zero

    private var near = false
    private var kept = 0L

    /** The pen hovers at ([windowX], [windowY]) in window pixels. */
    fun at(windowX: Float, windowY: Float) {
        val now = active &&
            penNearBar(position, windowX - bounds.left, windowY - bounds.top, bounds.width, bounds.height, band)
        val time = SystemClock.uptimeMillis()
        if (now && !near) {
            onNear()
            kept = time
        } else if (now && time - kept >= HOVER_KEEP_MS) {
            onStay()
            kept = time
        }
        near = now
    }
}

/** Hears the pen hovering anywhere over the screen's box, without taking anything from what is under it. */
internal fun Modifier.watchPenHover(hover: PenHover): Modifier = this
    .onGloballyPositioned { hover.bounds = it.boundsInWindow() }
    .pointerInput(hover) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                val hovering = event.type == PointerEventType.Move || event.type == PointerEventType.Enter
                if (hovering && change.type == PointerType.Stylus && !change.pressed) {
                    hover.at(change.position.x + hover.bounds.left, change.position.y + hover.bounds.top)
                }
            }
        }
    }
