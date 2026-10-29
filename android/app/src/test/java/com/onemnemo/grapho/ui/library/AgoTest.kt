package com.onemnemo.grapho.ui.library

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgoTest {
    private val now = 1_700_000_000_000L
    private val minute = 60_000L

    @Test
    fun underAMinuteIsJustNow() {
        assertEquals(Ago.JustNow, agoOf(now, now))
        assertEquals(Ago.JustNow, agoOf(now - 59_999L, now))
    }

    @Test
    fun aModifiedTimeInTheFutureIsJustNow() {
        assertEquals(Ago.JustNow, agoOf(now + 5 * minute, now))
    }

    @Test
    fun underAnHourCountsMinutes() {
        assertEquals(Ago.Minutes(1), agoOf(now - minute, now))
        assertEquals(Ago.Minutes(59), agoOf(now - 59 * minute, now))
    }

    @Test
    fun laterTheSameDayCountsHours() {
        val zone = ZoneId.of("UTC")
        val noon = LocalDate.of(2026, 3, 4).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(Ago.Hours(3), agoOf(noon - 3 * 60 * minute, noon, zone))
    }

    // Day labels compare against the real calendar date, so build times from today's date.
    private fun noonDaysAgo(days: Long): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).minusDays(days).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    }

    @Test
    fun yesterdayIsNamed() {
        assertEquals(Ago.Yesterday, agoOf(noonDaysAgo(1)))
    }

    @Test
    fun uptoAWeekCountsDays() {
        assertEquals(Ago.Days(2), agoOf(noonDaysAgo(2)))
        assertEquals(Ago.Days(6), agoOf(noonDaysAgo(6)))
    }

    @Test
    fun aWeekOrMoreShowsADate() {
        assertTrue(agoOf(noonDaysAgo(7)) is Ago.On)
    }
}
