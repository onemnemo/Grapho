package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.note.TextAlignment
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBarPlacementTest {
    @Test
    fun theBarSitsCentredAboveTheBox() {
        val spot = FormatBarPlacement.above(100f, 300f, 400f, 440f, 50f, 70f, 160f, 40f, 1000f, 800f, EdgeInsets())
        assertEquals(120f, spot.x, 0.01f)
        assertEquals(310f, spot.y, 0.01f)
    }

    @Test
    fun withNoRoomAboveTheBarGoesUnderTheBox() {
        val spot = FormatBarPlacement.above(100f, 300f, 20f, 60f, 50f, 100f, 160f, 40f, 1000f, 800f, EdgeInsets())
        assertEquals(160f, spot.y, 0.01f)
    }

    @Test
    fun theBarStaysInsideTheWindow() {
        val margins = EdgeInsets(left = 8f)
        val spot = FormatBarPlacement.above(-80f, 20f, 400f, 440f, 50f, 70f, 160f, 40f, 1000f, 800f, margins)
        assertEquals(8f, spot.x, 0.01f)
    }

    @Test
    fun alignmentCyclesRoundAgain() {
        assertEquals(TextAlignment.Centre, nextAlignment(TextAlignment.Start))
        assertEquals(TextAlignment.End, nextAlignment(TextAlignment.Centre))
        assertEquals(TextAlignment.Start, nextAlignment(TextAlignment.End))
    }
}
