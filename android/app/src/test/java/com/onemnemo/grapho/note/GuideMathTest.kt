package com.onemnemo.grapho.note

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideMathTest {
    private val ruler = GuidePlace(100f, 100f, 0f, 0f, 0)
    private val scratch = FloatArray(2)
    private val out = FloatArray(3)

    @Test
    fun packingPutsTheGuideInViewPixels() {
        val packed = GuideMath.pack(GuideSet(ruler = ruler), 2f, 10f, 20f)
        assertEquals(GuideMath.STRIDE, packed.size)
        assertEquals(0f, packed[0], 0f)
        assertEquals(210f, packed[1], 1e-3f)
        assertEquals(220f, packed[2], 1e-3f)
        assertEquals(GuideMath.RULER_LENGTH * 2f, packed[4], 1e-3f)
        assertEquals(GuideMath.RULER_THICKNESS * 2f, packed[5], 1e-3f)
        assertEquals(0, GuideMath.pack(GuideSet(), 1f, 0f, 0f).size)
    }

    @Test
    fun aRulerOneHundredMillimetresLongSpansTheRightPixels() {
        // At 3 px per point a millimetre is about 8.5 px.
        val mmPx = NotebookInfo.PT_PER_MM * 3f
        val packed = GuideMath.pack(GuideSet(ruler = ruler), 3f, 0f, 0f)
        assertEquals(300f * mmPx, packed[4], 0.01f)
    }

    @Test
    fun theRulerBodyAndItsMarginAreHit() {
        val s = 1f
        assertTrue(GuideMath.hits(GuideKind.RULER, ruler, s, 0f, 0f, 100f, 100f, 0f, scratch))
        val edge = GuideMath.RULER_THICKNESS / 2
        assertTrue(GuideMath.hits(GuideKind.RULER, ruler, s, 0f, 0f, 100f, 100f + edge - 1f, 0f, scratch))
        assertFalse(GuideMath.hits(GuideKind.RULER, ruler, s, 0f, 0f, 100f, 100f + edge + 5f, 0f, scratch))
        assertTrue(GuideMath.hits(GuideKind.RULER, ruler, s, 0f, 0f, 100f, 100f + edge + 5f, 8f, scratch))
    }

    @Test
    fun aTurnedRulerIsHitAlongItsOwnAxis() {
        val turned = GuidePlace(0f, 0f, 0f, (PI / 2).toFloat(), 0)
        val along = GuideMath.RULER_LENGTH / 2 - 5f
        assertTrue(GuideMath.hits(GuideKind.RULER, turned, 1f, 0f, 0f, 0f, along, 0f, scratch))
        assertFalse(GuideMath.hits(GuideKind.RULER, turned, 1f, 0f, 0f, along, 0f, 0f, scratch))
    }

    @Test
    fun theProtractorDiscIsOnlyTheHalfAboveItsBase() {
        val p = GuidePlace(0f, 0f, 0f, 0f, 0)
        val r = GuideMath.PROTRACTOR_RADIUS
        assertTrue(GuideMath.hits(GuideKind.PROTRACTOR, p, 1f, 0f, 0f, 0f, -r / 2, 0f, scratch))
        assertFalse(GuideMath.hits(GuideKind.PROTRACTOR, p, 1f, 0f, 0f, 0f, r / 2, 0f, scratch))
        assertFalse(GuideMath.hits(GuideKind.PROTRACTOR, p, 1f, 0f, 0f, 0f, -r - 10f, 0f, scratch))
    }

    @Test
    fun theCloseButtonsSitWhereTheyAreDrawn() {
        assertTrue(
            GuideMath.hitsClose(
                GuideKind.RULER, ruler, 1f, 0f, 0f, 100f + GuideMath.RULER_CLOSE, 100f, 10f, scratch,
            ),
        )
        assertFalse(GuideMath.hitsClose(GuideKind.RULER, ruler, 1f, 0f, 0f, 100f, 100f, 10f, scratch))
        val p = GuidePlace(50f, 50f, 0f, 0f, 0)
        assertTrue(
            GuideMath.hitsClose(
                GuideKind.PROTRACTOR, p, 1f, 0f, 0f, 50f, 50f - GuideMath.PROTRACTOR_CLOSE, 10f, scratch,
            ),
        )
    }

    @Test
    fun oneFingerMovesTheGuideByTheDragInContentPoints() {
        val from = floatArrayOf(300f, 300f, 0f, 0f)
        val to = floatArrayOf(340f, 280f, 0f, 0f)
        GuideMath.move(ruler, 2f, 0f, 0f, from, to, 1, out)
        assertEquals(120f, out[0], 1e-3f)
        assertEquals(90f, out[1], 1e-3f)
        assertEquals(0f, out[2], 0f)
    }

    @Test
    fun twoFingersTurnTheGuideAboutTheirMiddle() {
        // The fingers start level around the guide's centre and turn a quarter clockwise on screen.
        val from = floatArrayOf(100f, 200f, 300f, 200f)
        val to = floatArrayOf(200f, 100f, 200f, 300f)
        val centred = GuidePlace(200f, 200f, 0f, 0f, 0)
        GuideMath.move(centred, 1f, 0f, 0f, from, to, 2, out)
        assertEquals(200f, out[0], 1e-2f)
        assertEquals(200f, out[1], 1e-2f)
        assertEquals((PI / 2).toFloat(), out[2], 1e-4f)
    }

    @Test
    fun aGuideOffTheMiddleSwingsRoundTheFingers() {
        val from = floatArrayOf(100f, 100f, 300f, 100f)
        val to = floatArrayOf(200f, 0f, 200f, 200f)
        val offset = GuidePlace(300f, 100f, 0f, 0f, 0)
        GuideMath.move(offset, 1f, 0f, 0f, from, to, 2, out)
        // The guide was 100 to the right of the middle (200, 100), and is now 100 below it.
        assertEquals(200f, out[0], 1e-2f)
        assertEquals(200f, out[1], 1e-2f)
    }

    @Test
    fun twoFingersTravellingTogetherDoNotTurnTheGuide() {
        val from = floatArrayOf(100f, 100f, 200f, 100f)
        val to = floatArrayOf(150f, 130f, 250f, 130f)
        GuideMath.move(ruler, 1f, 0f, 0f, from, to, 2, out)
        assertEquals(150f, out[0], 1e-3f)
        assertEquals(130f, out[1], 1e-3f)
        assertEquals(0f, out[2], 1e-6f)
    }

    @Test
    fun theTurnTakesTheShortWayAcrossTheHalfTurn() {
        val from = floatArrayOf(0f, 0f, -100f, 1f)
        val to = floatArrayOf(0f, 0f, -100f, -1f)
        GuideMath.move(ruler, 1f, 0f, 0f, from, to, 2, out)
        assertTrue(kotlin.math.abs(out[2]) < 0.05f)
    }
}
