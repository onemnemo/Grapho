package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.roundToInt

/** The name of the pen just picked, beside the wheel. The point ([x], [y]) is at [alignX], [alignY] (0 to 1) across the pill. */
@Composable
internal fun NamePill(text: String, x: Float, y: Float, alignX: Float, alignY: Float, progress: Float) {
    val density = LocalDensity.current.density
    Popup(
        popupPositionProvider = AlignedAt((x * density).roundToInt(), (y * density).roundToInt(), alignX, alignY),
        properties = OverlayProperties,
    ) {
        Box(
            Modifier
                .graphicsLayer { alpha = progress }
                .clip(RoundedCornerShape(16.dp))
                .background(GraphoWheel.body)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(text, style = Grapho.type.label, color = GraphoWheel.text)
        }
    }
}

/** Like [AnchoredAt], but the point is [alignX] and [alignY] of the way across the popup, and the popup is kept inside the area. */
private class AlignedAt(
    private val dx: Int,
    private val dy: Int,
    private val alignX: Float,
    private val alignY: Float,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left + dx - (popupContentSize.width * alignX).roundToInt()
        val y = anchorBounds.top + dy - (popupContentSize.height * alignY).roundToInt()
        return IntOffset(
            x.coerceIn(anchorBounds.left, maxOf(anchorBounds.left, anchorBounds.right - popupContentSize.width)),
            y.coerceIn(anchorBounds.top, maxOf(anchorBounds.top, anchorBounds.bottom - popupContentSize.height)),
        )
    }
}
