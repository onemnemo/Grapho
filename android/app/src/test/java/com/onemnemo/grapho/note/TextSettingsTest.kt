package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSettingsTest {
    @Test
    fun storedSettingsComeBackAsTheyWereSaved() {
        val s = TextSettings(
            size = 40,
            argb = 0xFF336699.toInt(),
            align = TextAlignment.End,
            bold = true,
            italic = true,
            underline = true,
        )
        assertEquals(s, TextSettings.decode(s.encode()))
    }

    @Test
    fun missingOrDamagedStorageGivesTheDefaults() {
        assertEquals(TextSettings(), TextSettings.decode(null))
        assertEquals(TextSettings(), TextSettings.decode(""))
        assertEquals(TextSettings(), TextSettings.decode("12,3"))
    }

    @Test
    fun settingsSavedBeforeItalicAndUnderlineStillLoad() {
        val old = TextSettings.decode("40,-10066330,2,true")
        assertEquals(TextSettings(size = 40, argb = -10066330, align = TextAlignment.End, bold = true), old)
    }

    @Test
    fun numbersStayInRangeAndColoursOpaque() {
        assertEquals(TextSettings.MAX_SIZE, TextSettings.decode("500,-1,0,false").size)
        assertEquals(TextSettings.MIN_SIZE, TextSettings.decode("1,-1,0,false").size)
        assertEquals(0xFF000000.toInt(), TextSettings.decode("20,0,0,false").argb)
        assertEquals(TextAlignment.Start, TextSettings.decode("20,0,9,maybe").align)
        assertEquals(false, TextSettings.decode("20,0,1,maybe").bold)
    }
}
