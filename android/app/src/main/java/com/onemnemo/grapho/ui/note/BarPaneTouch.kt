package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

/**
 * Closes the bar's open pane when a pen or finger comes down anywhere on the screen but the bar and the open card.
 * It only watches: the touch still goes on to whatever is under it, so the page draws and a wheel button works on
 * that same touch. A touch on the bar is left to its buttons, which open, close or switch panes themselves. Menus
 * are windows of their own, so a touch inside one never gets here.
 */
internal fun Modifier.closePanesOnTouch(ui: NoteUiState): Modifier = composed {
    val origin = remember { arrayOf(Offset.Zero) }
    onGloballyPositioned { origin[0] = it.positionInWindow() }
        .pointerInput(ui) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type != PointerEventType.Press) continue
                    val down = event.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
                    if (paneClosesOn(ui.pane, down.position + origin[0], ui.barBounds, ui.cardBounds)) {
                        ui.closePanes()
                    }
                }
            }
        }
}

/** The panes that are an options card over the page, as opposed to a menu. */
private val CARD_PANES = setOf(BarPane.Shapes, BarPane.Text, BarPane.Pen, BarPane.Eraser, BarPane.Lasso)

/**
 * Whether a touch at [at], in window pixels, closes [pane]. Nothing closes when no pane is open, or when it is
 * Layers, which stays while you draw. A touch on the [bar] belongs to its buttons, and one inside the options
 * [card] of a tool is a touch on the pane itself.
 */
internal fun paneClosesOn(pane: BarPane?, at: Offset, bar: Rect?, card: Rect?): Boolean {
    if (pane == null || pane == BarPane.Layers) return false
    if (bar != null && bar.contains(at)) return false
    val onCard = pane in CARD_PANES && card != null && card.contains(at)
    return !onCard
}
