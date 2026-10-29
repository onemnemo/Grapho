package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.NotebookEntry

/** Where in the library the person is. Home shows the top level, a folder shows what is in it. */
sealed interface LibraryPlace {
    data object Home : LibraryPlace
    data class InFolder(val id: String) : LibraryPlace
    data object Starred : LibraryPlace
    data object AllFolders : LibraryPlace
    data object Deleted : LibraryPlace
}

/** [place], or Home when it names a folder that is gone, such as one that was just deleted. */
fun resolvePlace(place: LibraryPlace, folders: List<LibraryFolder>): LibraryPlace =
    if (place is LibraryPlace.InFolder && folders.none { it.id == place.id }) LibraryPlace.Home else place

/** The place that Back goes to, or null at Home. */
fun placeParent(place: LibraryPlace, folders: List<LibraryFolder>): LibraryPlace? = when (place) {
    LibraryPlace.Home -> null

    is LibraryPlace.InFolder -> folders.firstOrNull { it.id == place.id }?.parentId
        ?.let { LibraryPlace.InFolder(it) } ?: LibraryPlace.Home

    LibraryPlace.Starred, LibraryPlace.AllFolders -> LibraryPlace.Home

    LibraryPlace.Deleted -> LibraryPlace.AllFolders
}

/** The folders from the top level down to the one named [id], or nothing when there is no such folder. */
fun folderPath(folders: List<LibraryFolder>, id: String?): List<LibraryFolder> {
    val byId = folders.associateBy { it.id }
    val path = ArrayList<LibraryFolder>()
    var current = id?.let { byId[it] }
    while (current != null && path.size <= folders.size) {
        path.add(0, current)
        current = current.parentId?.let { byId[it] }
    }
    return path
}

/** The folders directly under [parentId] (null for the top level), in the order [folders] has them. */
fun childFolders(folders: List<LibraryFolder>, parentId: String?): List<LibraryFolder> =
    folders.filter { it.parentId == parentId }

/** The notebooks a place lists. A folder place lists what is directly in it, Starred lists them in any folder. */
fun placeEntries(entries: List<NotebookEntry>, place: LibraryPlace): List<NotebookEntry> = when (place) {
    LibraryPlace.Home -> entries.filter { it.folderId == null }
    is LibraryPlace.InFolder -> entries.filter { it.folderId == place.id }
    LibraryPlace.Starred -> entries.filter { it.starred }
    LibraryPlace.AllFolders, LibraryPlace.Deleted -> emptyList()
}

/** A folder with how deep it sits, for the tree of All folders and for the lists of places to move to. */
data class FolderNode(val folder: LibraryFolder, val depth: Int)

/** Every folder, each followed by its subfolders, so the list reads as a tree. */
fun folderTree(folders: List<LibraryFolder>): List<FolderNode> {
    val children = folders.groupBy { it.parentId }
    val out = ArrayList<FolderNode>()
    val seen = HashSet<String>()
    fun walk(parentId: String?, depth: Int) {
        for (folder in children[parentId].orEmpty()) {
            if (!seen.add(folder.id)) continue
            out.add(FolderNode(folder, depth))
            walk(folder.id, depth + 1)
        }
    }
    walk(null, 0)
    return out
}

/** What a folder holds directly. */
data class FolderCounts(val items: Int, val folders: Int)

fun folderCounts(folderId: String, entries: List<NotebookEntry>, folders: List<LibraryFolder>) =
    FolderCounts(entries.count { it.folderId == folderId }, folders.count { it.parentId == folderId })

/** Why a place cannot take what is being moved. */
enum class MoveBlock {
    /** Everything is already there. */
    Here,

    /** The place is one of the folders being moved. */
    Self,

    /** The place is inside one of the folders being moved. */
    Inside,
}

/** A place to move to: the top level when [folder] is null. A [block] means it cannot be picked. */
data class MoveTarget(val folder: LibraryFolder?, val depth: Int, val block: MoveBlock?) {
    val id: String? get() = folder?.id
    val enabled: Boolean get() = block == null
}

/**
 * The top level and every folder as a place to move to. [moving] holds the folders being moved, which can't go
 * into themselves or what is inside them. [origins] holds the folder each thing being moved is in now, so a
 * place that is all of them is marked as already there. The core makes the same checks and has the last word.
 */
fun moveTargets(
    folders: List<LibraryFolder>,
    moving: Set<String> = emptySet(),
    origins: Set<String?> = emptySet(),
): List<MoveTarget> {
    fun blockOf(id: String?): MoveBlock? {
        val path = folderPath(folders, id).map { it.id }
        return when {
            id != null && id in moving -> MoveBlock.Self
            path.dropLast(1).any { it in moving } -> MoveBlock.Inside
            origins.size == 1 && origins.first() == id -> MoveBlock.Here
            else -> null
        }
    }
    val top = MoveTarget(null, 0, blockOf(null))
    return listOf(top) + folderTree(folders).map { MoveTarget(it.folder, it.depth + 1, blockOf(it.folder.id)) }
}

/** The selection key of a folder. A notebook's key is its file name, which always ends in an extension. */
fun folderKey(id: String): String = "f:$id"

fun isFolderKey(key: String): Boolean = key.startsWith("f:")

fun folderIdOf(key: String): String = key.removePrefix("f:")
