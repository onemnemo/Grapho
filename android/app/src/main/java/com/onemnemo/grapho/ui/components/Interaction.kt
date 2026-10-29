package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The pressed, hovered and focused state of one control, read from a single interaction source. */
@Stable
class ControlInteraction internal constructor(
    val source: MutableInteractionSource,
    private val pressedState: State<Boolean>,
    private val hoveredState: State<Boolean>,
    private val focusedState: State<Boolean>,
) {
    val pressed: Boolean get() = pressedState.value
    val hovered: Boolean get() = hoveredState.value
    val focused: Boolean get() = focusedState.value
}

@Composable
fun rememberControlInteraction(): ControlInteraction {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val hovered = source.collectIsHoveredAsState()
    val focused = source.collectIsFocusedAsState()
    return remember(source) { ControlInteraction(source, pressed, hovered, focused) }
}

/**
 * The one focus ring: a gap in the surface colour, then the focus colour. It sits outside the control, or
 * inside it ([inset]) for rows and options that live inside a track. Apply it outside any clip.
 */
fun Modifier.focusRing(visible: Boolean, shape: Shape, inset: Boolean = false): Modifier = composed {
    if (!visible) return@composed this
    val ring = Grapho.colors.borderFocus
    val gap = Grapho.colors.borderFocusGap
    val width = GraphoSpace.borderWidthFocus
    drawWithContent {
        drawContent()
        val w = width.toPx()
        if (inset) {
            val outline = shape.createOutline(Size(size.width - w, size.height - w), layoutDirection, this)
            translate(w / 2f, w / 2f) { drawOutline(outline, ring, style = Stroke(w)) }
        } else {
            val gapOutline = shape.createOutline(Size(size.width + w, size.height + w), layoutDirection, this)
            translate(-w / 2f, -w / 2f) { drawOutline(gapOutline, gap, style = Stroke(w)) }
            val ringOutline = shape.createOutline(
                Size(size.width + 3f * w, size.height + 3f * w),
                layoutDirection,
                this,
            )
            translate(-1.5f * w, -1.5f * w) { drawOutline(ringOutline, ring, style = Stroke(w)) }
        }
    }
}

/**
 * Reports whether a pointer is pressed on this element or hovering over it. It only watches, so nothing is
 * consumed and the element's own gestures work as before.
 */
fun Modifier.trackPointerHold(onChange: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        var hovering = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            when (event.type) {
                PointerEventType.Enter -> hovering = true
                PointerEventType.Exit -> hovering = false
            }
            onChange(hovering || event.changes.any { it.pressed })
        }
    }
}
