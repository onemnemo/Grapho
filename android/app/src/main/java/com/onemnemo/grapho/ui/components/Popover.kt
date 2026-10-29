package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val FLOATING_ELEVATION = 8.dp

/** Menus and popovers start a little smaller and grow into place. */
private const val POP_START_SCALE = 0.94f

/** The look of everything that floats over the page without a scrim: overlay fill, hairline edge, soft shadow. */
fun Modifier.floatingSurface(shape: Shape = GraphoShapes.surface): Modifier = composed {
    val c = Grapho.colors
    this
        .shadow(FLOATING_ELEVATION, shape)
        .background(c.surfaceOverlay, shape)
        .border(GraphoSpace.borderWidthHairline, c.borderSubtle, shape)
        .clip(shape)
}

/** A floating card that swallows taps on its padding, so they cannot reach the page and clear a selection. */
fun Modifier.floatingCard(shape: Shape = GraphoShapes.surface): Modifier = this
    .floatingSurface(shape)
    .pointerInput(Unit) { detectTapGestures { } }
    .padding(GraphoSpace.spaceXs)

/**
 * A small panel tied to its anchor, the parent it is declared in. It applies choices at once, so it has no
 * Done or Cancel: anything that needs those is a Sheet. A tap outside or Back calls [onDismiss]. A [narrow]
 * popover takes the narrow token width.
 */
@Composable
fun GraphoPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    narrow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val width = if (narrow) GraphoSpace.sizePopoverWidthNarrow else GraphoSpace.sizePopoverWidth
    AnchoredPanel(
        onDismiss = onDismiss,
        modifier = modifier
            .width(width)
            .heightIn(max = GraphoSpace.sizePopoverMaxHeight)
            .padding(GraphoSpace.spaceLg),
        content = content,
    )
}

/** The popup, its placement and its entrance, shared by [GraphoPopover] and the menu. */
@Composable
internal fun AnchoredPanel(onDismiss: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    val side = LocalPopupSide.current
    val positioner = remember(density, side) {
        AnchoredPositioner(
            gap = with(density) { GraphoSpace.spaceXs.roundToPx() },
            margin = with(density) { GraphoSpace.spaceSm.roundToPx() },
            side = side,
        )
    }
    val calm = motionCalm()
    val progress = remember { Animatable(if (calm) 1f else 0f) }
    val enter = motionTween<Float>(GraphoMotion.motionDurationPopMs, motionEnterEasing())
    LaunchedEffect(Unit) {
        progress.animateTo(1f, enter)
    }
    Popup(
        popupPositionProvider = positioner,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val scale = POP_START_SCALE + (1f - POP_START_SCALE) * progress.value
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.value
                    transformOrigin = side.growsFrom()
                }
                .floatingSurface()
                .then(modifier)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
    }
}

/** The corner a popup on this side grows out of: the one nearest its anchor. */
private fun PopupSide.growsFrom(): TransformOrigin = when (this) {
    PopupSide.BelowStart, PopupSide.RightOf -> TransformOrigin(0f, 0f)
    PopupSide.BelowEnd, PopupSide.LeftOf -> TransformOrigin(1f, 0f)
    PopupSide.AboveEnd -> TransformOrigin(1f, 1f)
}

private class AnchoredPositioner(private val gap: Int, private val margin: Int, private val side: PopupSide) :
    PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val spot = anchoredPopupSpot(
            anchorLeft = anchorBounds.left,
            anchorTop = anchorBounds.top,
            anchorBottom = anchorBounds.bottom,
            windowWidth = windowSize.width,
            windowHeight = windowSize.height,
            contentWidth = popupContentSize.width,
            contentHeight = popupContentSize.height,
            gap = gap,
            margin = margin,
            anchorRight = anchorBounds.right,
            side = side,
        )
        return IntOffset(spot.x, spot.y)
    }
}
