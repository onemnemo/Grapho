package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionFlipTest {
    private fun apply(m: FloatArray, x: Float, y: Float) = (m[0] * x + m[1] * y + m[2]) to (m[3] * x + m[4] * y + m[5])

    @Test
    fun horizontalFlipMirrorsAcrossTheMiddleColumn() {
        val m = SelectionFlip.matrix(horizontal = true, cx = 50f, cy = 20f)
        assertEquals(80f to 7f, apply(m, 20f, 7f))
        assertEquals(50f to 33f, apply(m, 50f, 33f))
    }

    @Test
    fun verticalFlipMirrorsAcrossTheMiddleRow() {
        val m = SelectionFlip.matrix(horizontal = false, cx = 50f, cy = 20f)
        assertEquals(12f to 35f, apply(m, 12f, 5f))
    }

    @Test
    fun aFlipUndoesItself() {
        val m = SelectionFlip.matrix(horizontal = true, cx = 31f, cy = 9f)
        val (x, y) = apply(m, 4f, 6f)
        assertEquals(4f to 6f, apply(m, x, y))
    }

    @Test
    fun aReplacementFitsItsFrameAndKeepsItsProportions() {
        val wide = ImageSizing.fit(1, 10f, 20f, 200f, 100f, 400, 100)
        assertEquals(200f, wide.width, 0.01f)
        assertEquals(50f, wide.height, 0.01f)
        assertEquals(10f, wide.x, 0.01f)
        assertEquals(45f, wide.y, 0.01f)
        val tall = ImageSizing.fit(1, 10f, 20f, 200f, 100f, 100, 400)
        assertEquals(25f, tall.width, 0.01f)
        assertEquals(100f, tall.height, 0.01f)
        assertEquals(97.5f, tall.x, 0.01f)
    }
}
