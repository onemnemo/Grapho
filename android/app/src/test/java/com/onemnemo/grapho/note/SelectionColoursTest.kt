package com.onemnemo.grapho.note

import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionColoursTest {
    private val papers = PaperColors.presets

    private fun contrast(a: Int, b: Int): Float {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    @Test
    fun accentStandsOutOnEveryPaper() {
        for (paper in papers) {
            val accent = SelectionColours.accent(isDarkPaper(paper.argb))
            assertTrue("accent on $paper", contrast(accent, paper.argb) >= 3f)
        }
    }

    @Test
    fun accentIsNeverBlackOnLightPaper() {
        val accent = SelectionColours.accent(isDarkPaper(PaperColors.WHITE.argb))
        assertTrue(contrast(accent, PaperColors.WHITE.argb) < 10f)
    }

    @Test
    fun haloIsTheOppositeToneOfTheAccent() {
        for (dark in listOf(false, true)) {
            val accent = SelectionColours.accent(dark)
            val halo = SelectionColours.halo(dark) or 0xFF000000.toInt()
            assertTrue(contrast(accent, halo) >= 3f)
        }
    }
}
