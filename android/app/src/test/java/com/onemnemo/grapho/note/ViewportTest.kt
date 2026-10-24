package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTest {
    // Page width plus both margins equals the view width, so the base scale is exactly 1.
    private val paged = Viewport(
        viewWidth = 480f,
        viewHeight = 800f,
        pages = PageLayout.uniform(3, 432f, 600f),
    )

    private val infinite = Viewport(
        viewWidth = 1000f,
        viewHeight = 800f,
        infinite = true,
        pages = PageLayout.uniform(1, 0f, 0f),
        offsetX = 500f,
        offsetY = 400f,
    )

    @Test
    fun viewCoordinatesInvertLocalCoordinates() {
        for (vp in listOf(paged.zoomedAt(2f, 100f, 300f), infinite)) {
            val page = if (vp.infinite) 0 else 1
            assertEquals(123f, vp.viewX(page, vp.localX(page, 123f)), 1e-3f)
            assertEquals(456f, vp.viewY(page, vp.localY(page, 456f)), 1e-3f)
        }
    }

    @Test
    fun pagedBaseScaleFitsPageWidthToView() {
        assertEquals(1f, paged.baseScale, 0f)
        assertEquals(0.5f, paged.copy(viewWidth = 240f).baseScale, 0f)
    }

    @Test
    fun centrePageFollowsTheMiddleOfTheView() {
        // Pages start at 24, 648 and 1272 with 600 high pages and a 24 gap; the view is 800 high.
        assertEquals(0, paged.centrePage())
        assertEquals(1, paged.panned(0f, -500f).centrePage())
        assertEquals("gap, nearer page 0", 0, paged.panned(0f, -230f).centrePage())
        assertEquals(2, paged.panned(0f, -5000f).centrePage())
        assertEquals(0, infinite.centrePage())
    }

    @Test
    fun pageAtFindsPagesAndRejectsTheDesk() {
        assertEquals(0, paged.pageAt(100f, 30f))
        assertEquals(1, paged.pageAt(100f, 700f))
        assertEquals("left margin", -1, paged.pageAt(10f, 30f))
        assertEquals("above the first page", -1, paged.pageAt(100f, 10f))
        assertEquals("gap between pages", -1, paged.pageAt(100f, 630f))
        assertEquals("past the last page", -1, paged.pageAt(100f, 1900f))
    }

    @Test
    fun pageAtOnInfiniteCanvasIsAlwaysThePageWhenThereIsOne() {
        assertEquals(0, infinite.pageAt(-5000f, 9000f))
        assertEquals(-1, infinite.copy(pages = PageLayout.EMPTY).pageAt(0f, 0f))
    }

    @Test
    fun pannedStaysWithinTheContent() {
        assertEquals(-100f, paged.panned(0f, -100f).offsetY, 0f)
        assertEquals("cannot scroll above the top", 0f, paged.panned(0f, 50f).offsetY, 0f)
        // 3 pages of 624 + 24 margin + add target 60 + 24 margin = 1980, minus the 800 view.
        assertEquals(-1180f, paged.panned(0f, -5000f).offsetY, 0f)
    }

    @Test
    fun narrowContentIsCentred() {
        // At zoom 0.5 the 480 wide content is 240 wide in a 480 wide view.
        assertEquals(120f, paged.zoomedAt(0.5f, 0f, 0f).offsetX, 0.001f)
    }

    @Test
    fun zoomIsClampedToTheAllowedRange() {
        assertEquals(Viewport.MAX_ZOOM, paged.zoomedAt(10f, 0f, 0f).zoom, 0f)
        assertEquals(Viewport.MIN_ZOOM_PAGED, paged.zoomedAt(0.01f, 0f, 0f).zoom, 0f)
        assertEquals(Viewport.MIN_ZOOM_INFINITE, infinite.zoomedAt(0.01f, 0f, 0f).zoom, 0f)
    }

    @Test
    fun zoomedAtKeepsTheFocalPointStill() {
        val zoomed = infinite.zoomedAt(2f, 300f, 200f)
        assertEquals(700f, zoomed.offsetX, 0f)
        assertEquals(600f, zoomed.offsetY, 0f)
        // The content under the focal point is the same before and after.
        assertEquals((300f - infinite.offsetX) / infinite.scale, (300f - zoomed.offsetX) / zoomed.scale, 0.001f)
    }

    @Test
    fun fittedCentresTheOriginOnInfiniteCanvasAndResetsPagedOnes() {
        val f = infinite.copy(zoom = 3f, offsetX = 1f, offsetY = 2f).fitted()
        assertEquals(1f, f.zoom, 0f)
        assertEquals(500f, f.offsetX, 0f)
        assertEquals(400f, f.offsetY, 0f)

        val p = paged.panned(0f, -300f).fitted()
        assertEquals(0f, p.offsetY, 0f)
    }

    @Test
    fun revealedScrollsAPageNearTheTop() {
        assertEquals(-624f, paged.revealed(1).offsetY, 0f)
        assertEquals("page 0 clamps to the top", 0f, paged.revealed(0).offsetY, 0f)
        assertEquals(infinite, infinite.revealed(2))
    }

    @Test
    fun localCoordinatesRemoveMarginAndPageOffset() {
        assertEquals(76f, paged.localX(0, 100f), 0f)
        assertEquals(76f, paged.localY(0, 100f), 0f)
        assertEquals(52f, paged.localY(1, 700f), 0f)
    }

    @Test
    fun onlyPagedNotebooksHaveAnAddTarget() {
        assertEquals(true, paged.hasAddTarget())
        assertEquals(false, infinite.hasAddTarget())
    }

    // A narrow page between two wide ones: 600, 300 and 600 points wide, 800, 400 and 800 high.
    // The column is 600 + 48 = 648 wide and the view is 648 wide, so the base scale is exactly 1.
    private val mixed = Viewport(
        viewWidth = 648f,
        viewHeight = 900f,
        pages = PageLayout(floatArrayOf(600f, 300f, 600f), floatArrayOf(800f, 400f, 800f)),
    )

    @Test
    fun mixedPagesFitTheWidestPageToTheView() {
        assertEquals(1f, mixed.baseScale, 0f)
        assertEquals(0.5f, mixed.copy(viewWidth = 324f).baseScale, 0f)
    }

    @Test
    fun mixedPagesAreCentredInTheColumnAndStackedByTheirOwnHeights() {
        // Tops: 24, 24 + 800 + 24 = 848, 848 + 400 + 24 = 1272. The add target follows at 1272 + 800 + 24 = 2096.
        val tops = listOf(24f, 848f, 1272f, 2096f)
        for (i in tops.indices) assertEquals(tops[i], mixed.pages.top(i), 0f)
        assertEquals(24f, mixed.pages.left(0), 0f)
        assertEquals(174f, mixed.pages.left(1), 0f)
        assertEquals(24f, mixed.pages.left(2), 0f)
        assertEquals("view position of page 1", 174f, mixed.viewX(1, 0f), 0f)
        assertEquals("view position of page 1", 848f, mixed.viewY(1, 0f), 0f)
    }

    @Test
    fun mixedPageAtUsesEachPagesOwnExtent() {
        assertEquals(0, mixed.pageAt(30f, 30f))
        assertEquals("beside the narrow page", -1, mixed.pageAt(100f, 900f))
        assertEquals(1, mixed.pageAt(200f, 900f))
        assertEquals("below the narrow page", -1, mixed.pageAt(200f, 1255f))
        assertEquals(2, mixed.pageAt(100f, 1300f))
        assertEquals("gap", -1, mixed.pageAt(200f, 835f))
    }

    @Test
    fun mixedLocalCoordinatesAccountForCentring() {
        // Page 1 starts 150 points right of the margin and 848 down.
        assertEquals(26f, mixed.localX(1, 200f), 0f)
        assertEquals(52f, mixed.localY(1, 900f), 0f)
        assertEquals(200f, mixed.viewX(1, mixed.localX(1, 200f)), 1e-3f)
    }

    @Test
    fun mixedCentrePageFollowsTheMiddleOfTheView() {
        assertEquals(0, mixed.centrePage())
        // The middle at 848 + 100 is inside the short page.
        assertEquals(1, mixed.panned(0f, -(948f - 450f)).centrePage())
        assertEquals(2, mixed.panned(0f, -5000f).centrePage())
    }

    @Test
    fun mixedContentHeightBoundsScrolling() {
        // 2096 + 60 + 24 = 2180 high content in a 900 high view.
        assertEquals(-1280f, mixed.panned(0f, -9000f).offsetY, 0f)
    }

    @Test
    fun mixedRevealedUsesThePageTop() {
        assertEquals(-824f, mixed.revealed(1).offsetY, 0f)
    }

    @Test
    fun visiblePagesAreThoseTouchingTheView() {
        assertEquals(0..1, mixed.visiblePages())
        assertEquals(1..2, mixed.panned(0f, -840f).visiblePages())
        assertEquals("only the last page", 2..2, mixed.panned(0f, -1300f).visiblePages())
        assertEquals(0..0, infinite.visiblePages())
        assertEquals(true, mixed.isPageVisible(1))
        assertEquals(false, mixed.isPageVisible(2))
    }

    @Test
    fun layoutsCompareByContent() {
        assertEquals(PageLayout.uniform(2, 10f, 20f), PageLayout.uniform(2, 10f, 20f))
        assertEquals(false, PageLayout.uniform(2, 10f, 20f) == PageLayout.uniform(2, 10f, 21f))
    }
}
