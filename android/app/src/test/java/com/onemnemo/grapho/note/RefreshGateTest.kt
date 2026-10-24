package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshGateTest {
    @Test
    fun firstRequestStartsAndLaterOnesQueue() {
        val gate = RefreshGate<String>()
        assertTrue(gate.submit("a", false))
        assertFalse(gate.submit("a", false))
        assertFalse(gate.submit("a", false))
        assertEquals(false, gate.complete("a"))
        assertNull(gate.complete("a"))
        assertTrue(gate.submit("a", false))
    }

    @Test
    fun upgradeRequestSurvivesMerging() {
        val gate = RefreshGate<String>()
        assertTrue(gate.submit("a", false))
        assertFalse(gate.submit("a", true))
        assertFalse(gate.submit("a", false))
        assertEquals(true, gate.complete("a"))
        assertNull(gate.complete("a"))
    }

    @Test
    fun keysAreIndependent() {
        val gate = RefreshGate<String>()
        assertTrue(gate.submit("a", false))
        assertTrue(gate.submit("b", false))
        gate.clear()
        assertTrue(gate.submit("a", false))
    }
}
