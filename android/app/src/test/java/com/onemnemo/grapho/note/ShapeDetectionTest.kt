package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeDetectionTest {
    @Test
    fun snapOnHoldIsTheDefaultForAnythingMissingOrUnknown() {
        assertEquals(ShapeDetection.SNAP_ON_HOLD, ShapeDetection.fromKey(null))
        assertEquals(ShapeDetection.SNAP_ON_HOLD, ShapeDetection.fromKey("sometimes"))
    }

    @Test
    fun everyChoiceComesBackFromItsKey() {
        for (mode in ShapeDetection.entries) assertEquals(mode, ShapeDetection.fromKey(mode.key))
    }

    @Test
    fun onlyOneChoiceSuggestsAndOnlyOneSnapsOnHold() {
        assertEquals(listOf(ShapeDetection.SUGGEST), ShapeDetection.entries.filter { it.suggests })
        assertEquals(listOf(ShapeDetection.SNAP_ON_HOLD), ShapeDetection.entries.filter { it.snapsOnHold })
        assertFalse(ShapeDetection.OFF.suggests || ShapeDetection.OFF.snapsOnHold)
    }

    @Test
    fun theCoreCodesNameTheirGuesses() {
        assertEquals(ShapeGuess.LINE, ShapeGuess.fromCode(1))
        assertEquals(ShapeGuess.CIRCLE, ShapeGuess.fromCode(3))
        assertEquals(ShapeGuess.ELBOW, ShapeGuess.fromCode(9))
        assertNull(ShapeGuess.fromCode(0))
        assertNull(ShapeGuess.fromCode(10))
        assertEquals((1..9).toList(), ShapeGuess.entries.map { it.code })
    }

    @Test
    fun aSmallStrokeIsWritingAndABigOneMayBeAShape() {
        assertFalse(worthSuggesting(widthPx = 30f, heightPx = 40f, minPx = 100f))
        assertTrue(worthSuggesting(widthPx = 60f, heightPx = 80f, minPx = 100f))
        assertTrue(worthSuggesting(widthPx = 200f, heightPx = 0f, minPx = 100f))
    }
}
