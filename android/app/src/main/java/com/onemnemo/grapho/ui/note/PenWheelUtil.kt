package com.onemnemo.grapho.ui.note

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Places a popup [dx] and [dy] px from the top left of the area it was declared in, kept inside it when [clamp]. */
internal class AnchoredAt(
    private val dx: Int,
    private val dy: Int,
    private val clamp: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + dx
        var y = anchorBounds.top + dy
        if (clamp) {
            x = x.coerceIn(anchorBounds.left, maxOf(anchorBounds.left, anchorBounds.right - popupContentSize.width))
            y = y.coerceIn(anchorBounds.top, maxOf(anchorBounds.top, anchorBounds.bottom - popupContentSize.height))
        }
        return IntOffset(x, y)
    }
}

internal fun anchoredAt(density: Float, xDp: Float, yDp: Float, clamp: Boolean = true) =
    AnchoredAt((xDp * density).roundToInt(), (yDp * density).roundToInt(), clamp)

/** Offset in dp from a wheel's centre; angles run clockwise from the right. */
internal fun polar(radius: Float, degrees: Float): Offset {
    val a = degrees * PI.toFloat() / 180f
    return Offset(radius * cos(a), radius * sin(a))
}

/** [degrees] brought into -180 to 180, for the difference of two angles. */
internal fun normDeg(degrees: Float): Float = ((degrees + 540f) % 360f + 360f) % 360f - 180f

/** [degrees] brought into half a [period] either side of zero. */
internal fun wrapDeg(degrees: Float, period: Float): Float {
    var r = degrees % period
    if (r >= period / 2f) r -= period
    if (r < -period / 2f) r += period
    return r
}

/** True when the pointer [id] is no longer down in the event just received. */
internal fun AwaitPointerEventScope.isReleased(id: PointerId): Boolean =
    currentEvent.changes.firstOrNull { it.id == id }?.pressed == false

internal fun Color.argbOpaque(): Int = toArgb() or 0xFF000000.toInt()
