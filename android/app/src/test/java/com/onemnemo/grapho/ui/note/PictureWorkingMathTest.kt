package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureWorkingMathTest {
    @Test
    fun theLineIsHiddenAtBothEndsOfAPass() {
        assertEquals(0f, ScanSweep.alpha(0f), 0f)
        assertEquals(0f, ScanSweep.alpha(1f), 0f)
    }

    @Test
    fun theLineFadesInThenHoldsThenFadesOut() {
        assertEquals(0.5f, ScanSweep.alpha(0.06f), 0.001f)
        assertEquals(1f, ScanSweep.alpha(0.5f), 0f)
        assertEquals(0.5f, ScanSweep.alpha(0.94f), 0.001f)
    }

    @Test
    fun theLineEndsFlushWithTheFarEdge() {
        assertEquals(0f, ScanSweep.offset(0f, 200f, 2f), 0f)
        assertEquals(198f, ScanSweep.offset(1f, 200f, 2f), 0f)
        assertEquals(99f, ScanSweep.offset(0.5f, 200f, 2f), 0f)
    }

    @Test
    fun aLineThickerThanTheBoxStaysPut() {
        assertEquals(0f, ScanSweep.offset(0.7f, 1f, 2f), 0f)
    }

    @Test
    fun thePillCentresUnderThePicture() {
        val spot = PillPlacement.below(100f, 300f, 400f, 12f, 160f, 40f, 1000f, 800f, EdgeInsets())
        assertEquals(120f, spot.x, 0f)
        assertEquals(412f, spot.y, 0f)
    }

    @Test
    fun thePillStaysInsideTheWindowSideways() {
        val left = PillPlacement.below(-50f, 20f, 400f, 12f, 160f, 40f, 1000f, 800f, EdgeInsets(left = 8f))
        assertEquals(8f, left.x, 0f)
        val right = PillPlacement.below(950f, 1100f, 400f, 12f, 160f, 40f, 1000f, 800f, EdgeInsets(right = 8f))
        assertEquals(832f, right.x, 0f)
    }

    @Test
    fun thePillMovesInsideThePictureWhenThereIsNoRoomUnderIt() {
        val spot = PillPlacement.below(100f, 300f, 790f, 12f, 160f, 40f, 1000f, 800f, EdgeInsets(bottom = 8f))
        assertEquals(738f, spot.y, 0f)
    }

    @Test
    fun thePillKeepsClearOfAFloatingBarAtTheBottom() {
        val spot = PillPlacement.below(100f, 300f, 700f, 12f, 160f, 40f, 1000f, 800f, EdgeInsets(bottom = 100f))
        assertEquals(648f, spot.y, 0f)
    }

    @Test
    fun theCompareOfferHoldsUntilAnythingElseIsEdited() {
        val offer = CompareOffer(0, longArrayOf(1, 2), 7, longArrayOf(3, 4))
        assertTrue(offer.stillCurrent(7))
        assertFalse(offer.stillCurrent(8))
    }
}
