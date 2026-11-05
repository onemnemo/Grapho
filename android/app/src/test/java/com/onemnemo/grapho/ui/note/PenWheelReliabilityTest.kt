package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.note.ToolMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenWheelReliabilityTest {
    @Test
    fun `a tap off the drawn band is not on it`() {
        assertFalse(onBand(120f, 143f, 19f))
        assertTrue(onBand(143f, 143f, 19f))
        assertTrue(onBand(160f, 143f, 19f))
    }

    @Test
    fun `half faded tools on the docked ring are not hit`() {
        assertTrue(toolFade(false, 76f) >= MIN_HIT_FADE)
        assertTrue(toolFade(false, 90f) < MIN_HIT_FADE)
        assertEquals(1f, toolFade(true, 170f), 0f)
    }

    @Test
    fun `a palm or a finger right after the pen is not a tap on the tab`() {
        assertTrue(tabTouchWanted(8f, false, 2000L))
        assertFalse(tabTouchWanted(20f, false, 2000L))
        assertFalse(tabTouchWanted(8f, true, 2000L))
        assertFalse(tabTouchWanted(8f, false, 100L))
    }

    @Test
    fun `a fill armed from the highlighter keeps it lit`() {
        val state = ToolbarState()
        state.mode = ToolMode.HIGHLIGHTER
        state.mode = ToolMode.FILL
        assertEquals(ToolMode.HIGHLIGHTER, state.barMode())
        state.mode = ToolMode.ERASER
        assertEquals(ToolMode.ERASER, state.barMode())
    }
}
