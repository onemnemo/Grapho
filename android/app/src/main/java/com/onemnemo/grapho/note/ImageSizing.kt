package com.onemnemo.grapho.note

import kotlin.math.max
import kotlin.math.min

/** Where a new image goes on a page, in page-local units. */
class ImagePlacement(val page: Int, val x: Float, val y: Float, val width: Float, val height: Float)

/** Pure arithmetic for pictures, kept apart from the drawing so it can be tested without a device. */
object ImageSizing {
    /** The longest side of a picture kept in the note, in pixels. */
    const val MAX_STORED_SIDE = 4096

    /** The longest side of a picture decoded for the screen, in pixels. */
    const val MAX_SCREEN_SIDE = 2048

    /** How much of the visible page a new image fills at most, across and down. */
    const val PLACE_FRACTION = 0.6f

    /** The power of two to decode a picture with so its longest side is at most [maxSide], or 1 if it already is. */
    fun sampleSize(longSide: Int, maxSide: Int): Int {
        var sample = 1
        while (longSide / sample > maxSide) sample *= 2
        return sample
    }

    /**
     * The rectangle for a picture of [pixelsWide] by [pixelsHigh] centred in the visible area (its left, top, width and
     * height), covering at most [PLACE_FRACTION] of it each way with the picture's proportions kept.
     */
    fun place(
        page: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        pixelsWide: Int,
        pixelsHigh: Int,
    ): ImagePlacement {
        val wide = max(pixelsWide, 1).toFloat()
        val high = max(pixelsHigh, 1).toFloat()
        val scale = min(width * PLACE_FRACTION / wide, height * PLACE_FRACTION / high)
        val w = wide * scale
        val h = high * scale
        return ImagePlacement(page, left + (width - w) / 2f, top + (height - h) / 2f, w, h)
    }

    /**
     * The rectangle for a picture of [pixelsWide] by [pixelsHigh] that stands in for a frame ([left], [top], [width],
     * [height]): as large as fits inside it with the picture's proportions kept, centred.
     */
    fun fit(
        page: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        pixelsWide: Int,
        pixelsHigh: Int,
    ): ImagePlacement {
        val wide = max(pixelsWide, 1).toFloat()
        val high = max(pixelsHigh, 1).toFloat()
        val scale = min(width / wide, height / high)
        val w = wide * scale
        val h = high * scale
        return ImagePlacement(page, left + (width - w) / 2f, top + (height - h) / 2f, w, h)
    }
}
