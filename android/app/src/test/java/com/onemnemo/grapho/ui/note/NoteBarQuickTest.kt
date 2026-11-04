package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBarQuickTest {
    private val black = 0xFF000000.toInt()
    private val palette = listOf(1, 2, 3, 4, 5, 6).map { it or black }

    @Test
    fun theFirstColoursOfThePaletteShow() {
        assertEquals(palette.take(5), quickColours(palette, palette[2]))
    }

    @Test
    fun theColourInUseAlwaysShows() {
        val custom = 0x123456 or black
        assertEquals(palette.take(4) + custom, quickColours(palette, custom))
    }

    @Test
    fun aShortPaletteGainsTheColourInUse() {
        val custom = 0x123456 or black
        assertEquals(palette.take(2) + custom, quickColours(palette.take(2), custom))
    }

    @Test
    fun aSizeNearAQuickSizePicksIt() {
        assertEquals(40, quickSizeAt(42))
        assertNull(quickSizeAt(55))
    }

    private fun measure(width: Float, grow: Float = 0f) = BarMeasure(
        barLengthDp = width,
        fixedSlots = 6,
        slotDp = 50f,
        separatorDp = 10f,
        paddingDp = 8f,
        titleMinDp = 96f,
        quickDp = 300f,
        quickGrowDp = grow,
    )

    @Test
    fun aWideBarHoldsTheQuickColours() {
        val fit = fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, true, measure(1400f))
        assertTrue(fit.quick)
        assertTrue(fit.showTitle)
    }

    @Test
    fun theQuickColoursComeBeforeTheTitle() {
        val fit = fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, true, measure(1020f))
        assertTrue(fit.quick)
        assertFalse(fit.showTitle)
    }

    @Test
    fun aFloatingBarGrowsToHoldThem() {
        assertFalse(fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, false, measure(720f)).quick)
        assertTrue(fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, false, measure(720f, grow = 300f)).quick)
    }

    @Test
    fun theyWaitUntilEveryToolFits() {
        assertFalse(fitBar(NoteBarTool.defaults, NoteBarTool.Pen, false, false, measure(500f, grow = 300f)).quick)
    }
}
