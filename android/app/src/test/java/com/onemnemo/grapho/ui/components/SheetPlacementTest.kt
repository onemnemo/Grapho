package com.onemnemo.grapho.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetPlacementTest {
    @Test
    fun compactWindowsPinTheSheetToTheBottom() {
        assertEquals(SheetPlacement.Bottom, sheetPlacement(360f))
        assertEquals(SheetPlacement.Bottom, sheetPlacement(599.9f))
    }

    @Test
    fun mediumAndWiderWindowsCentreTheSheet() {
        assertEquals(SheetPlacement.Centered, sheetPlacement(600f))
        assertEquals(SheetPlacement.Centered, sheetPlacement(1280f))
    }

    @Test
    fun centredSheetKeepsItsTokenWidthWhenThereIsRoom() {
        assertEquals(560f, centredSheetWidthDp(1000f, 560f, 16f), 0f)
    }

    @Test
    fun centredSheetLeavesTheMarginOnNarrowWindows() {
        assertEquals(568f, centredSheetWidthDp(600f, 760f, 16f), 0f)
        assertEquals(0f, centredSheetWidthDp(20f, 560f, 16f), 0f)
    }
}
