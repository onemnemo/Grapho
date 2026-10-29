package com.onemnemo.grapho.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ToastQueueTest {
    private fun plain(id: Long) = Notice("plain $id", isError = false, id = id)

    private fun undo(id: Long) = Notice("undo $id", isError = false, id = id, actionLabel = "Undo", onAction = {})

    private fun failure(id: Long) = Notice("failed $id", isError = true, id = id)

    private fun ToastQueue.ids() = visible.map { it.id }

    @Test
    fun twoToastsShowTogetherInArrivalOrder() {
        val queue = ToastQueue().offer(plain(1)).offer(plain(2))
        assertEquals(listOf(1L, 2L), queue.ids())
    }

    @Test
    fun aThirdToastReplacesTheOldestWithoutAnAction() {
        val queue = ToastQueue().offer(plain(1)).offer(undo(2)).offer(plain(3))
        assertEquals(listOf(2L, 3L), queue.ids())
        assertEquals(emptyList<Notice>(), queue.waiting)
    }

    @Test
    fun aNewToastNeverCancelsAnUndo() {
        val queue = ToastQueue().offer(undo(1)).offer(undo(2)).offer(plain(3))
        assertEquals(listOf(1L, 2L), queue.ids())
        assertEquals(listOf(3L), queue.waiting.map { it.id })
    }

    @Test
    fun aFailureIsNotReplacedByAPlainToast() {
        val queue = ToastQueue().offer(failure(1)).offer(undo(2)).offer(plain(3))
        assertEquals(listOf(1L, 2L), queue.ids())
    }

    @Test
    fun aFreedSlotGoesToTheEarliestWaitingToast() {
        val full = ToastQueue().offer(undo(1)).offer(undo(2)).offer(plain(3)).offer(plain(4))
        val queue = full.remove(1)
        assertEquals(listOf(2L, 3L), queue.ids())
        assertEquals(listOf(4L), queue.waiting.map { it.id })
    }

    @Test
    fun removingAWaitingToastKeepsTheVisibleOnes() {
        val queue = ToastQueue().offer(undo(1)).offer(undo(2)).offer(plain(3)).remove(3)
        assertEquals(listOf(1L, 2L), queue.ids())
        assertEquals(emptyList<Notice>(), queue.waiting)
    }

    @Test
    fun removingAnUnknownIdChangesNothing() {
        val queue = ToastQueue().offer(plain(1))
        assertEquals(queue, queue.remove(9))
    }

    @Test
    fun readingTimeTakesTheLongestOfTheThreeSources() {
        assertEquals(8_000L, toastReadingMs(8_000, 4_000, 0))
        assertEquals(12_000L, toastReadingMs(8_000, 12_000, 0))
        assertEquals(10_000L, toastReadingMs(4_000, 4_000, 10_000))
    }

    @Test
    fun steppedFractionTicksInWholeSteps() {
        assertEquals(0.5f, steppedFraction(0.74f, 2), 0f)
        assertEquals(1f, steppedFraction(1f, 4), 0f)
        assertEquals(0f, steppedFraction(0.9f, 0), 0f)
    }

    @Test
    fun durationsFollowTheKindOfToast() {
        assertEquals(10_000, failure(1).durationMs)
        assertEquals(8_000, undo(1).durationMs)
        assertEquals(4_000, plain(1).durationMs)
    }
}
