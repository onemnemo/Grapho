package com.onemnemo.grapho.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test
    fun aZeroAnimatorScaleIsReducedMotion() {
        assertTrue(isReducedMotion(0f))
    }

    @Test
    fun anyOtherScaleKeepsMotion() {
        assertFalse(isReducedMotion(1f))
        assertFalse(isReducedMotion(0.5f))
        assertFalse(isReducedMotion(10f))
    }
}
