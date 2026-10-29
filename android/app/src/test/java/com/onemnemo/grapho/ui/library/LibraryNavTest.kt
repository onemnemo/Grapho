package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.LibrarySnapshot
import com.onemnemo.grapho.ui.NotebookEntry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryNavTest {
    private fun folder(id: String, parent: String? = null, name: String = id) =
        LibraryFolder(id, parent, name, null, 0L, 0L, 0L, null)

    private fun entry(name: String, folderId: String? = null, starred: Boolean = false) = NotebookEntry(
        file = File("$name.grapho"),
        title = name,
        infinite = false,
        coverColor = null,
        modifiedMs = 0L,
        pages = 1,
        hasPdf = false,
        background = PageBackground(0, 8f, -1),
        folderId = folderId,
        starred = starred,
    )

    /** maths > algebra > linear, and a separate art. */
    private val folders = listOf(
        folder("maths"),
        folder("art"),
        folder("algebra", "maths"),
        folder("linear", "algebra"),
    )

    @Test
    fun pathRunsFromTheTopLevelDown() {
        assertEquals(listOf("maths", "algebra", "linear"), folderPath(folders, "linear").map { it.id })
        assertEquals(listOf("art"), folderPath(folders, "art").map { it.id })
    }

    @Test
    fun pathOfNothingOrAMissingFolderIsEmpty() {
        assertTrue(folderPath(folders, null).isEmpty())
        assertTrue(folderPath(folders, "gone").isEmpty())
    }

    @Test
    fun pathStopsOnACycle() {
        val loop = listOf(folder("a", "b"), folder("b", "a"))
        assertTrue(folderPath(loop, "a").size <= loop.size + 1)
    }

    @Test
    fun aPlaceInAGoneFolderFallsBackToHome() {
        assertEquals(LibraryPlace.Home, resolvePlace(LibraryPlace.InFolder("gone"), folders))
        assertEquals(LibraryPlace.InFolder("art"), resolvePlace(LibraryPlace.InFolder("art"), folders))
        assertEquals(LibraryPlace.Starred, resolvePlace(LibraryPlace.Starred, folders))
    }

    @Test
    fun backGoesUpOneLevel() {
        assertNull(placeParent(LibraryPlace.Home, folders))
        assertEquals(LibraryPlace.InFolder("algebra"), placeParent(LibraryPlace.InFolder("linear"), folders))
        assertEquals(LibraryPlace.Home, placeParent(LibraryPlace.InFolder("maths"), folders))
        assertEquals(LibraryPlace.Home, placeParent(LibraryPlace.Starred, folders))
        assertEquals(LibraryPlace.Home, placeParent(LibraryPlace.AllFolders, folders))
        assertEquals(LibraryPlace.AllFolders, placeParent(LibraryPlace.Deleted, folders))
    }

    @Test
    fun childrenAreTheFoldersDirectlyUnderAParent() {
        assertEquals(listOf("maths", "art"), childFolders(folders, null).map { it.id })
        assertEquals(listOf("algebra"), childFolders(folders, "maths").map { it.id })
        assertTrue(childFolders(folders, "linear").isEmpty())
    }

    @Test
    fun placesListTheirOwnNotebooks() {
        val list = listOf(
            entry("top"),
            entry("in-maths", "maths"),
            entry("in-art", "art", starred = true),
            entry("star-top", starred = true),
        )
        assertEquals(listOf("top", "star-top"), placeEntries(list, LibraryPlace.Home).map { it.title })
        assertEquals(listOf("in-maths"), placeEntries(list, LibraryPlace.InFolder("maths")).map { it.title })
        assertEquals(listOf("in-art", "star-top"), placeEntries(list, LibraryPlace.Starred).map { it.title })
        assertTrue(placeEntries(list, LibraryPlace.AllFolders).isEmpty())
        assertTrue(placeEntries(list, LibraryPlace.Deleted).isEmpty())
    }

    @Test
    fun theTreeListsEachFolderBeforeItsChildren() {
        val tree = folderTree(folders)
        assertEquals(listOf("maths", "algebra", "linear", "art"), tree.map { it.folder.id })
        assertEquals(listOf(0, 1, 2, 0), tree.map { it.depth })
    }

    @Test
    fun countsAreWhatIsDirectlyInsideAFolder() {
        val list = listOf(entry("a", "maths"), entry("b", "maths"), entry("c", "algebra"))
        assertEquals(FolderCounts(items = 2, folders = 1), folderCounts("maths", list, folders))
        assertEquals(FolderCounts(items = 1, folders = 1), folderCounts("algebra", list, folders))
        assertEquals(FolderCounts(items = 0, folders = 0), folderCounts("art", list, folders))
    }

    @Test
    fun aFolderCannotMoveIntoItselfOrWhatIsInsideIt() {
        val targets = moveTargets(folders, moving = setOf("maths"), origins = setOf(null))
        val blocks = targets.associate { it.id to it.block }
        assertEquals(MoveBlock.Here, blocks[null])
        assertEquals(MoveBlock.Self, blocks["maths"])
        assertEquals(MoveBlock.Inside, blocks["algebra"])
        assertEquals(MoveBlock.Inside, blocks["linear"])
        assertNull(blocks["art"])
    }

    @Test
    fun theTopLevelComesFirstAndFoldersFollowAsATree() {
        val targets = moveTargets(folders)
        assertEquals(listOf(null, "maths", "algebra", "linear", "art"), targets.map { it.id })
        assertEquals(listOf(0, 1, 2, 3, 1), targets.map { it.depth })
        assertTrue(targets.all { it.enabled })
    }

    @Test
    fun theFolderEverythingIsInAlreadyIsMarkedHere() {
        val one = moveTargets(folders, origins = setOf("art")).first { it.id == "art" }
        assertEquals(MoveBlock.Here, one.block)
        val mixed = moveTargets(folders, origins = setOf("art", null))
        assertTrue(mixed.all { it.enabled })
    }

    @Test
    fun selectionKeysTellFoldersFromNotebooks() {
        assertEquals("f:abc", folderKey("abc"))
        assertTrue(isFolderKey(folderKey("abc")))
        assertFalse(isFolderKey("abc.grapho"))
        assertEquals("abc", folderIdOf(folderKey("abc")))
    }

    @Test
    fun contentOfAPlaceFollowsTheSnapshot() {
        val snapshot = LibrarySnapshot(
            entries = listOf(entry("top"), entry("in-maths", "maths")),
            folders = folders,
            trash = emptyList(),
            trashFolders = emptyList(),
        )
        val home = placeContent(snapshot, LibraryPlace.Home)
        assertEquals(listOf("top"), home.here.map { it.title })
        assertEquals(listOf("maths", "art"), home.subfolders.map { it.id })
        val maths = placeContent(snapshot, LibraryPlace.InFolder("maths"))
        assertEquals(listOf("in-maths"), maths.here.map { it.title })
        assertEquals(listOf("algebra"), maths.subfolders.map { it.id })
    }

    @Test
    fun contentOfAGoneFolderIsHome() {
        val snapshot = LibrarySnapshot(listOf(entry("top")), folders, emptyList(), emptyList())
        val content = placeContent(snapshot, LibraryPlace.InFolder("gone"))
        assertEquals(LibraryPlace.Home, content.place)
        assertEquals(listOf("top"), content.here.map { it.title })
    }

    @Test
    fun contentBeforeTheFirstReadIsEmpty() {
        val content = placeContent(null, LibraryPlace.Home)
        assertTrue(content.live.isEmpty())
        assertTrue(content.subfolders.isEmpty())
        assertTrue(content.trash.isEmpty())
    }
}
