package com.onemnemo.grapho.ui.library

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** How long ago a note was edited, as data. The screen turns it into words, so the text lives in resources. */
sealed interface Ago {
    data object JustNow : Ago
    data class Minutes(val n: Int) : Ago
    data class Hours(val n: Int) : Ago
    data object Yesterday : Ago
    data class Days(val n: Int) : Ago
    data class On(val date: String) : Ago
}

fun agoOf(modifiedMs: Long, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Ago {
    val minutes = (nowMs - modifiedMs) / 60_000
    if (minutes < 1) return Ago.JustNow
    if (minutes < 60) return Ago.Minutes(minutes.toInt())
    val then = Instant.ofEpochMilli(modifiedMs).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return when (val days = ChronoUnit.DAYS.between(then, today)) {
        in Long.MIN_VALUE..0L -> Ago.Hours(maxOf(1L, minutes / 60).toInt())
        1L -> Ago.Yesterday
        in 2L..6L -> Ago.Days(days.toInt())
        else -> Ago.On(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(then))
    }
}
