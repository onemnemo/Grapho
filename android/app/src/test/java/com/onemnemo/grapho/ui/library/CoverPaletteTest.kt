package com.onemnemo.grapho.ui.library

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.onemnemo.grapho.ui.theme.CanvasPalette
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.CoverPreset
import com.onemnemo.grapho.ui.theme.FolderColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverPaletteTest {
    @Test
    fun aPresetColourShowsItsDrawing() {
        for (preset in CoverPreset.entries) {
            assertEquals(preset, CoverPalette.resolve(preset.base.toArgb()).preset)
        }
    }

    @Test
    fun aNotebookWithoutACoverIsPlain() {
        val look = CoverPalette.resolve(null)
        assertEquals(CoverPreset.Plain, look.preset)
        assertEquals(CoverPreset.Plain.base, look.base)
    }

    @Test
    fun everyColourOfTheFirstReleaseStillRendersAsAPlainDesignColour() {
        for ((_, old) in FolderColors.all) {
            val look = CoverPalette.resolve(old.toArgb())
            assertEquals(CoverPreset.Plain, look.preset)
            assertNotEquals(old, look.base)
            assertTrue(look.base in CoverPalette.swatches)
        }
    }

    @Test
    fun anUnknownColourRendersAsItself() {
        val odd = Color(0xFF123456)
        val look = CoverPalette.resolve(odd.toArgb())
        assertEquals(odd, look.base)
        assertEquals(CoverPreset.Plain, look.preset)
    }

    @Test
    fun aColourPickedOnItsOwnNeverPicksUpADrawing() {
        for (swatch in CoverPalette.swatches) {
            val stored = CoverPalette.solid(swatch)
            assertEquals(CoverPreset.Plain, CoverPalette.resolve(stored).preset)
            assertTrue("one step in blue at most", kotlin.math.abs((stored and 0xFF) - (swatch.toArgb() and 0xFF)) <= 1)
        }
    }

    @Test
    fun aColourThatIsNoPresetIsStoredAsPicked() {
        val wine = Color(0xFF5B1F2B)
        assertEquals(wine.toArgb(), CoverPalette.solid(wine))
    }

    @Test
    fun canvasPresetsAreOpaqueWithAKnownPattern() {
        for (preset in CanvasPalette.presets) {
            assertEquals(1f, preset.background.alpha, 0f)
            assertTrue(preset.pattern in 0..3)
        }
    }
}
