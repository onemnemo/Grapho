package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeSettingsTest {
    @Test
    fun storedSettingsComeBackAsTheyWereSaved() {
        val s = ShapeSettings(
            kind = ShapeKind.STAR,
            points = 7,
            sharpness = 70,
            sides = 8,
            radius = 20,
            path = LinePath.ELBOW,
            start = LineEnd.ARROW,
            end = LineEnd.NONE,
        )
        assertEquals(s, ShapeSettings.decode(s.encode()))
    }

    @Test
    fun missingOrDamagedStorageGivesTheDefaults() {
        assertEquals(ShapeSettings(), ShapeSettings.decode(null))
        assertEquals(ShapeSettings(), ShapeSettings.decode(""))
        assertEquals(ShapeSettings(), ShapeSettings.decode("star,5"))
    }

    @Test
    fun unknownNamesFallBackAndNumbersStayInRange() {
        val s = ShapeSettings.decode("heart,99,200,1,x,zigzag,dot,flag")
        assertEquals(ShapeKind.BOX, s.kind)
        assertEquals(ShapeSettings.MAX_SIDES, s.points)
        assertEquals(ShapeSettings.MAX_SHARPNESS, s.sharpness)
        assertEquals(ShapeSettings.MIN_SIDES, s.sides)
        assertEquals(ShapeSettings().radius, s.radius)
        assertEquals(LinePath.STRAIGHT, s.path)
        assertEquals(LineEnd.NONE, s.start)
        assertEquals(LineEnd.NONE, s.end)
    }

    @Test
    fun theCoreKindsKeepTheirNumbers() {
        assertEquals(listOf(0, 1, 2, 3, 4), ShapeKind.entries.map { it.code })
        assertEquals(listOf(4, 6, 5), LinePath.entries.map { it.code })
    }

    @Test
    fun aBoxPassesItsRadiusInViewPixels() {
        val spec = ShapeSettings(kind = ShapeKind.BOX, radius = 10).spec(scale = 2f, widthPt = 1f)
        assertEquals(0f, spec[0], 0f)
        assertEquals(20f, spec[1], 0f)
    }

    @Test
    fun aStarPassesItsPointsAndSharpness() {
        val spec = ShapeSettings(kind = ShapeKind.STAR, points = 6, sharpness = 40).spec(1f, 1f)
        assertEquals(3f, spec[0], 0f)
        assertEquals(6f, spec[1], 0f)
        assertEquals(0.4f, spec[2], 1e-6f)
    }

    @Test
    fun aPolygonPassesItsSides() {
        val spec = ShapeSettings(kind = ShapeKind.POLYGON, sides = 3).spec(1f, 1f)
        assertEquals(2f, spec[0], 0f)
        assertEquals(3f, spec[1], 0f)
    }

    @Test
    fun aLineTakesItsKindFromThePathAndItsEndsAsBits() {
        val line =
            ShapeSettings(kind = ShapeKind.LINE, path = LinePath.CURVED, start = LineEnd.ARROW, end = LineEnd.ARROW)
        val spec = line.spec(1f, 1f)
        assertEquals(6f, spec[0], 0f)
        assertEquals(3f, spec[4], 0f)
        assertEquals(1f, line.copy(end = LineEnd.NONE).spec(1f, 1f)[4], 0f)
        assertEquals(2f, line.copy(start = LineEnd.NONE).spec(1f, 1f)[4], 0f)
    }

    @Test
    fun closedShapesIgnoreTheLineEnds() {
        val spec = ShapeSettings(kind = ShapeKind.OVAL, start = LineEnd.ARROW, end = LineEnd.ARROW).spec(1f, 1f)
        assertEquals(0f, spec[4], 0f)
    }

    @Test
    fun anArrowHeadGrowsWithTheLineButSlowerAndScalesWithTheView() {
        val thin = ShapeSettings.headSize(1f)
        val thick = ShapeSettings.headSize(10f)
        assertTrue(thick > thin)
        assertTrue(thick / thin < 10f)
        val spec = ShapeSettings(kind = ShapeKind.LINE).spec(scale = 3f, widthPt = 2f)
        assertEquals(ShapeSettings.headSize(2f) * 3f, spec[3], 1e-4f)
    }

    @Test
    fun theSpecAlwaysHasFiveNumbers() {
        for (kind in ShapeKind.entries) {
            assertEquals(5, ShapeSettings(kind = kind).spec(1f, 1f).size)
        }
    }
}
