package com.onemnemo.grapho.ui.components

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class TooltipSpotTest {
    private val window = IntSize(1000, 800)
    private val button = IntRect(400, 20, 448, 68)

    private fun spot(side: TooltipSide, anchor: IntRect = button) =
        tooltipSpot(side, anchor, window, width = 80, height = 30, gap = 8, margin = 8)

    @Test
    fun belowCentresUnderTheButton() {
        assertEquals(TooltipSpot(384, 76), spot(TooltipSide.Below))
    }

    @Test
    fun aboveFlipsBelowWhenThereIsNoRoomOverTheButton() {
        assertEquals(TooltipSpot(384, 76), spot(TooltipSide.Above))
    }

    @Test
    fun aboveStaysAboveWhenThereIsRoom() {
        val low = IntRect(400, 700, 448, 748)
        assertEquals(TooltipSpot(384, 662), spot(TooltipSide.Above, low))
    }

    @Test
    fun belowFlipsAboveAtTheFootOfTheWindow() {
        val low = IntRect(400, 740, 448, 788)
        assertEquals(TooltipSpot(384, 702), spot(TooltipSide.Below, low))
    }

    @Test
    fun staysInsideTheWindowMargin() {
        val corner = IntRect(0, 20, 48, 68)
        assertEquals(8, spot(TooltipSide.Below, corner).x)
    }

    @Test
    fun endSitsBesideTheButtonAndCentresOnIt() {
        assertEquals(TooltipSpot(456, 29), spot(TooltipSide.End))
    }

    @Test
    fun startFlipsToTheEndAtTheLeftEdge() {
        val edge = IntRect(0, 100, 48, 148)
        assertEquals(56, spot(TooltipSide.Start, edge).x)
    }
}
