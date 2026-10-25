package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationTest {
    @Test
    fun aTurnNearAQuarterSnapsToIt() {
        assertEquals(0f, Rotation.snap(3f), 0f)
        assertEquals(90f, Rotation.snap(86.5f), 0f)
        assertEquals(-90f, Rotation.snap(-93f), 0f)
        assertEquals(180f, Rotation.snap(176.5f), 0f)
    }

    @Test
    fun aTurnFartherFromAQuarterIsKept() {
        assertEquals(10f, Rotation.snap(10f), 0f)
        assertEquals(-45f, Rotation.snap(-45f), 0f)
        assertEquals(94.5f, Rotation.snap(94.5f), 0f)
    }

    @Test
    fun theTurnBetweenAnglesTakesTheShortWay() {
        assertEquals(20f, Rotation.between(350f, 10f), 0.001f)
        assertEquals(-20f, Rotation.between(10f, 350f), 0.001f)
        assertEquals(90f, Rotation.between(0f, 90f), 0.001f)
    }

    @Test
    fun aHalfTurnIsReportedAsPositive() {
        assertEquals(180f, Rotation.between(0f, 180f), 0.001f)
        assertEquals(180f, Rotation.between(0f, -180f), 0.001f)
    }
}
