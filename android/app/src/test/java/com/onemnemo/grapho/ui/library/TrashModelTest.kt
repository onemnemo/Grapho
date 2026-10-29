package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.NotebookEntry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class TrashModelTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_700_000_000_000L

    private fun deletedFolder(id: String, at: Long, batch: String?) =
        LibraryFolder(id, null, id, null, 0L, 0L, 0L, at, batch)

    private fun deleted(name: String, at: Long, batch: String? = null) = NotebookEntry(
        file = File("$name.grapho"),
        title = name,
        infinite = false,
        coverColor = null,
        modifiedMs = 0L,
        pages = 1,
        hasPdf = false,
        background = PageBackground(0, 8f, -1),
        deletedAt = at,
        deleteBatchId = batch,
    )

    @Test
    fun itemsThatWentWithAFolderAreListedUnderIt() {
        val rows = groupTrash(
            listOf(deletedFolder("trip", now - day, "b1")),
            listOf(deleted("packing", now - day, "b1"), deleted("tickets", now - day, "b1")),
        )
        assertEquals(1, rows.size)
        val folder = rows.single() as TrashRow.Folder
        assertEquals(listOf("packing", "tickets"), folder.inside.map { it.title })
    }

    @Test
    fun anItemDeletedAloneIsItsOwnRow() {
        val rows = groupTrash(
            listOf(deletedFolder("trip", now - 2 * day, "b1")),
            listOf(deleted("packing", now - 2 * day, "b1"), deleted("scan", now - day)),
        )
        assertEquals(listOf("scan.grapho", "f:trip"), rows.map { it.key })
    }

    @Test
    fun aFolderWithoutABatchHoldsNothing() {
        val rows = groupTrash(listOf(deletedFolder("old", now, null)), listOf(deleted("x", now, "other")))
        assertEquals(0, (rows.first { it is TrashRow.Folder } as TrashRow.Folder).inside.size)
        assertEquals(2, rows.size)
    }

    @Test
    fun rowsRunNewestFirst() {
        val rows = groupTrash(
            listOf(deletedFolder("mid", now - 2 * day, "b")),
            listOf(deleted("old", now - 5 * day), deleted("new", now)),
        )
        assertEquals(listOf("new.grapho", "f:mid", "old.grapho"), rows.map { it.key })
    }

    @Test
    fun theCountTakesAFolderAndEverythingInIt() {
        val rows = groupTrash(
            listOf(deletedFolder("trip", now, "b1")),
            listOf(deleted("a", now, "b1"), deleted("b", now, "b1"), deleted("c", now)),
        )
        assertEquals(4, trashCount(rows))
        assertEquals(0, trashCount(emptyList()))
    }

    @Test
    fun daysLeftCountsUpToThirty() {
        assertEquals(30, daysLeft(now, now))
        assertEquals(30, daysLeft(now - 1, now))
        assertEquals(29, daysLeft(now - day, now))
        assertEquals(19, daysLeft(now - 11 * day, now))
    }

    @Test
    fun daysLeftNeverShowsLessThanOneDay() {
        assertEquals(1, daysLeft(now - 29 * day - day / 2, now))
        assertEquals(1, daysLeft(now - 31 * day, now))
    }

    @Test
    fun aTimeInTheFutureStillShowsThirty() {
        assertEquals(30, daysLeft(now + day, now))
    }
}
