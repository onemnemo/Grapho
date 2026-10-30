package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelPlacementTest {
    private val left = WheelPlacement(docked = false, edge = DockEdge.Left, along = 500f, floatX = 1f, floatY = 2f)

    @Test
    fun sameOrientationKeepsAlong() {
        val next = placementOnEdge(left, DockEdge.Right)
        assertEquals(DockEdge.Right, next.edge)
        assertEquals(500f, next.along, 0f)
    }

    @Test
    fun otherOrientationStartsAtDefault() {
        val next = placementOnEdge(left, DockEdge.Bottom)
        assertEquals(TAB_DEFAULT_Y, next.along, 0f)
    }

    @Test
    fun dockingReturnsTheFreeWheelToTheEdge() {
        assertTrue(placementOnEdge(left, DockEdge.Left).docked)
    }
}
