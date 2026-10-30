package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Test

class CutoutBrushTest {
    private val opaque = 0xFF112233.toInt()
    private val gone = 0x00112233

    @Test
    fun aTouchMapsThroughTheCropAndBack() {
        val f = BrushMath.fractionOf(50f, 200f, 0.25f, 0.75f)
        assertEquals(0.375f, f, 0.0001f)
        assertEquals(50f, BrushMath.localOf(f, 200f, 0.25f, 0.75f), 0.001f)
    }

    @Test
    fun aTouchOutsideTheShownAreaStaysOnItsEdge() {
        assertEquals(0.75f, BrushMath.fractionOf(500f, 200f, 0.25f, 0.75f), 0.0001f)
        assertEquals(0.25f, BrushMath.fractionOf(-5f, 200f, 0.25f, 0.75f), 0.0001f)
    }

    @Test
    fun erasingClearsTheAlphaAndKeepsTheColourOfWhatItCovers() {
        val cut = IntArray(100) { opaque }
        val stroke = BrushStroke(true, 0.1f).apply { points.addAll(listOf(0.5f, 0.5f)) }
        BrushMath.apply(listOf(stroke), cut, IntArray(100), 10, 10)
        assertEquals(gone, cut[5 * 10 + 5])
        assertEquals(opaque, cut[0])
    }

    @Test
    fun restoringTakesThePixelsFromTheOriginal() {
        val cut = IntArray(100) { gone }
        val original = IntArray(100) { opaque }
        val stroke = BrushStroke(false, 0.1f).apply { points.addAll(listOf(0.1f, 0.5f, 0.9f, 0.5f)) }
        BrushMath.apply(listOf(stroke), cut, original, 10, 10)
        assertEquals(opaque, cut[5 * 10 + 1])
        assertEquals(opaque, cut[5 * 10 + 5])
        assertEquals(opaque, cut[5 * 10 + 8])
        assertEquals(gone, cut[0])
    }
}
