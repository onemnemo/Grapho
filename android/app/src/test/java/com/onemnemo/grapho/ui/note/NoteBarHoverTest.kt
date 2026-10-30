package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBarHoverTest {
    private fun near(position: BarPosition, x: Float, y: Float) = penNearBar(position, x, y, 1000f, 800f, band = 80f)

    @Test
    fun aPenOverTheTopEdgeIsNearATopBar() {
        assertTrue(near(BarPosition.Top, 500f, 60f))
        assertFalse(near(BarPosition.Top, 500f, 120f))
    }

    @Test
    fun eachEdgeCountsFromItsOwnSide() {
        assertTrue(near(BarPosition.Bottom, 500f, 750f))
        assertFalse(near(BarPosition.Bottom, 500f, 60f))
        assertTrue(near(BarPosition.Left, 30f, 400f))
        assertTrue(near(BarPosition.Right, 950f, 400f))
        assertFalse(near(BarPosition.Right, 30f, 400f))
    }
}
