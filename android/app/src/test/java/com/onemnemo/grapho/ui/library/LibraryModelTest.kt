package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.NotebookEntry
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryModelTest {
    private val paper = PageBackground(0, 8f, -1)
    private val zone = ZoneId.of("UTC")
    private val noon = LocalDate.of(2026, 10, 29).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val day = 24L * 60 * 60 * 1000

    private fun entry(
        title: String,
        modifiedMs: Long = noon,
        pages: Int = 1,
        infinite: Boolean = false,
        hasPdf: Boolean = false,
        file: String = "$title.grapho",
    ) = NotebookEntry(File(file), title, infinite, null, modifiedMs, pages, hasPdf, paper)

    private fun titles(list: List<NotebookEntry>) = list.map { it.title }

    @Test
    fun kindFollowsTheCanvasAndPdfFlags() {
        assertEquals(EntryKind.Notebook, entry("a").kind)
        assertEquals(EntryKind.Canvas, entry("a", infinite = true).kind)
        assertEquals(EntryKind.Pdf, entry("a", hasPdf = true).kind)
        assertEquals(EntryKind.Canvas, entry("a", infinite = true, hasPdf = true).kind)
    }

    @Test
    fun editedSortsNewestFirstByDefaultDirection() {
        val list = listOf(entry("old", noon - 2 * day), entry("new", noon), entry("mid", noon - day))
        assertEquals(listOf("new", "mid", "old"), titles(sortEntries(list, SortKey.Edited, SortDir.Desc)))
        assertEquals(listOf("old", "mid", "new"), titles(sortEntries(list, SortKey.Edited, SortDir.Asc)))
    }

    @Test
    fun nameSortIgnoresCaseAndReverses() {
        val list = listOf(entry("banana"), entry("Apple"), entry("cherry"))
        assertEquals(listOf("Apple", "banana", "cherry"), titles(sortEntries(list, SortKey.Name, SortDir.Asc)))
        assertEquals(listOf("cherry", "banana", "Apple"), titles(sortEntries(list, SortKey.Name, SortDir.Desc)))
    }

    @Test
    fun sizeSortCountsPagesAndBreaksTiesByName() {
        val list = listOf(entry("b", pages = 3), entry("a", pages = 3), entry("c", pages = 1))
        assertEquals(listOf("c", "a", "b"), titles(sortEntries(list, SortKey.Size, SortDir.Asc)))
        assertEquals(listOf("b", "a", "c"), titles(sortEntries(list, SortKey.Size, SortDir.Desc)))
    }

    @Test
    fun sortingNeverChangesTheInput() {
        val list = listOf(entry("b"), entry("a"))
        sortEntries(list, SortKey.Name, SortDir.Asc)
        assertEquals(listOf("b", "a"), titles(list))
    }

    @Test
    fun matchRangeFindsTheFirstHitIgnoringCase() {
        assertEquals(2..4, matchRange("Calculus", "LCU"))
        assertEquals(0..0, matchRange("a a", "a"))
        assertNull(matchRange("Calculus", "xyz"))
    }

    @Test
    fun blankQueryMatchesNothing() {
        assertNull(matchRange("Calculus", ""))
        assertNull(matchRange("Calculus", "   "))
        assertTrue(searchEntries(listOf(entry("Calculus")), " ").isEmpty())
    }

    @Test
    fun queryIsTrimmedBeforeMatching() {
        assertEquals(listOf("Biology"), titles(searchEntries(listOf(entry("Biology"), entry("Art")), " bio ")))
    }

    @Test
    fun recentSearchesKeepTheNewestFirstWithoutRepeats() {
        assertEquals(listOf("b", "a"), pushRecent(listOf("a"), "b"))
        assertEquals(listOf("A", "b"), pushRecent(listOf("b", "a"), "A"))
        assertEquals(listOf("x"), pushRecent(listOf("x"), "  "))
        assertEquals(5, pushRecent(listOf("1", "2", "3", "4", "5"), "6").size)
        assertEquals("6", pushRecent(listOf("1", "2", "3", "4", "5"), "6").first())
    }

    @Test
    fun noGroupingIsOneGroupInOrder() {
        val groups = groupEntries(listOf(entry("a"), entry("b")), GroupBy.None, noon, zone)
        assertEquals(1, groups.size)
        assertEquals(GroupName.All, groups[0].name)
    }

    @Test
    fun dateGroupsUseCalendarDays() {
        val late = LocalDate.of(2026, 10, 28).atTime(23, 50).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDate.of(2026, 10, 29).atTime(0, 10).atZone(zone).toInstant().toEpochMilli()
        assertEquals(GroupName.Yesterday, dateBucket(late, now, zone))
        assertEquals(GroupName.Today, dateBucket(now, now, zone))
        assertEquals(GroupName.ThisWeek, dateBucket(now - 3 * day, now, zone))
        assertEquals(GroupName.Earlier, dateBucket(now - 9 * day, now, zone))
    }

    @Test
    fun typeGroupsSplitNotebooksPdfsAndCanvases() {
        val list = listOf(entry("n"), entry("c", infinite = true), entry("p", hasPdf = true), entry("m"))
        val groups = groupEntries(list, GroupBy.Type, noon, zone)
        assertEquals(listOf(GroupName.Notebooks, GroupName.Canvases, GroupName.Pdfs), groups.map { it.name })
        assertEquals(listOf("n", "m"), titles(groups[0].items))
    }

    @Test
    fun letterGroupsUseTheFirstLetterAndHashForTheRest() {
        assertEquals("B", letterOf("banana"))
        assertEquals("#", letterOf("42 notes"))
        assertEquals("#", letterOf(""))
        assertEquals("#", letterOf("  "))
        val groups = groupEntries(listOf(entry("apple"), entry("avocado"), entry("7up")), GroupBy.Letter, noon, zone)
        assertEquals(listOf("A", "#"), groups.map { it.letter })
        assertEquals(2, groups[0].items.size)
    }

    @Test
    fun widthClassesFollowTheDesignBreakpoints() {
        assertEquals(WidthClass.Compact, widthClass(360f))
        assertEquals(WidthClass.Compact, widthClass(599.9f))
        assertEquals(WidthClass.Medium, widthClass(600f))
        assertEquals(WidthClass.Medium, widthClass(839.9f))
        assertEquals(WidthClass.Expanded, widthClass(840f))
    }

    @Test
    fun aPhoneShowsTwoColumns() {
        assertEquals(2, gridColumns(328f, 24f, 50))
        assertEquals(2, gridColumns(328f, 24f, 100))
        assertEquals(2, gridColumns(100f, 24f, 0))
    }

    @Test
    fun columnsGrowWithTheWidthAndShrinkWithTheCoverSize() {
        assertEquals(3, gridColumns(652f, 24f, 50))
        assertEquals(7, gridColumns(1216f, 24f, 50))
        assertTrue(gridColumns(1216f, 24f, 0) > gridColumns(1216f, 24f, 100))
    }

    @Test
    fun columnsNeverPassEight() {
        assertEquals(8, gridColumns(4000f, 24f, 0))
    }

    @Test
    fun coverWidthRunsFromSmallToLarge() {
        assertEquals(110f, tileWidthDp(0), 0.01f)
        assertEquals(150f, tileWidthDp(50), 0.01f)
        assertEquals(190f, tileWidthDp(100), 0.01f)
        assertEquals(190f, tileWidthDp(400), 0.01f)
    }

    @Test
    fun togglingAddsAndRemoves() {
        assertEquals(setOf("a"), toggled(emptySet(), "a"))
        assertEquals(emptySet<String>(), toggled(setOf("a"), "a"))
    }

    @Test
    fun selectAllThenNone() {
        val ids = listOf("a", "b", "c")
        assertEquals(setOf("a", "b", "c"), allOrNone(setOf("a"), ids))
        assertEquals(emptySet<String>(), allOrNone(setOf("a", "b", "c"), ids))
        assertEquals(emptySet<String>(), allOrNone(emptySet(), emptyList()))
    }

    @Test
    fun groupHeadingsSitAfterTheLeadingItems() {
        val groups = groupEntries(listOf(entry("apple"), entry("avocado"), entry("berry")), GroupBy.Letter, noon, zone)
        val starts = groupStarts(groups, 1)
        assertEquals(1, starts["LetterA"])
        assertEquals(4, starts["LetterB"])
        assertNull(activeGroupId(starts, 0))
        assertEquals("LetterA", activeGroupId(starts, 3))
        assertEquals("LetterB", activeGroupId(starts, 4))
    }

    @Test
    fun ungroupedNotesHaveNoHeadingSlots() {
        val groups = groupEntries(listOf(entry("a")), GroupBy.None, noon, zone)
        assertTrue(groupStarts(groups, 1).isEmpty())
    }

    @Test
    fun theRailJumpsToTheNearestLetterWithAGroup() {
        val have = setOf("C", "F")
        assertEquals("C", nearestLetter(2, have))
        assertEquals("C", nearestLetter(3, have))
        assertEquals("F", nearestLetter(4, have))
        assertEquals("F", nearestLetter(25, have))
        assertEquals("C", nearestLetter(-5, have))
        assertNull(nearestLetter(3, emptySet()))
    }

    @Test
    fun spacingSnapsToTheClosestTier() {
        assertEquals(5f, nearestSpacing(4f), 0f)
        assertEquals(8f, nearestSpacing(8f), 0f)
        assertEquals(8f, nearestSpacing(9f), 0f)
        assertEquals(12f, nearestSpacing(30f), 0f)
    }
}
