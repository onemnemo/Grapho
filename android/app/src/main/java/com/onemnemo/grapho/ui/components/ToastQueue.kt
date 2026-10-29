package com.onemnemo.grapho.ui.components

import kotlin.math.floor

private const val INFO_MS = 4_000
private const val ERROR_MS = 10_000

/** An action keeps the toast up longer, so there is time to reach it. */
private const val ACTION_MS = 8_000

/** The most toasts on screen at once. */
internal const val MAX_VISIBLE_TOASTS = 2

/** One toast. [actionLabel] and [onAction] are set together, and the action is the toast's own Undo or View. */
data class Notice(
    val text: String,
    val isError: Boolean,
    val id: Long,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
) {
    val durationMs: Int get() = when {
        isError -> ERROR_MS
        onAction != null -> ACTION_MS
        else -> INFO_MS
    }

    /** A toast with an action, or a failure, is never dropped to make room: the person may still need it. */
    val keeps: Boolean get() = onAction != null || isError
}

/**
 * Which toasts are on screen and which wait their turn. A new toast takes a free slot. With both slots taken
 * it replaces the oldest one that [Notice.keeps] nothing, and if both keep something it waits, so an Undo is
 * never lost to a newer toast. A waiting toast takes a slot as soon as one frees up.
 */
data class ToastQueue(val visible: List<Notice> = emptyList(), val waiting: List<Notice> = emptyList()) {
    fun offer(notice: Notice): ToastQueue {
        if (visible.size < MAX_VISIBLE_TOASTS) return copy(visible = visible + notice)
        val dropped = visible.firstOrNull { !it.keeps } ?: return copy(waiting = waiting + notice)
        return copy(visible = visible.filter { it.id != dropped.id } + notice)
    }

    fun remove(id: Long): ToastQueue {
        if (waiting.any { it.id == id }) return copy(waiting = waiting.filter { it.id != id })
        val kept = visible.filter { it.id != id }
        if (kept.size == visible.size) return this
        val room = MAX_VISIBLE_TOASTS - kept.size
        return ToastQueue(kept + waiting.take(room), waiting.drop(room))
    }
}

/** How long a toast stays: its own time, or longer when the system or a screen reader asks for more. */
internal fun toastReadingMs(tokenMs: Int, systemMs: Int, screenReaderMs: Int): Long =
    maxOf(tokenMs, systemMs, screenReaderMs).toLong()

/** Snaps [fraction] down to one of [steps] levels, so a progress ring ticks instead of sliding. */
internal fun steppedFraction(fraction: Float, steps: Int): Float {
    val n = steps.coerceAtLeast(1)
    return floor(fraction * n) / n
}
