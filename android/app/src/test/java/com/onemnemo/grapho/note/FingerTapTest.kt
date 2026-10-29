package com.onemnemo.grapho.note

import org.junit.Assert.assertEquals
import org.junit.Test

class FingerTapTest {
    private val tap = FingerTap(slop = 20f)

    private fun land(vararg ms: Long) = ms.forEachIndexed { i, t -> tap.down(i, 100f * i, 100f, t, pen = false) }

    @Test
    fun twoFingersTapping() {
        land(0, 40)
        assertEquals(0, tap.up(120))
        assertEquals(2, tap.up(140))
    }

    @Test
    fun threeFingersTapping() {
        land(0, 30, 60)
        tap.up(150)
        tap.up(160)
        assertEquals(3, tap.up(170))
    }

    @Test
    fun oneFingerIsNoTap() {
        land(0)
        assertEquals(0, tap.up(80))
    }

    @Test
    fun aPanIsNoTap() {
        land(0, 20)
        tap.move(1, 140f, 100f)
        tap.up(150)
        assertEquals(0, tap.up(160))
    }

    @Test
    fun aSmallWobbleStillCounts() {
        land(0, 20)
        tap.move(0, 10f, 105f)
        tap.up(150)
        assertEquals(2, tap.up(160))
    }

    @Test
    fun holdingTooLongIsNoTap() {
        land(0, 20)
        tap.up(500)
        assertEquals(0, tap.up(510))
    }

    @Test
    fun aLateFingerIsNoTap() {
        land(0, FingerTap.LAND_MS + 50)
        tap.up(300)
        assertEquals(0, tap.up(310))
    }

    @Test
    fun aFingerAfterOneLiftsIsNoTap() {
        land(0, 20)
        tap.up(60)
        tap.down(2, 50f, 50f, 80, pen = false)
        tap.up(120)
        assertEquals(0, tap.up(130))
    }

    @Test
    fun thePenSpoilsTheTap() {
        tap.down(0, 0f, 0f, 0, pen = false)
        tap.down(1, 50f, 0f, 20, pen = true)
        tap.up(100)
        assertEquals(0, tap.up(110))
    }

    @Test
    fun aCancelEndsTheTapAndTheNextOneCounts() {
        land(0, 20)
        tap.cancel()
        land(1000, 1020)
        tap.up(1100)
        assertEquals(2, tap.up(1110))
    }

    @Test
    fun aWideContactIsAPalm() {
        val palm = FingerTap(slop = 20f, maxContact = 60f, maxSpread = 400f)
        palm.down(0, 0f, 0f, 0, pen = false, contact = 30f)
        palm.down(1, 50f, 0f, 20, pen = false, contact = 120f)
        palm.up(100)
        assertEquals(0, palm.up(110))
    }

    @Test
    fun aContactThatGrowsWideIsAPalm() {
        val palm = FingerTap(slop = 20f, maxContact = 60f, maxSpread = 400f)
        palm.down(0, 0f, 0f, 0, pen = false, contact = 30f)
        palm.down(1, 50f, 0f, 20, pen = false, contact = 30f)
        palm.move(1, 52f, 0f, contact = 90f)
        palm.up(100)
        assertEquals(0, palm.up(110))
    }

    @Test
    fun fingersFarApartAreNoTap() {
        val spread = FingerTap(slop = 20f, maxContact = 60f, maxSpread = 400f)
        spread.down(0, 0f, 0f, 0, pen = false, contact = 30f)
        spread.down(1, 600f, 0f, 20, pen = false, contact = 30f)
        spread.up(100)
        assertEquals(0, spread.up(110))
    }

    @Test
    fun fingersCloseTogetherStillTap() {
        val spread = FingerTap(slop = 20f, maxContact = 60f, maxSpread = 400f)
        spread.down(0, 0f, 0f, 0, pen = false, contact = 30f)
        spread.down(1, 150f, 0f, 20, pen = false, contact = 30f)
        spread.up(100)
        assertEquals(2, spread.up(110))
    }

    @Test
    fun aLiftCalledOffAsAPalmIsNoTap() {
        land(0, 20)
        tap.up(100, canceled = true)
        assertEquals(0, tap.up(110))
    }
}
