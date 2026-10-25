package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPlanTest {
    @Test
    fun aSinglePageIsZeroBased() {
        assertEquals(listOf(4), ExportPlan.parseRange("5", 10))
    }

    @Test
    fun rangesAndSinglePagesMix() {
        assertEquals(listOf(0, 1, 2, 4), ExportPlan.parseRange("1-3, 5", 5))
    }

    @Test
    fun pagesComeBackSortedAndWithoutRepeats() {
        assertEquals(listOf(0, 1, 2, 3, 6), ExportPlan.parseRange("7, 3-4, 1-3", 7))
    }

    @Test
    fun spacesAndSemicolonsAreFine() {
        assertEquals(listOf(0, 1, 2, 4), ExportPlan.parseRange(" 1 - 3 ;5 ", 5))
    }

    @Test
    fun anEmptyOrBlankTextIsNotValid() {
        assertNull(ExportPlan.parseRange("", 5))
        assertNull(ExportPlan.parseRange("   ", 5))
        assertNull(ExportPlan.parseRange(",", 5))
    }

    @Test
    fun anEmptyPartIsNotValid() {
        assertNull(ExportPlan.parseRange("1,,3", 5))
        assertNull(ExportPlan.parseRange("1,", 5))
    }

    @Test
    fun pagesOutsideTheDocumentAreNotValid() {
        assertNull(ExportPlan.parseRange("0", 5))
        assertNull(ExportPlan.parseRange("6", 5))
        assertNull(ExportPlan.parseRange("4-9", 5))
    }

    @Test
    fun aReversedOrOpenRangeIsNotValid() {
        assertNull(ExportPlan.parseRange("3-1", 5))
        assertNull(ExportPlan.parseRange("2-", 5))
        assertNull(ExportPlan.parseRange("-2", 5))
        assertNull(ExportPlan.parseRange("1-2-3", 5))
    }

    @Test
    fun textThatIsNotANumberIsNotValid() {
        assertNull(ExportPlan.parseRange("a", 5))
        assertNull(ExportPlan.parseRange("1.5", 5))
        assertNull(ExportPlan.parseRange("+2", 5))
        assertNull(ExportPlan.parseRange("99999999999", 5))
    }

    @Test
    fun aChoiceBecomesPageIndices() {
        assertEquals(listOf(0, 1, 2), ExportPlan.pages(PageChoice.All, 3, 1))
        assertEquals(listOf(1), ExportPlan.pages(PageChoice.Current, 3, 1))
        assertEquals(listOf(2), ExportPlan.pages(PageChoice.Range("3"), 3, 0))
    }

    @Test
    fun theCurrentPageMustExist() {
        assertNull(ExportPlan.pages(PageChoice.Current, 3, 3))
        assertNull(ExportPlan.pages(PageChoice.Current, 0, 0))
    }

    @Test
    fun anA4PageIsDrawnAt300Dpi() {
        val size = ExportPlan.imageSize(595.28f, 841.89f)
        assertEquals(2480, size.width)
        assertEquals(3508, size.height)
        assertEquals(300f / 72f, size.scale, 1e-4f)
    }

    @Test
    fun aLargePageIsScaledDownToTheCap() {
        // A3 at 300 dpi would be 4961 by 3508.
        val size = ExportPlan.imageSize(1190.55f, 841.89f)
        assertEquals(ExportPlan.MAX_SIDE, size.width)
        assertTrue(size.height in 2890..2900)
        assertEquals(ExportPlan.MAX_SIDE / 1190.55f, size.scale, 1e-4f)
    }

    @Test
    fun aTallAreaIsCappedOnItsHeight() {
        val size = ExportPlan.imageSize(500f, 6000f)
        assertEquals(ExportPlan.MAX_SIDE, size.height)
        assertTrue(size.width in 340..344)
    }

    @Test
    fun aSliverIsStillOnePixel() {
        val size = ExportPlan.imageSize(0.1f, 20000f)
        assertEquals(1, size.width)
        assertEquals(ExportPlan.MAX_SIDE, size.height)
    }

    @Test
    fun anEmptyAreaStillHasASize() {
        val size = ExportPlan.imageSize(0f, 0f)
        assertEquals(1, size.width)
        assertEquals(1, size.height)
    }

    @Test
    fun fileNamesAvoidCharactersProvidersReject() {
        assertEquals("a_b_c", ExportPlan.safeName("a/b:c", "Untitled"))
        assertEquals("Untitled", ExportPlan.safeName("  ", "Untitled"))
        assertEquals("Maths p3.png", ExportPlan.imageName("Maths", 2, "Untitled"))
        assertEquals("Untitled p1.png", ExportPlan.imageName("", 0, "Untitled"))
    }

    @Test
    fun aSharedCopyIsNamedAfterTheNote() {
        assertEquals("Maths.pdf", ExportPlan.pdfName("Maths", "Untitled"))
        assertEquals("a_b.pdf", ExportPlan.pdfName("a/b", "Untitled"))
        assertEquals("Untitled.pdf", ExportPlan.pdfName(" ", "Untitled"))
    }
}
