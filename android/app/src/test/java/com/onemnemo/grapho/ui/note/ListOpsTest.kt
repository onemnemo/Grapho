package com.onemnemo.grapho.ui.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListOpsTest {
    private val list = listOf("a", "b", "c", "d")

    @Test
    fun withoutAtDropsTheItem() {
        assertEquals(listOf("a", "c", "d"), list.withoutAt(1))
        assertEquals(listOf("b", "c", "d"), list.withoutAt(0))
        assertEquals(listOf("a", "b", "c"), list.withoutAt(3))
    }

    @Test
    fun withoutAtOutOfRangeChangesNothing() {
        assertSame(list, list.withoutAt(-1))
        assertSame(list, list.withoutAt(4))
    }

    @Test
    fun restoredAtUndoesWithoutAt() {
        for (i in list.indices) assertEquals(list, list.withoutAt(i).restoredAt(i, list[i]))
    }

    @Test
    fun restoredAtStaysInsideAShorterList() {
        assertEquals(listOf("a", "x"), listOf("a").restoredAt(5, "x"))
        assertEquals(listOf("x", "a"), listOf("a").restoredAt(-2, "x"))
        assertEquals(listOf("x"), emptyList<String>().restoredAt(0, "x"))
    }

    @Test
    fun movedFirstPutsTheItemAtTheFront() {
        assertEquals(listOf("c", "a", "b", "d"), list.movedFirst(2))
        assertEquals(listOf("d", "a", "b", "c"), list.movedFirst(3))
    }

    @Test
    fun movedFirstLeavesTheFrontAndBadIndexesAlone() {
        assertSame(list, list.movedFirst(0))
        assertSame(list, list.movedFirst(-1))
        assertSame(list, list.movedFirst(9))
    }
}
