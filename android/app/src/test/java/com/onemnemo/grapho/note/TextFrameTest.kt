package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class TextFrameTest {
    private val out = FloatArray(2)

    @Test
    fun anUnturnedBoxKeepsItsTopLeftWhenItGrows() {
        TextFrame.regrown(10f, 20f, 100f, 20f, 0f, 60f, out)
        assertEquals(10f, out[0], EPS)
        assertEquals(20f, out[1], EPS)
    }

    @Test
    fun aTurnedBoxKeepsItsTurnedCornerWhenItGrows() {
        val turn = (Math.PI / 2).toFloat()
        val before = FloatArray(2)
        TextFrame.corner(0f, 0f, 100f, 20f, turn, before)
        TextFrame.regrown(0f, 0f, 100f, 20f, turn, 40f, out)
        assertEquals(-10f, out[0], EPS)
        assertEquals(-10f, out[1], EPS)
        val after = FloatArray(2)
        TextFrame.corner(out[0], out[1], 100f, 40f, turn, after)
        assertEquals(before[0], after[0], EPS)
        assertEquals(before[1], after[1], EPS)
    }

    @Test
    fun draggingTheRightSideKeepsTheLeftSideOnThePage() {
        val turn = 0.6f
        val before = FloatArray(2)
        TextFrame.corner(30f, 40f, 100f, 20f, turn, before)
        TextFrame.resized(30f, 40f, 100f, 20f, turn, 160f, 20f, false, out)
        val after = FloatArray(2)
        TextFrame.corner(out[0], out[1], 160f, 20f, turn, after)
        assertEquals(before[0], after[0], EPS)
        assertEquals(before[1], after[1], EPS)
    }

    @Test
    fun draggingTheLeftSideKeepsTheRightSideOnThePage() {
        val turn = 0.6f
        val edge = FloatArray(2)
        val after = FloatArray(2)
        // The top right corner is the top left corner plus the box's own width along its axis.
        TextFrame.corner(30f, 40f, 100f, 20f, turn, edge)
        val rightX = edge[0] + 100f * kotlin.math.cos(turn)
        val rightY = edge[1] + 100f * kotlin.math.sin(turn)
        TextFrame.resized(30f, 40f, 100f, 20f, turn, 60f, 20f, true, out)
        TextFrame.corner(out[0], out[1], 60f, 20f, turn, after)
        assertEquals(rightX, after[0] + 60f * kotlin.math.cos(turn), EPS)
        assertEquals(rightY, after[1] + 60f * kotlin.math.sin(turn), EPS)
    }

    @Test
    fun aDraggedSideNeverMakesTheBoxNarrowerThanTheLeast() {
        assertEquals(TextFrame.MIN_WIDTH, TextFrame.draggedWidth(100f, true, 500f), EPS)
        assertEquals(TextFrame.MIN_WIDTH, TextFrame.draggedWidth(100f, false, -500f), EPS)
        assertEquals(130f, TextFrame.draggedWidth(100f, false, 30f), EPS)
        assertEquals(130f, TextFrame.draggedWidth(100f, true, -30f), EPS)
    }

    @Test
    fun theSameHeightChangesNothing() {
        TextFrame.regrown(5f, 7f, 80f, 30f, 0.7f, 30f, out)
        assertEquals(5f, out[0], EPS)
        assertEquals(7f, out[1], EPS)
    }

    @Test
    fun aBoxIsPulledBackInsideThePage() {
        assertEquals(360f, TextFrame.keptInside(500f, 240f, 600f), EPS)
        assertEquals(0f, TextFrame.keptInside(-20f, 240f, 600f), EPS)
        assertEquals(100f, TextFrame.keptInside(100f, 240f, 600f), EPS)
        assertEquals(0f, TextFrame.keptInside(30f, 700f, 600f), EPS)
    }

    private companion object {
        const val EPS = 0.001f
    }
}
