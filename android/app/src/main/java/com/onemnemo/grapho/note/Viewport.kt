package com.onemnemo.grapho.note

import android.graphics.RectF

/**
 * Where the document sits on screen. Immutable, so the render thread can keep
 * using one while input builds the next.
 *
 * view px = (content pt * [scale]) + offset. Content coordinates for a paged
 * notebook are the stacked pages with margins and gaps, each page centred in a
 * column as wide as the widest one; for an infinite canvas they are the
 * document coordinates themselves.
 */
data class Viewport(
    val viewWidth: Float = 0f,
    val viewHeight: Float = 0f,
    val infinite: Boolean = false,
    /** Sizes and positions of the pages. An infinite canvas has one page of no size. */
    val pages: PageLayout = PageLayout.EMPTY,
    /** Pixels per point at true size, used by infinite canvases. */
    val trueSize: Float = 1f,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val pageCount: Int get() = pages.count

    /** Scale at zoom 1: the widest page fits the view, or true size on an infinite canvas. */
    val baseScale: Float
        get() = when {
            infinite -> trueSize
            viewWidth > 0f && pages.maxWidth > 0f -> viewWidth / (pages.maxWidth + 2 * MARGIN)
            else -> 1f
        }

    val scale: Float get() = baseScale * zoom

    private val minZoom: Float get() = if (infinite) MIN_ZOOM_INFINITE else MIN_ZOOM_PAGED

    private val contentWidth: Float get() = pages.maxWidth + 2 * MARGIN

    private val contentHeight: Float get() = pageTop(pageCount) + ADD_HEIGHT + MARGIN

    /** Top of a page in content points. The slot after the last page holds the add target. */
    private fun pageTop(page: Int): Float = if (infinite) 0f else pages.top(page.coerceIn(0, pageCount))

    private fun pageLeft(page: Int): Float =
        if (infinite || pageCount == 0) 0f else pages.left(page.coerceIn(0, pageCount - 1))

    fun resized(width: Float, height: Float): Viewport = copy(viewWidth = width, viewHeight = height).clamped()

    fun withStructure(info: NotebookInfo): Viewport = copy(
        infinite = info.infinite,
        pages = if (info.infinite) INFINITE_PAGE else PageLayout.of(info.pageSizes),
    ).clamped()

    /** Zoom 1, top of the notebook; for an infinite canvas, the origin in the middle. */
    fun fitted(): Viewport = if (infinite) {
        copy(zoom = 1f, offsetX = viewWidth / 2, offsetY = viewHeight / 2)
    } else {
        copy(zoom = 1f, offsetX = 0f, offsetY = 0f).clamped()
    }

    fun panned(dx: Float, dy: Float): Viewport = copy(offsetX = offsetX + dx, offsetY = offsetY + dy).clamped()

    /** Changes the zoom keeping the content under ([fx], [fy]) where it is. */
    fun zoomedAt(newZoom: Float, fx: Float, fy: Float): Viewport {
        val z = newZoom.coerceIn(minZoom, MAX_ZOOM)
        val ratio = baseScale * z / scale
        return copy(
            zoom = z,
            offsetX = fx - (fx - offsetX) * ratio,
            offsetY = fy - (fy - offsetY) * ratio,
        ).clamped()
    }

    /** Scrolls so [page] starts near the top of the view. */
    fun revealed(page: Int): Viewport {
        if (infinite) return this
        return copy(offsetY = -(pageTop(page) - PAGE_GAP) * scale).clamped()
    }

    /** Centres narrow content, and keeps wide or tall content from scrolling past its edges. */
    private fun clamped(): Viewport {
        if (infinite) return this
        val cw = contentWidth * scale
        val ch = contentHeight * scale
        val x = if (cw <= viewWidth) (viewWidth - cw) / 2 else offsetX.coerceIn(viewWidth - cw, 0f)
        val y = if (ch <= viewHeight) 0f else offsetY.coerceIn(viewHeight - ch, 0f)
        return copy(offsetX = x, offsetY = y)
    }

    /** On-screen rectangle of a page, in view pixels. */
    fun pageRect(page: Int, out: RectF): RectF {
        if (infinite) return out.apply { set(0f, 0f, viewWidth, viewHeight) }
        val left = offsetX + pageLeft(page) * scale
        val top = offsetY + pageTop(page) * scale
        return out.apply { set(left, top, left + pages.width(page) * scale, top + pages.height(page) * scale) }
    }

    fun isPageVisible(page: Int): Boolean = page in visiblePages()

    /** The pages that touch the view, top to bottom: empty when none does, and just page 0 on an infinite canvas. */
    fun visiblePages(): IntRange {
        if (infinite) return 0..0
        if (pageCount == 0) return IntRange.EMPTY
        val s = scale
        val first = pages.lastTopAtOrAbove(-offsetY / s).coerceAtLeast(0)
        val last = pages.lastTopAtOrAbove((viewHeight - offsetY) / s)
        // The first candidate may end above the view, in the gap before the next page.
        val endsAbove = offsetY + (pages.top(first) + pages.height(first)) * s < 0f
        val start = if (endsAbove) first + 1 else first
        return if (start > last) IntRange.EMPTY else start..last
    }

    /** The page under a view point, or -1 for the desk between pages. */
    fun pageAt(x: Float, y: Float): Int {
        if (infinite) return if (pageCount > 0) 0 else -1
        val s = scale
        val cx = (x - offsetX) / s
        val cy = (y - offsetY) / s
        val page = pages.lastTopAtOrAbove(cy)
        if (page < 0 || cy - pages.top(page) > pages.height(page)) return -1
        val left = pages.left(page)
        return if (cx < left || cx > left + pages.width(page)) -1 else page
    }

    /** The page nearest the middle of the view: the one whose half of the gap below it the middle falls in. */
    fun centrePage(): Int {
        if (infinite || pageCount <= 1) return 0
        val middle = (viewHeight / 2f - offsetY) / scale
        return pages.lastTopAtOrAbove(middle + PAGE_GAP / 2f).coerceIn(0, pageCount - 1)
    }

    fun hasAddTarget(): Boolean = !infinite

    /** On-screen rectangle of the "Add page" target, in view pixels. It is as wide as the widest page. */
    fun addTargetRect(out: RectF): RectF {
        val left = offsetX + MARGIN * scale
        val top = offsetY + pageTop(pageCount) * scale
        return out.apply { set(left, top, left + pages.maxWidth * scale, top + ADD_HEIGHT * scale) }
    }

    fun hitsAddTarget(x: Float, y: Float): Boolean = hasAddTarget() && addTargetRect(RectF()).contains(x, y)

    /** View pixel to page-local document units. */
    fun localX(page: Int, x: Float): Float = (x - offsetX) / scale - pageLeft(page)

    fun localY(page: Int, y: Float): Float = (y - offsetY) / scale - pageTop(page)

    /** Page-local document units to view pixels, the inverse of [localX] and [localY]. */
    fun viewX(page: Int, x: Float): Float = (x + pageLeft(page)) * scale + offsetX

    fun viewY(page: Int, y: Float): Float = (y + pageTop(page)) * scale + offsetY

    companion object {
        /** Space around the pages, in points. */
        const val MARGIN = 24f
        const val PAGE_GAP = 24f
        const val ADD_HEIGHT = 60f
        const val MAX_ZOOM = 5f

        /** How far out a notebook of pages zooms: a few pages fit on screen. */
        const val MIN_ZOOM_PAGED = 0.2f

        /** How far out an infinite canvas zooms: a whole board of notes fits on screen. */
        const val MIN_ZOOM_INFINITE = 0.05f

        private val INFINITE_PAGE = PageLayout.uniform(1, 0f, 0f)
    }
}
