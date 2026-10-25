package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPlannerTest {
    private val source = "11111111-1111-1111-1111-111111111111"

    private fun info(sizes: List<PageSize>, refs: List<PdfRef?>) = NotebookInfo(
        title = "t",
        infinite = false,
        pageSize = PageSize(595f, 842f),
        background = PageBackground(0, 8f, 0xFFFFFFFF.toInt()),
        coverColor = null,
        pageIds = sizes.indices.map { "p$it" },
        pageSizes = sizes,
        pdfRefs = refs,
        pdfs = emptyList(),
        canUndo = false,
        canRedo = false,
    )

    private fun viewport(info: NotebookInfo) = Viewport(viewWidth = 1000f, viewHeight = 800f).withStructure(info)

    private val a4 = PageSize(595f, 842f)

    @Test
    fun noPlanWithoutPdfPages() {
        val i = info(listOf(a4, a4), listOf(null, null))
        assertNull(PdfPlanner.plan(viewport(i), i, true))
    }

    @Test
    fun visiblePagesComeFirstAndNeighboursAreNear() {
        val refs = List(6) { PdfRef(source, it) }
        val i = info(List(6) { a4 }, refs)
        // A fitted A4 page is about 1300 pixels high in a 800 pixel view: one page is visible, sometimes two.
        val plan = PdfPlanner.plan(viewport(i), i, false)
        assertNotNull(plan)
        assertEquals(PdfRef(source, 0), plan!!.visible.first().ref)
        assertTrue(plan.near.all { n -> plan.visible.none { it.ref == n.ref } })
        assertTrue(plan.details.isEmpty())
    }

    @Test
    fun pagesWithoutPdfAreLeftOut() {
        val i = info(listOf(a4, a4, a4), listOf(null, PdfRef(source, 4), null))
        val plan = PdfPlanner.plan(viewport(i), i, true)!!
        val all = plan.visible + plan.near
        assertEquals(listOf(PdfRef(source, 4)), all.map { it.ref })
    }

    @Test
    fun wholePageBitmapsFitTheViewWidth() {
        val i = info(listOf(a4), listOf(PdfRef(source, 0)))
        val vp = viewport(i)
        val want = PdfPlanner.plan(vp, i, true)!!.visible.single()
        assertEquals(vp.baseScale, want.scale, 1e-6f)
    }

    @Test
    fun hugePagesAreCappedAtTheBitmapSide() {
        val big = PageSize(3000f, 4000f)
        val i = info(listOf(big), listOf(PdfRef(source, 0)))
        val wide = Viewport(viewWidth = 5000f, viewHeight = 3000f).withStructure(i)
        val want = PdfPlanner.plan(wide, i, true)!!.visible.single()
        assertEquals(PdfPlanner.MAX_BASE_SIDE, want.height * want.scale, 0.01f)
    }

    @Test
    fun detailsOnlyWhenSettledAndZoomedPastTheBase() {
        val i = info(listOf(a4, a4), listOf(PdfRef(source, 0), PdfRef(source, 1)))
        val fitted = viewport(i)
        assertTrue(PdfPlanner.plan(fitted, i, true)!!.details.isEmpty())

        val zoomed = fitted.zoomedAt(3f, 500f, 400f)
        assertTrue(PdfPlanner.plan(zoomed, i, false)!!.details.isEmpty())
        val details = PdfPlanner.plan(zoomed, i, true)!!.details
        assertTrue(details.isNotEmpty())
        for (d in details) {
            assertEquals(zoomed.scale, d.scale, 1e-6f)
            assertTrue("margin around the visible part", d.area.contains(d.visible))
            assertTrue(d.area.left >= 0f && d.area.top >= 0f)
            assertTrue(d.area.right <= d.width && d.area.bottom <= d.height)
        }
    }

    @Test
    fun mixedSizesGetTheirOwnBaseScale() {
        val small = PageSize(200f, 300f)
        val i = info(listOf(a4, small), listOf(PdfRef(source, 0), PdfRef(source, 1)))
        val plan = PdfPlanner.plan(viewport(i), i, false)!!
        val wanted = (plan.visible + plan.near).associateBy { it.ref.index }
        assertEquals(595f, wanted.getValue(0).width, 0f)
        assertEquals(200f, wanted.getValue(1).width, 0f)
        // The column is as wide as the widest page, so a narrower page is shown smaller at the same scale.
        assertEquals(wanted.getValue(0).scale, wanted.getValue(1).scale, 1e-6f)
    }
}
