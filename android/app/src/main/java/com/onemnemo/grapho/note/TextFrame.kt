package com.onemnemo.grapho.note

import kotlin.math.cos
import kotlin.math.sin

/**
 * The maths of a text box's frame, in page-local units. A frame is `x, y, width, height` before it is turned,
 * with the rotation the clockwise turn in radians about its centre.
 */
internal object TextFrame {
    /** The narrowest a box is made by dragging its sides, in page-local units. */
    const val MIN_WIDTH = 24f

    /** Writes the turned top left corner of the frame to `out[0]` and `out[1]`. */
    fun corner(x: Float, y: Float, width: Float, height: Float, rotation: Float, out: FloatArray) {
        val dx = -width / 2f
        val dy = -height / 2f
        out[0] = x + width / 2f + dx * cos(rotation) - dy * sin(rotation)
        out[1] = y + height / 2f + dx * sin(rotation) + dy * cos(rotation)
    }

    /**
     * Writes to `out[0]` and `out[1]` the `x, y` of the frame once it is [newHeight] tall, placed so the turned top
     * left corner stays where it was. Text grows downwards from its corner, whatever the box is turned to.
     */
    fun regrown(x: Float, y: Float, width: Float, height: Float, rotation: Float, newHeight: Float, out: FloatArray) {
        corner(x, y, width, height, rotation, out)
        placed(out[0], out[1], width, newHeight, rotation, out)
    }

    /**
     * Writes to `out[0]` and `out[1]` the `x, y` of a frame [width] by [height] whose turned top left corner is at
     * ([cornerX], [cornerY]).
     */
    fun placed(cornerX: Float, cornerY: Float, width: Float, height: Float, rotation: Float, out: FloatArray) {
        val dx = -width / 2f
        val dy = -height / 2f
        val centreX = cornerX - (dx * cos(rotation) - dy * sin(rotation))
        val centreY = cornerY - (dx * sin(rotation) + dy * cos(rotation))
        out[0] = centreX - width / 2f
        out[1] = centreY - height / 2f
    }

    /** The width a box [width] wide gets when a side is dragged by [delta] along the box, never under [MIN_WIDTH]. */
    fun draggedWidth(width: Float, fromLeft: Boolean, delta: Float): Float =
        (if (fromLeft) width - delta else width + delta).coerceAtLeast(MIN_WIDTH)

    /**
     * Writes to `out[0]` and `out[1]` the `x, y` of the frame once it is [newWidth] by [newHeight]. The side opposite
     * the one dragged stays where it was on the page: the right side when [fromLeft], else the left.
     */
    fun resized(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        newWidth: Float,
        newHeight: Float,
        fromLeft: Boolean,
        out: FloatArray,
    ) {
        corner(x, y, width, height, rotation, out)
        val shift = if (fromLeft) width - newWidth else 0f
        placed(out[0] + shift * cos(rotation), out[1] + shift * sin(rotation), newWidth, newHeight, rotation, out)
    }

    /** [x] moved, if need be, so a box [width] wide ends inside a page [pageWidth] wide. */
    fun keptInside(x: Float, width: Float, pageWidth: Float): Float =
        if (width >= pageWidth) 0f else x.coerceIn(0f, pageWidth - width)
}
