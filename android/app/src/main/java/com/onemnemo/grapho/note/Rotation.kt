package com.onemnemo.grapho.note

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.roundToInt

/** The turn gesture of a selection: the angle a drag makes and where the handle that starts it sits. */
internal object Rotation {
    /** A turn this close to a multiple of 90 degrees snaps to it. */
    const val SNAP_DEGREES = 4f

    /** [degrees], or the nearest multiple of 90 if it is within [SNAP_DEGREES] of one. */
    fun snap(degrees: Float): Float {
        val nearest = (degrees / 90f).roundToInt() * 90f
        return if (abs(degrees - nearest) <= SNAP_DEGREES) nearest else degrees
    }

    /** The turn from angle [from] to angle [to], both in degrees, as a value from -180 up to 180. */
    fun between(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        return d
    }

    /**
     * Puts the centre of the rotate handle for a selection box in [out] as x, y: [distance] above the middle of the top
     * edge, or below the bottom edge when there is no room above in a view starting at 0.
     */
    fun handle(box: RectF, distance: Float, out: FloatArray) {
        out[0] = box.centerX()
        out[1] = if (box.top - distance >= 0f) box.top - distance else box.bottom + distance
    }
}
