package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeGuardTest {
    private fun guard() = EdgeGuard(slop = 10f).apply { setBands(top = 50f, bottom = 950f) }

    @Test
    fun touchesClearOfTheBandsAreNotHeld() {
        val g = guard()
        assertFalse(g.begin(100f, 50f, 0))
        assertFalse(g.begin(100f, 500f, 0))
        assertFalse(g.begin(100f, 950f, 0))
    }

    @Test
    fun quickSwipeDownFromTheTopIsASwipe() {
        val g = guard()
        assertTrue(g.begin(300f, 5f, 0))
        assertEquals(EdgeGuard.Verdict.WAIT, g.move(301f, 9f, 16))
        assertEquals(EdgeGuard.Verdict.SWIPE, g.move(304f, 40f, 40))
    }

    @Test
    fun quickSwipeUpFromTheBottomIsASwipe() {
        val g = guard()
        assertTrue(g.begin(300f, 995f, 0))
        assertEquals(EdgeGuard.Verdict.SWIPE, g.move(296f, 960f, 50))
    }

    @Test
    fun movingTowardsTheEdgeOrSidewaysDraws() {
        val g = guard()
        assertTrue(g.begin(300f, 30f, 0))
        assertEquals(EdgeGuard.Verdict.DRAW, g.move(300f, 10f, 30))
        assertTrue(g.begin(300f, 30f, 100))
        assertEquals(EdgeGuard.Verdict.DRAW, g.move(340f, 45f, 130))
    }

    @Test
    fun slowOrRestingStartDraws() {
        val g = guard()
        assertTrue(g.begin(300f, 990f, 0))
        assertEquals(EdgeGuard.Verdict.DRAW, g.move(300f, 989f, EdgeGuard.DECIDE_MS))
        assertTrue(g.begin(300f, 990f, 1000))
        assertEquals(EdgeGuard.Verdict.DRAW, g.move(300f, 940f, 1000 + EdgeGuard.DECIDE_MS + 20))
    }

    @Test
    fun unguardedEdgesHoldNothing() {
        val g = EdgeGuard(10f)
        g.setBands(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        assertFalse(g.begin(0f, 0f, 0))
        assertFalse(g.begin(0f, -5f, 0))
    }
}
