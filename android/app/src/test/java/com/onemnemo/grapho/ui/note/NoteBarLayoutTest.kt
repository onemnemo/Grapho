package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBarLayoutTest {
    @Test
    fun compactWindowsOpenLayersAtTheBottom() {
        assertEquals(LayersPlacement.Bottom, layersPlacement(360f))
        assertEquals(LayersPlacement.Bottom, layersPlacement(599.9f))
    }

    @Test
    fun mediumAndWiderWindowsOpenLayersOnTheSide() {
        assertEquals(LayersPlacement.Side, layersPlacement(600f))
        assertEquals(LayersPlacement.Side, layersPlacement(1280f))
    }

    @Test
    fun insertFoldsIntoMoreOnCompactWindows() {
        assertFalse(insertOnBar(411f))
        assertTrue(insertOnBar(600f))
    }

    @Test
    fun floatingBarKeepsItsCapWhenThereIsRoom() {
        assertEquals(720f, floatingBarMaxWidthDp(1280f, 720f), 0f)
    }

    @Test
    fun floatingBarLeavesTheMarginOnNarrowWindows() {
        assertEquals(336f, floatingBarMaxWidthDp(360f, 720f), 0f)
        assertEquals(0f, floatingBarMaxWidthDp(10f, 720f), 0f)
    }

    @Test
    fun savedPositionAndAutoHideFallBackToTheirDefaults() {
        assertEquals(BarPosition.Top, barPositionFrom(null))
        assertEquals(BarPosition.Top, barPositionFrom("Diagonal"))
        assertEquals(AutoHide.Off, autoHideFrom(null))
        assertEquals(AutoHide.Off, autoHideFrom("Sometimes"))
    }

    @Test
    fun everyPositionAndAutoHideSurvivesItsSavedName() {
        for (p in BarPosition.entries) assertEquals(p, barPositionFrom(p.name))
        for (a in AutoHide.entries) assertEquals(a, autoHideFrom(a.name))
    }

    @Test
    fun onlyLeftAndRightAreVertical() {
        assertEquals(listOf(BarPosition.Left, BarPosition.Right), BarPosition.entries.filter { it.vertical })
    }

    @Test
    fun aFloatingBarOccupiesItsThicknessAndGapOnItsOwnEdge() {
        assertEquals(EdgeInsets(top = 60f), barOccupied(BarPosition.Top, NoteBarStyle.Floating, 48f, 12f))
        assertEquals(EdgeInsets(bottom = 60f), barOccupied(BarPosition.Bottom, NoteBarStyle.Floating, 48f, 12f))
        assertEquals(EdgeInsets(left = 60f), barOccupied(BarPosition.Left, NoteBarStyle.Floating, 48f, 12f))
        assertEquals(EdgeInsets(right = 60f), barOccupied(BarPosition.Right, NoteBarStyle.Floating, 48f, 12f))
    }

    @Test
    fun aFlatBarOccupiesAndReservesItsThickness() {
        assertEquals(EdgeInsets(right = 48f), barOccupied(BarPosition.Right, NoteBarStyle.Line, 48f, 12f))
        assertEquals(EdgeInsets(right = 48f), barReserve(BarPosition.Right, NoteBarStyle.Line, 48f))
        assertEquals(EdgeInsets(), barReserve(BarPosition.Right, NoteBarStyle.Floating, 48f))
    }

    @Test
    fun theOverlapOfAFloatingBarIsWhatTheReserveLeavesOver() {
        val occupied = barOccupied(BarPosition.Left, NoteBarStyle.Floating, 48f, 12f)
        assertEquals(occupied, occupied - barReserve(BarPosition.Left, NoteBarStyle.Floating, 48f))
        val line = barOccupied(BarPosition.Left, NoteBarStyle.Line, 48f, 12f)
        assertEquals(EdgeInsets(), line - barReserve(BarPosition.Left, NoteBarStyle.Line, 48f))
    }

    @Test
    fun readingAndAnOffBarAreNeverHidden() {
        assertFalse(barHidden(AutoHide.Always, readMode = true, writing = true, summoned = false))
        assertFalse(barHidden(AutoHide.Off, readMode = false, writing = true, summoned = false))
    }

    @Test
    fun aWhileWritingBarLeavesOnlyWhileWriting() {
        assertTrue(barHidden(AutoHide.WhileWriting, readMode = false, writing = true, summoned = false))
        assertFalse(barHidden(AutoHide.WhileWriting, readMode = false, writing = false, summoned = false))
    }

    @Test
    fun anAlwaysBarIsAwayUntilItIsCalledBack() {
        assertTrue(barHidden(AutoHide.Always, readMode = false, writing = false, summoned = false))
        assertFalse(barHidden(AutoHide.Always, readMode = false, writing = false, summoned = true))
    }

    @Test
    fun clearanceIsAddedOnlyWhereTheBarIs() {
        assertEquals(EdgeInsets(left = 68f), EdgeInsets(left = 60f).clearOf(8f))
        assertEquals(EdgeInsets(), EdgeInsets().clearOf(8f))
    }

    @Test
    fun theLayersPanelAtTheFootKeepsClearOfABarOnItsSides() {
        val margins = layersInsets(LayersPlacement.Bottom, EdgeInsets(left = 60f), 8f)
        assertEquals(EdgeInsets(left = 68f, top = 8f, right = 8f, bottom = 8f), margins)
        assertEquals(68f, layersInsets(LayersPlacement.Bottom, EdgeInsets(bottom = 60f), 8f).bottom, 0f)
    }

    @Test
    fun theLayersCardSitsBelowATopBarAndBesideARightBar() {
        assertEquals(EdgeInsets(top = 68f, right = 8f), layersInsets(LayersPlacement.Side, EdgeInsets(top = 60f), 8f))
        assertEquals(EdgeInsets(top = 8f, right = 68f), layersInsets(LayersPlacement.Side, EdgeInsets(right = 60f), 8f))
        assertEquals(EdgeInsets(top = 8f, right = 8f), layersInsets(LayersPlacement.Side, EdgeInsets(left = 60f), 8f))
    }
}
