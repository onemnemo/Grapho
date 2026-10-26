package com.onemnemo.grapho.note

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideControllerTest {
    private class Host : GuideHost {
        override var currentViewport = Viewport(viewWidth = 1000f, viewHeight = 800f, infinite = true, trueSize = 1f)
        override val density = 1f
        override val touchSlop = 8f
        override val paper = 0xFFFFFFFF.toInt()
        var redraws = 0
        var changes = 0

        override fun redraw() {
            redraws++
        }

        override fun guidesChanged() {
            changes++
        }
    }

    private val host = Host()

    // The test snaps to whole degrees only, so rotation can be checked without the engine.
    private val controller = GuideController(
        host,
        snapAngle = { Math.toRadians(Math.toDegrees(it.toDouble()).roundToInt().toDouble()).toFloat() },
        rulerLabel = { Math.toDegrees(it.toDouble()).roundToInt() },
    )

    @Test
    fun aGuideSwitchedOnAppearsInTheMiddleOfTheView() {
        controller.setRuler(true)
        val ruler = controller.guides.ruler
        assertNotNull(ruler)
        assertEquals(500f, ruler!!.x, 1e-3f)
        assertTrue(controller.rulerOn)
        assertFalse(controller.protractorOn)
        controller.setRuler(false)
        assertNull(controller.guides.ruler)
        assertTrue(host.redraws >= 2)
    }

    @Test
    fun aFingerOffTheGuidePansTheCanvasInstead() {
        controller.setRuler(true)
        assertFalse(controller.begin(20f, 20f))
    }

    @Test
    fun oneFingerDragsTheGuide() {
        controller.setRuler(true)
        val before = controller.guides.ruler!!
        val vp = host.currentViewport
        val x = before.x * vp.scale + vp.offsetX
        val y = before.y * vp.scale + vp.offsetY
        assertTrue(controller.begin(x, y + 20f))
        controller.update(floatArrayOf(x + 60f, y + 50f, 0f, 0f), 1)
        controller.end()
        val after = controller.guides.ruler!!
        assertEquals(before.x + 60f / vp.scale, after.x, 1e-3f)
        assertEquals(before.y + 30f / vp.scale, after.y, 1e-3f)
        assertEquals(0, host.changes)
    }

    @Test
    fun aSecondFingerLandingDoesNotJumpTheGuide() {
        controller.setRuler(true)
        val g = controller.guides.ruler!!
        val vp = host.currentViewport
        val x = g.x * vp.scale + vp.offsetX
        val y = g.y * vp.scale + vp.offsetY
        assertTrue(controller.begin(x, y))
        controller.update(floatArrayOf(x, y, 0f, 0f), 1)
        // The second finger lands far away; with both down the reference is only taken.
        controller.update(floatArrayOf(x, y, x + 300f, y + 90f), 2)
        val landed = controller.guides.ruler!!
        assertEquals(g.x, landed.x, 1e-3f)
        assertEquals(g.y, landed.y, 1e-3f)
        assertEquals(0f, landed.turn, 0f)
    }

    @Test
    fun twoFingersTurnTheGuideAndItSnapsToWholeDegrees() {
        controller.setRuler(true)
        val g = controller.guides.ruler!!
        val vp = host.currentViewport
        val x = g.x * vp.scale + vp.offsetX
        val y = g.y * vp.scale + vp.offsetY
        assertTrue(controller.begin(x, y))
        controller.rebase(floatArrayOf(x - 100f, y, x + 100f, y), 2)
        // A turn of 37.4 degrees.
        val t = Math.toRadians(37.4)
        val dx = (100 * Math.cos(t)).toFloat()
        val dy = (100 * Math.sin(t)).toFloat()
        controller.update(floatArrayOf(x - dx, y - dy, x + dx, y + dy), 2)
        val turned = controller.guides.ruler!!
        assertEquals(Math.toDegrees(turned.turn.toDouble()).toFloat(), 37.4f, 0.01f)
        assertEquals(Math.toDegrees(turned.angle.toDouble()).toFloat(), 37f, 0.01f)
        assertEquals(37, turned.label)
        assertEquals(g.x, turned.x, 1e-2f)
    }

    @Test
    fun aTapOnTheCloseButtonClosesTheGuide() {
        controller.setRuler(true)
        val g = controller.guides.ruler!!
        val vp = host.currentViewport
        val x = g.x * vp.scale + vp.offsetX + GuideMath.RULER_CLOSE * vp.scale
        val y = g.y * vp.scale + vp.offsetY
        assertTrue(controller.begin(x, y))
        controller.end()
        assertNull(controller.guides.ruler)
        assertEquals(1, host.changes)
    }

    @Test
    fun dragFromTheCloseButtonMovesInsteadOfClosing() {
        controller.setRuler(true)
        val g = controller.guides.ruler!!
        val vp = host.currentViewport
        val x = g.x * vp.scale + vp.offsetX + GuideMath.RULER_CLOSE * vp.scale
        val y = g.y * vp.scale + vp.offsetY
        assertTrue(controller.begin(x, y))
        controller.update(floatArrayOf(x + 80f, y, 0f, 0f), 1)
        controller.end()
        assertNotNull(controller.guides.ruler)
        assertEquals(0, host.changes)
    }
}
