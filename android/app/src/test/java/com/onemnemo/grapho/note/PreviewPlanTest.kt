package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewPlanTest {
    @Test
    fun visiblePagesComeFirstFromTheCentreOutwards() {
        assertEquals(listOf(4, 3, 5, 6, 2, 7, 1), PreviewPlan.order(3..5, 4, 10, reach = 2))
    }

    @Test
    fun neighboursStopAtTheEndsOfTheNotebook() {
        assertEquals(listOf(0, 1, 2, 3), PreviewPlan.order(0..1, 0, 4, reach = 2))
        assertEquals(listOf(3, 2, 1), PreviewPlan.order(3..3, 3, 4, reach = 2))
    }

    @Test
    fun noVisiblePageStartsFromTheCentre() {
        assertEquals(listOf(2, 3, 1), PreviewPlan.order(IntRange.EMPTY, 2, 4, reach = 1))
        assertEquals(emptyList<Int>(), PreviewPlan.order(IntRange.EMPTY, 0, 0))
    }

    @Test
    fun aCentreOutsideTheVisibleRangeIsClampedToIt() {
        assertEquals(listOf(2, 1), PreviewPlan.order(1..2, 7, 3, reach = 0))
    }

    @Test
    fun theLongestSideFitsTheLimit() {
        // A4 portrait in points.
        val k = PreviewPlan.scale(595f, 842f, 1024)
        assertEquals(1024 to 724, PreviewPlan.pixels(842f, 595f, k))
        assertEquals(724 to 1024, PreviewPlan.pixels(595f, 842f, k))
        assertEquals(0f, PreviewPlan.scale(0f, 0f, 1024), 0f)
    }

    @Test
    fun farOffContentKeepsItsSize() {
        // Size depends only on width and height, so content thousands of units from the origin is no different.
        val k = PreviewPlan.scale(12000f, 300f, 1536)
        assertEquals(1536 to 38, PreviewPlan.pixels(12000f, 300f, k))
        assertEquals(1 to 1, PreviewPlan.pixels(0.01f, 0.01f, 1f))
    }
}
