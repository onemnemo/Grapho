package com.onemnemo.grapho.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SliderMathTest {
    private val hue = 0f..360f

    @Test
    fun valueFollowsThePointerAcrossTheTrack() {
        assertEquals(0f, sliderValueAt(0f, 200f, hue), 0.001f)
        assertEquals(180f, sliderValueAt(100f, 200f, hue), 0.001f)
        assertEquals(360f, sliderValueAt(200f, 200f, hue), 0.001f)
    }

    @Test
    fun valueIsHeldInsideTheRangeWhenTheFingerLeavesTheTrack() {
        assertEquals(0f, sliderValueAt(-40f, 200f, hue), 0.001f)
        assertEquals(360f, sliderValueAt(260f, 200f, hue), 0.001f)
    }

    @Test
    fun anEmptyTrackReadsAsTheStart() {
        assertEquals(0f, sliderValueAt(10f, 0f, hue), 0f)
    }

    @Test
    fun fractionIsTheShareOfTheRange() {
        assertEquals(0.5f, sliderFraction(180f, hue), 0.001f)
        assertEquals(1f, sliderFraction(500f, hue), 0f)
        assertEquals(0f, sliderFraction(1f, 1f..1f), 0f)
    }
}
