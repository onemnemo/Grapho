package com.onemnemo.grapho.note

/**
 * Where each page of a paged notebook sits in content points: pages stack
 * vertically and are centred in a column as wide as the widest page. Built
 * once per structure change and never changed, so the render thread can share
 * it. The slot after the last page holds the "Add page" target.
 */
class PageLayout(private val widths: FloatArray, private val heights: FloatArray) {
    init {
        require(widths.size == heights.size) { "one width and one height per page" }
    }

    val count: Int get() = widths.size

    /** The widest page, which sets the width of the column. */
    val maxWidth: Float = widths.maxOrNull() ?: 0f

    // tops[i] is the top of page i, and tops[count] the top of the add target.
    private val tops = FloatArray(widths.size + 1).also {
        var y = Viewport.MARGIN
        for (i in widths.indices) {
            it[i] = y
            y += heights[i] + Viewport.PAGE_GAP
        }
        it[widths.size] = y
    }

    fun width(page: Int): Float = widths[page]

    fun height(page: Int): Float = heights[page]

    /** Top of a page, or of the add target for `page == count`. */
    fun top(page: Int): Float = tops[page]

    fun left(page: Int): Float = Viewport.MARGIN + (maxWidth - widths[page]) / 2f

    /** The last page whose top is at or above [y], or -1 when [y] is above the first page. */
    fun lastTopAtOrAbove(y: Float): Int {
        var lo = 0
        var hi = count - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (tops[mid] <= y) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return found
    }

    override fun equals(other: Any?): Boolean =
        other is PageLayout && widths.contentEquals(other.widths) && heights.contentEquals(other.heights)

    override fun hashCode(): Int = 31 * widths.contentHashCode() + heights.contentHashCode()

    companion object {
        val EMPTY = PageLayout(FloatArray(0), FloatArray(0))

        fun uniform(count: Int, width: Float, height: Float) =
            PageLayout(FloatArray(count) { width }, FloatArray(count) { height })

        fun of(sizes: List<PageSize>) =
            PageLayout(FloatArray(sizes.size) { sizes[it].width }, FloatArray(sizes.size) { sizes[it].height })
    }
}
