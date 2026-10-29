package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadProgressTest {
    @Test
    fun theFirstPageIsPageOne() {
        assertEquals(ReadPosition(1, 12), ReadProgress.position(0, 12, infinite = false))
    }

    @Test
    fun aMiddlePageIsCountedFromOne() {
        assertEquals(ReadPosition(3, 12), ReadProgress.position(2, 12, infinite = false))
    }

    @Test
    fun aPageOutsideTheNoteIsHeldToItsEnds() {
        assertEquals(ReadPosition(1, 4), ReadProgress.position(-3, 4, infinite = false))
        assertEquals(ReadPosition(4, 4), ReadProgress.position(9, 4, infinite = false))
    }

    @Test
    fun anInfiniteCanvasHasNothingToCount() {
        assertNull(ReadProgress.position(0, 1, infinite = true))
    }

    @Test
    fun anEmptyNoteHasNothingToCount() {
        assertNull(ReadProgress.position(0, 0, infinite = false))
    }
}
