package com.onemnemo.grapho.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PopupSpotTest {
    private fun spot(
        left: Int = 100,
        top: Int = 100,
        bottom: Int = 140,
        contentWidth: Int = 200,
        contentHeight: Int = 300,
        right: Int = left + 40,
        side: PopupSide = PopupSide.BelowStart,
    ) = anchoredPopupSpot(
        left,
        top,
        bottom,
        1000,
        800,
        contentWidth,
        contentHeight,
        gap = 4,
        margin = 8,
        anchorRight = right,
        side = side,
    )

    @Test
    fun opensBelowTheAnchorWithStartsAligned() {
        assertEquals(PopupSpot(100, 144), spot())
    }

    @Test
    fun flipsAboveWhenItWouldRunOffTheBottom() {
        assertEquals(PopupSpot(100, 396), spot(top = 700, bottom = 740))
    }

    @Test
    fun staysInsideTheWindowOnTheRight() {
        assertEquals(PopupSpot(792, 144), spot(left = 950))
    }

    @Test
    fun staysInsideTheWindowWhenNeitherSideFits() {
        assertEquals(PopupSpot(100, 8), spot(top = 200, bottom = 240, contentHeight = 790))
    }

    @Test
    fun belowEndLinesUpTheRightEdgesAndRunsLeft() {
        assertEquals(PopupSpot(540, 144), spot(left = 700, side = PopupSide.BelowEnd))
    }

    @Test
    fun aboveEndOpensOverABottomBar() {
        assertEquals(PopupSpot(540, 396), spot(left = 700, top = 700, bottom = 740, side = PopupSide.AboveEnd))
    }

    @Test
    fun aboveEndDropsBelowWhenThereIsNoRoomAbove() {
        assertEquals(PopupSpot(540, 144), spot(left = 700, side = PopupSide.AboveEnd))
    }

    @Test
    fun rightOfSitsBesideALeftBarWithTopsLevel() {
        assertEquals(PopupSpot(144, 100), spot(side = PopupSide.RightOf))
    }

    @Test
    fun leftOfSitsBesideARightBar() {
        assertEquals(PopupSpot(746, 100), spot(left = 950, right = 990, side = PopupSide.LeftOf))
    }
}
