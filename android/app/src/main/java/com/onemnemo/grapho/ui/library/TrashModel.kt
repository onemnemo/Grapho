package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.NotebookEntry

/** How many days things stay in Trash. The core purges them after this long. */
const val TRASH_DAYS = 30

private const val DAY_MS = 24L * 60 * 60 * 1000

/** One line of Recently deleted: a folder with what went with it, or a notebook that was deleted alone. */
sealed interface TrashRow {
    val key: String
    val deletedAt: Long

    /** What it takes to remove this row for good: the folder or notebook itself and everything inside. */
    val count: Int

    data class Folder(val folder: LibraryFolder, val inside: List<NotebookEntry>) : TrashRow {
        override val key: String get() = folderKey(folder.id)
        override val deletedAt: Long get() = folder.deletedAt ?: 0L
        override val count: Int get() = 1 + inside.size
    }

    data class Item(val entry: NotebookEntry) : TrashRow {
        override val key: String get() = entry.file.name
        override val deletedAt: Long get() = entry.deletedAt ?: 0L
        override val count: Int get() = 1
    }
}

/**
 * Recently deleted as rows, the newest first. A notebook that went to Trash with a folder is listed under that
 * folder, found by the batch both share. Anything else is a row of its own.
 */
fun groupTrash(trashFolders: List<LibraryFolder>, trash: List<NotebookEntry>): List<TrashRow> {
    val claimed = HashSet<String>()
    val rows = ArrayList<TrashRow>()
    for (folder in trashFolders) {
        val batch = folder.deleteBatchId
        val inside = if (batch == null) emptyList() else trash.filter { it.deleteBatchId == batch }
        inside.forEach { claimed.add(it.file.name) }
        rows.add(TrashRow.Folder(folder, inside))
    }
    for (entry in trash) {
        if (entry.file.name !in claimed) rows.add(TrashRow.Item(entry))
    }
    return rows.sortedByDescending { it.deletedAt }
}

/** How many things the rows hold, folders and notebooks alike. */
fun trashCount(rows: List<TrashRow>): Int = rows.sumOf { it.count }

/** Whole days before something deleted at [deletedAt] is removed for good, from 1 up to [TRASH_DAYS]. */
fun daysLeft(deletedAt: Long, nowMs: Long): Int {
    val left = deletedAt + TRASH_DAYS * DAY_MS - nowMs
    return ((left + DAY_MS - 1) / DAY_MS).coerceIn(1L, TRASH_DAYS.toLong()).toInt()
}
