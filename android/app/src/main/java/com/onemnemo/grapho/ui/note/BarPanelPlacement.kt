package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import com.onemnemo.grapho.ui.components.PopupSide
import com.onemnemo.grapho.ui.components.anchoredPopupSpot
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

/**
 * Where a panel of the bar (layers, shape options) sits in the screen's box. At the foot of a compact window it
 * is a sheet along the bottom. Anywhere wider it opens from its button on the bar, [anchor] in window pixels, the
 * way the bar's menus do; with no button to open from (the bar does not show it) it keeps to the end edge.
 */
internal fun BoxScope.barPanelPlacement(placement: LayersPlacement, anchor: Rect?, geometry: BarGeometry): Modifier =
    when {
        placement == LayersPlacement.Bottom ->
            Modifier
                .align(Alignment.BottomCenter)
                .padding(layersInsets(placement, geometry.occupied, GraphoSpace.spaceSm.value).toPadding())

        anchor != null -> Modifier.openFrom(anchor, geometry.position.popupSide)

        else ->
            Modifier
                .align(BiasAbsoluteAlignment(1f, -1f))
                .padding(layersInsets(placement, geometry.occupied, GraphoSpace.spaceSm.value).toPadding())
    }

/**
 * Fills the box and places what follows off [anchor] on [side], kept inside the box. The box itself takes no
 * touches, so the page under it still draws.
 */
private fun Modifier.openFrom(anchor: Rect, side: PopupSide): Modifier = composed {
    val density = LocalDensity.current
    val gap = with(density) { GraphoSpace.spaceXs.roundToPx() }
    val margin = with(density) { GraphoSpace.spaceSm.roundToPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val spot = anchoredPopupSpot(
                anchorLeft = (anchor.left - origin.x).roundToInt(),
                anchorTop = (anchor.top - origin.y).roundToInt(),
                anchorBottom = (anchor.bottom - origin.y).roundToInt(),
                windowWidth = width,
                windowHeight = height,
                contentWidth = placeable.width,
                contentHeight = placeable.height,
                gap = gap,
                margin = margin,
                anchorRight = (anchor.right - origin.x).roundToInt(),
                side = side,
            )
            layout(width, height) { placeable.place(spot.x, spot.y) }
        }
}
