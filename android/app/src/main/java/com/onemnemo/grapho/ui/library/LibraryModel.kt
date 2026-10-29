package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.ui.NotebookEntry
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** The kinds the library tells apart. An imported PDF is a notebook whose pages show PDF pages. */
enum class EntryKind { Notebook, Pdf, Canvas }

val NotebookEntry.kind: EntryKind
    get() = when {
        infinite -> EntryKind.Canvas
        hasPdf -> EntryKind.Pdf
        else -> EntryKind.Notebook
    }

enum class SortKey { Edited, Name, Size }

/** Asc is the natural reading: oldest first, A to Z, smallest first. */
enum class SortDir { Asc, Desc }

enum class GroupBy { None, Date, Type, Letter }

enum class ViewMode { Grid, List }

/** How the library is laid out. This is shell state only, kept between launches and never written into a notebook. */
data class LibraryView(
    val sortKey: SortKey = SortKey.Edited,
    val sortDir: SortDir = SortDir.Desc,
    val group: GroupBy = GroupBy.None,
    val mode: ViewMode = ViewMode.Grid,
    /** Cover size from 0 to 100. */
    val coverSize: Int = DEFAULT_COVER_SIZE,
) {
    companion object {
        const val DEFAULT_COVER_SIZE = 50
    }
}

/** Sorts a copy of [list]. Ties fall back to the name, then the file, so the order never flickers. */
fun sortEntries(list: List<NotebookEntry>, key: SortKey, dir: SortDir): List<NotebookEntry> {
    val byName = compareBy<NotebookEntry> { it.title.trim().lowercase() }.thenBy { it.file.name }
    val primary: Comparator<NotebookEntry> = when (key) {
        SortKey.Edited -> compareBy<NotebookEntry> { it.modifiedMs }.then(byName)
        SortKey.Name -> byName
        SortKey.Size -> compareBy<NotebookEntry> { it.pages }.then(byName)
    }
    return list.sortedWith(if (dir == SortDir.Asc) primary else primary.reversed())
}

/** Where [query] first appears in [title], ignoring case, or null. A blank query matches nothing. */
fun matchRange(title: String, query: String): IntRange? {
    val q = query.trim()
    if (q.isEmpty()) return null
    val at = title.indexOf(q, ignoreCase = true)
    return if (at < 0) null else at until at + q.length
}

fun searchEntries(list: List<NotebookEntry>, query: String): List<NotebookEntry> =
    list.filter { matchRange(it.title, query) != null }

/** The most recent searches first, without repeats, at most [max] long. A blank search is not kept. */
fun pushRecent(recent: List<String>, query: String, max: Int = MAX_RECENT): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return recent
    return (listOf(q) + recent.filter { !it.equals(q, ignoreCase = true) }).take(max)
}

const val MAX_RECENT = 5

/** The headings a group can have. [Letter] is paired with the letter itself. */
enum class GroupName { All, Today, Yesterday, ThisWeek, Earlier, Notebooks, Pdfs, Canvases, Letter }

data class EntryGroup(val name: GroupName, val letter: String, val items: List<NotebookEntry>) {
    /** Stable across reloads, for lazy list keys and the alphabet rail. */
    val id: String get() = "$name$letter"
}

fun letterOf(title: String): String {
    val first = title.trim().firstOrNull() ?: return "#"
    return if (first in 'a'..'z' || first in 'A'..'Z') first.uppercaseChar().toString() else "#"
}

/** Splits an already sorted list into groups. Groups keep the order in which their first item appears. */
fun groupEntries(
    list: List<NotebookEntry>,
    group: GroupBy,
    nowMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<EntryGroup> {
    if (group == GroupBy.None) return listOf(EntryGroup(GroupName.All, "", list))
    val buckets = LinkedHashMap<Pair<GroupName, String>, MutableList<NotebookEntry>>()
    for (entry in list) {
        val key = when (group) {
            GroupBy.Date -> dateBucket(entry.modifiedMs, nowMs, zone) to ""

            GroupBy.Type -> when (entry.kind) {
                EntryKind.Notebook -> GroupName.Notebooks
                EntryKind.Pdf -> GroupName.Pdfs
                EntryKind.Canvas -> GroupName.Canvases
            } to ""

            else -> GroupName.Letter to letterOf(entry.title)
        }
        buckets.getOrPut(key) { mutableListOf() }.add(entry)
    }
    return buckets.map { (key, items) -> EntryGroup(key.first, key.second, items) }
}

/** Calendar days, so a note edited at 23:50 is Yesterday at 00:10. */
fun dateBucket(modifiedMs: Long, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): GroupName {
    val then = Instant.ofEpochMilli(modifiedMs).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return when (ChronoUnit.DAYS.between(then, today)) {
        in Long.MIN_VALUE..0L -> GroupName.Today
        1L -> GroupName.Yesterday
        in 2L..6L -> GroupName.ThisWeek
        else -> GroupName.Earlier
    }
}

/** The window size classes of the design: compact under 600 dp, medium under 840, expanded from there. */
enum class WidthClass { Compact, Medium, Expanded }

fun widthClass(widthDp: Float): WidthClass = when {
    widthDp < MEDIUM_FROM_DP -> WidthClass.Compact
    widthDp < EXPANDED_FROM_DP -> WidthClass.Medium
    else -> WidthClass.Expanded
}

private const val MEDIUM_FROM_DP = 600f
private const val EXPANDED_FROM_DP = 840f

private const val TILE_MIN_DP = 110f
private const val TILE_SPAN_DP = 80f
private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 8

/** The cover width the size slider asks for: 110 dp at 0, 150 dp in the middle, 190 dp at 100. */
fun tileWidthDp(coverSize: Int): Float = TILE_MIN_DP + TILE_SPAN_DP * coverSize.coerceIn(0, 100) / 100f

/** Columns of cover tiles that fit [contentWidthDp] at the asked size, never fewer than two or more than eight. */
fun gridColumns(contentWidthDp: Float, gapDp: Float, coverSize: Int): Int {
    val fit = floor((contentWidthDp + gapDp) / (tileWidthDp(coverSize) + gapDp)).toInt()
    return fit.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
}

/** Selection by file name, so it survives a reload of the list. */
fun toggled(selected: Set<String>, id: String): Set<String> = if (id in selected) selected - id else selected + id

/** Select all, or none when everything is already picked. */
fun allOrNone(selected: Set<String>, everyId: List<String>): Set<String> =
    if (everyId.isNotEmpty() && selected.size >= everyId.size) emptySet() else everyId.toSet()

/** The three spacings of a canvas pattern, close to wide, in millimetres. */
val SPACING_TIERS = listOf(5f, 8f, 12f)

/** The tier closest to [mm], so a canvas saved with another spacing still shows one choice picked. */
fun nearestSpacing(mm: Float): Float = SPACING_TIERS.minBy { kotlin.math.abs(it - mm) }

val RAIL_LETTERS: List<String> = ('A'..'Z').map { it.toString() }

/** The letter of the rail at [index] if it has a group, else the closest one that does, or null if none has. */
fun nearestLetter(index: Int, available: Set<String>): String? {
    val at = index.coerceIn(0, RAIL_LETTERS.lastIndex)
    for (d in 0..RAIL_LETTERS.lastIndex) {
        val below = RAIL_LETTERS.getOrNull(at - d)
        if (below != null && below in available) return below
        val above = RAIL_LETTERS.getOrNull(at + d)
        if (above != null && above in available) return above
    }
    return null
}

/**
 * Where each group's heading sits in the grid, counting [leading] items before the first group. A group takes
 * one slot for its heading and one for each of its notes. Groups with no heading, [GroupName.All], are skipped.
 */
fun groupStarts(groups: List<EntryGroup>, leading: Int): Map<String, Int> {
    val starts = LinkedHashMap<String, Int>()
    var at = leading
    for (group in groups) {
        if (group.name == GroupName.All) {
            at += group.items.size
        } else {
            starts[group.id] = at
            at += 1 + group.items.size
        }
    }
    return starts
}

/** The group whose heading is the last one at or above [firstVisible], or null before the first heading. */
fun activeGroupId(starts: Map<String, Int>, firstVisible: Int): String? =
    starts.entries.lastOrNull { it.value <= firstVisible }?.key
