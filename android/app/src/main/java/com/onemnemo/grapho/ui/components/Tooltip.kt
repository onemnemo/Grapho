package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

/** Which side of its control a tooltip prefers. It flips to the opposite side when there is no room. */
enum class TooltipSide { Above, Below, Start, End }

/** How long a tooltip stays up once a long press or a hover has shown it. */
private const val VISIBLE_MS = 1500L

private const val ENTER_SHIFT = 0.25f
private const val ENTER_START_SCALE = 0.96f

/** Whether one control's tooltip is showing. A long press or a hover shows it, and it goes by itself. */
@Stable
class TooltipState {
    var shown by mutableStateOf(false)
        private set

    fun show() {
        shown = true
    }

    fun hide() {
        shown = false
    }
}

@Composable
fun rememberTooltipState(): TooltipState = remember { TooltipState() }

/**
 * A small label for an icon-only control. On touch it comes from a long press ([TooltipState.show]), with a
 * pointer from a hover. Declare it inside the control: it is placed against the box it is declared in, on the
 * [side] away from the edge the control sits on. It takes no touches and goes after a moment.
 */
@Composable
fun GraphoTooltip(label: String, state: TooltipState, side: TooltipSide = TooltipSide.Below) {
    if (!state.shown) return
    LaunchedEffect(Unit) {
        delay(VISIBLE_MS)
        state.hide()
    }
    val density = LocalDensity.current
    val positioner = remember(side, density) {
        val gap = with(density) { GraphoSpace.spaceSm.roundToPx() }
        TooltipPositioner(side, gap, gap)
    }
    val calm = motionCalm()
    val progress = remember { Animatable(if (calm) 1f else 0f) }
    val enter = motionTween<Float>(GraphoMotion.motionDurationFastMs, motionEnterEasing())
    LaunchedEffect(Unit) { progress.animateTo(1f, enter) }
    val c = Grapho.colors
    Popup(
        popupPositionProvider = positioner,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        Text(
            label,
            style = Grapho.type.micro,
            color = c.actionPrimaryFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .graphicsLayer {
                    val scale = ENTER_START_SCALE + (1f - ENTER_START_SCALE) * progress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.value
                    translationY = (1f - progress.value) * ENTER_SHIFT * size.height
                }
                .shadow(GraphoSpace.spaceXs, GraphoShapes.item)
                .background(c.actionPrimaryBg, GraphoShapes.item)
                .widthIn(max = GraphoSpace.sizeTooltipMaxWidth)
                .padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceXs + GraphoSpace.spaceNudge),
        )
    }
}

/** A tooltip's top-left corner in window pixels. */
data class TooltipSpot(val x: Int, val y: Int)

/**
 * Places a tooltip of ([width], [height]) against an anchor on [side], [gap] pixels off it and centred along it.
 * It flips to the opposite side when the preferred one runs out of room, and stays [margin] pixels inside the
 * window.
 */
fun tooltipSpot(
    side: TooltipSide,
    anchor: IntRect,
    window: IntSize,
    width: Int,
    height: Int,
    gap: Int,
    margin: Int,
): TooltipSpot {
    val centreX = anchor.left + (anchor.width - width) / 2
    val centreY = anchor.top + (anchor.height - height) / 2
    val x: Int
    val y: Int
    if (side == TooltipSide.Below || side == TooltipSide.Above) {
        val below = anchor.bottom + gap
        val above = anchor.top - gap - height
        val fitsBelow = below + height <= window.height - margin
        val fitsAbove = above >= margin
        val useBelow = if (side == TooltipSide.Below) fitsBelow || !fitsAbove else !fitsAbove && fitsBelow
        x = centreX
        y = if (useBelow) below else above
    } else {
        val after = anchor.right + gap
        val before = anchor.left - gap - width
        val fitsAfter = after + width <= window.width - margin
        val fitsBefore = before >= margin
        val useAfter = if (side == TooltipSide.End) fitsAfter || !fitsBefore else !fitsBefore && fitsAfter
        x = if (useAfter) after else before
        y = centreY
    }
    return TooltipSpot(
        x.coerceIn(margin, maxOf(margin, window.width - width - margin)),
        y.coerceIn(margin, maxOf(margin, window.height - height - margin)),
    )
}

private class TooltipPositioner(private val side: TooltipSide, private val gap: Int, private val margin: Int) :
    PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val spot = tooltipSpot(
            side,
            anchorBounds,
            windowSize,
            popupContentSize.width,
            popupContentSize.height,
            gap,
            margin,
        )
        return IntOffset(spot.x, spot.y)
    }
}
