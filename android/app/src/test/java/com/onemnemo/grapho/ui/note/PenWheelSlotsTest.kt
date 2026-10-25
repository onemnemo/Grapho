package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class PenWheelSlotsTest {
    private val known = listOf("pen", "highlighter", "eraser", "fill", "lasso", "ruler")

    private fun singles(vararg ids: String) = ids.map { WheelSlot.Single(it) }

    @Test
    fun newToolIsAppendedOnce() {
        val saved = singles("pen", "eraser", "fill")
        val next = withNewToolsOffered(saved, known, ORIGINAL_TOOL_IDS)
        assertEquals(singles("pen", "eraser", "fill", "lasso", "ruler"), next)
    }

    @Test
    fun offeredToolIsLeftOff() {
        val saved = singles("pen", "fill")
        assertEquals(saved, withNewToolsOffered(saved, known, known.toSet()))
    }

    @Test
    fun toolAlreadyInAGroupIsNotAddedAgain() {
        val saved = listOf(WheelSlot.Single("pen"), WheelSlot.Group("g1", "Group 1", listOf("lasso")))
        // Lasso stays in its group; only the ruler, which no group holds, joins the ring.
        assertEquals(saved + WheelSlot.Single("ruler"), withNewToolsOffered(saved, known, ORIGINAL_TOOL_IDS))
    }

    @Test
    fun fullWheelTakesNothing() {
        val full = (1..MAX_WHEEL_TOOLS).map { WheelSlot.Single("t$it") }
        assertEquals(full, withNewToolsOffered(full, known, ORIGINAL_TOOL_IDS))
    }

    @Test
    fun retiredSizesToolIsDroppedFromASavedRing() {
        val saved = singles("pen", "sizes", "eraser")
        assertEquals(singles("pen", "eraser"), withoutRetired(saved))
    }

    @Test
    fun retiredSizesToolIsDroppedFromAGroupAndTheGroupStays() {
        val saved = listOf(WheelSlot.Single("pen"), WheelSlot.Group("g1", "Group 1", listOf("sizes", "lasso")))
        val next = withoutRetired(saved)
        assertEquals(listOf(WheelSlot.Single("pen"), WheelSlot.Group("g1", "Group 1", listOf("lasso"))), next)
    }

    @Test
    fun aRingThatHeldOnlySizesHasNoToolsLeft() {
        assertEquals(emptyList<String>(), toolsOn(withoutRetired(singles("sizes"))))
    }

    @Test
    fun aLayoutWithoutRetiredToolsIsUnchanged() {
        val saved = listOf(WheelSlot.Single("pen"), WheelSlot.Group("g1", "Group 1", listOf("lasso")))
        assertEquals(saved, withoutRetired(saved))
    }

    @Test
    fun rulerIsOfferedAfterSizesGoesAndTakesItsPlace() {
        val saved = withoutRetired(singles("pen", "highlighter", "eraser", "fill", "sizes"))
        val offeredBefore = ORIGINAL_TOOL_IDS
        val next = withNewToolsOffered(saved, known, offeredBefore)
        assertEquals(singles("pen", "highlighter", "eraser", "fill", "lasso", "ruler"), next)
    }
}
