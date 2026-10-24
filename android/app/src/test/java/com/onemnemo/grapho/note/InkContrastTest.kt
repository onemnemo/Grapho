package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InkContrastTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val penBlack = 0xFF17181C.toInt()

    @Test
    fun blackInkTurnsLightOnDarkPaper() {
        assertEquals(0xFFECECEC.toInt(), displayColor(penBlack, PaperColors.BLACK.argb))
        assertEquals(0xFFECECEC.toInt(), displayColor(black, PaperColors.DARK.argb))
        assertEquals(0xFFECECEC.toInt(), displayColor(penBlack, PaperColors.SLATE.argb))
    }

    @Test
    fun whiteInkTurnsDarkOnLightPaper() {
        assertEquals(0xFF1A1A1A.toInt(), displayColor(white, PaperColors.WHITE.argb))
        assertEquals(0xFF1A1A1A.toInt(), displayColor(white, PaperColors.CREAM.argb))
    }

    @Test
    fun matchingInkIsUnchanged() {
        assertEquals(penBlack, displayColor(penBlack, PaperColors.WHITE.argb))
        assertEquals(white, displayColor(white, PaperColors.BLACK.argb))
    }

    @Test
    fun alphaIsKept() {
        assertEquals(0x80ECECEC.toInt(), displayColor(0x80000000.toInt(), PaperColors.BLACK.argb))
        assertEquals(0x401A1A1A, displayColor(0x40FFFFFF, PaperColors.WHITE.argb))
    }

    @Test
    fun colouredInkIsUnchanged() {
        val blue = 0xFF2F5BE0.toInt()
        val darkRed = 0xFF300000.toInt()
        assertEquals(blue, displayColor(blue, PaperColors.BLACK.argb))
        assertEquals(darkRed, displayColor(darkRed, PaperColors.BLACK.argb))
        assertEquals(0xFFF5D33F.toInt(), displayColor(0xFFF5D33F.toInt(), PaperColors.WHITE.argb))
    }

    @Test
    fun midGreyInkIsUnchanged() {
        val graphite = 0xFF5B5F69.toInt()
        assertEquals(graphite, displayColor(graphite, PaperColors.BLACK.argb))
        assertEquals(graphite, displayColor(graphite, PaperColors.WHITE.argb))
    }

    @Test
    fun presetsClassifyAsLightOrDark() {
        assertFalse(isDarkPaper(PaperColors.WHITE.argb))
        assertFalse(isDarkPaper(PaperColors.CREAM.argb))
        assertTrue(isDarkPaper(PaperColors.SLATE.argb))
        assertTrue(isDarkPaper(PaperColors.DARK.argb))
        assertTrue(isDarkPaper(PaperColors.BLACK.argb))
    }

    @Test
    fun blackPaperKeepsAPureBlackDesk() {
        assertEquals(black, PaperChrome.desk(PaperColors.BLACK.argb))
    }

    @Test
    fun hslRoundTrips() {
        for (argb in listOf(0xFFF6F0E1.toInt(), 0xFF3A3B40.toInt(), 0xFF2F5BE0.toInt(), white, black)) {
            assertEquals(argb, Hsl.from(argb).toArgb())
        }
    }
}
