package com.onemnemo.grapho.note

import kotlin.math.max
import kotlin.math.roundToInt

/** Which pages get a low-resolution preview, in what order, and at what size. Pure, so it is unit tested. */
object PreviewPlan {
    /** Pages beyond the visible ones that keep a preview, on each side: more than a zoom out reveals. */
    const val REACH = 5

    /** The longest side of a page preview, in pixels. */
    const val PAGE_SIDE = 1024

    /** The longest side of an infinite canvas preview, which covers all its content at once. */
    const val CANVAS_SIDE = 1536

    /**
     * The pages to preview, most needed first: the visible ones from [centre] outwards, then up to [reach] more on
     * each side, nearest first. Out of range pages are left out.
     */
    fun order(visible: IntRange, centre: Int, pageCount: Int, reach: Int = REACH): List<Int> {
        if (pageCount <= 0) return emptyList()
        val first = if (visible.isEmpty()) centre else visible.first
        val last = if (visible.isEmpty()) centre else visible.last
        val out = ArrayList<Int>()
        val mid = centre.coerceIn(first, last)
        out += mid
        for (d in 1..max(mid - first, last - mid)) {
            if (mid - d >= first) out += mid - d
            if (mid + d <= last) out += mid + d
        }
        for (d in 1..reach) {
            if (last + d < pageCount) out += last + d
            if (first - d >= 0) out += first - d
        }
        return out.filter { it in 0 until pageCount }
    }

    /** Pixels per unit for a preview of [width] by [height] units whose longest side is [maxSide] pixels. */
    fun scale(width: Float, height: Float, maxSide: Int): Float {
        val side = max(width, height)
        return if (side <= 0f) 0f else maxSide / side
    }

    /** The preview's size in whole pixels, at least one each way, for [scale] from [PreviewPlan.scale]. */
    fun pixels(width: Float, height: Float, scale: Float): Pair<Int, Int> =
        max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
