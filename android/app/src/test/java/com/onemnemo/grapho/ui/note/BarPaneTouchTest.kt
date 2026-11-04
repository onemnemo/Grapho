package com.onemnemo.grapho.ui.note

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarPaneTouchTest {
    private val bar = Rect(0f, 0f, 1000f, 60f)
    private val card = Rect(600f, 70f, 900f, 400f)
    private val page = Offset(300f, 500f)

    @Test
    fun nothingClosesWithNoPaneOpen() {
        assertFalse(paneClosesOn(null, page, bar, card))
    }

    @Test
    fun aTouchOnThePageClosesAMenu() {
        assertTrue(paneClosesOn(BarPane.Image, page, bar, card))
        assertTrue(paneClosesOn(BarPane.More, page, bar, null))
    }

    @Test
    fun aTouchOnTheBarIsLeftToItsButtons() {
        assertFalse(paneClosesOn(BarPane.Insert, Offset(500f, 30f), bar, card))
    }

    @Test
    fun aTouchInsideTheOptionsCardKeepsItOpen() {
        assertFalse(paneClosesOn(BarPane.Shapes, Offset(700f, 200f), bar, card))
        assertFalse(paneClosesOn(BarPane.Text, Offset(700f, 200f), bar, card))
    }

    @Test
    fun theCardOnlyCountsForTheCardPanes() {
        assertTrue(paneClosesOn(BarPane.Image, Offset(700f, 200f), bar, card))
    }

    @Test
    fun layersStaysOpenWhileDrawing() {
        assertFalse(paneClosesOn(BarPane.Layers, page, bar, card))
    }
}
