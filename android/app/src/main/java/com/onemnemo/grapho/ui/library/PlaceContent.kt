package com.onemnemo.grapho.ui.library

import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.LibrarySnapshot
import com.onemnemo.grapho.ui.NotebookEntry

/**
 * What the library holds for the place the person is in, worked out once per snapshot. [place] is the place that
 * is shown, which is Home when the asked for folder is gone. [here] are the notebooks the place lists and
 * [subfolders] its folders. [trash] is Recently deleted as rows.
 */
class PlaceContent(
    val place: LibraryPlace,
    val live: List<NotebookEntry>,
    val folders: List<LibraryFolder>,
    val here: List<NotebookEntry>,
    val subfolders: List<LibraryFolder>,
    val trash: List<TrashRow>,
)

fun placeContent(snapshot: LibrarySnapshot?, place: LibraryPlace): PlaceContent {
    val live = snapshot?.entries.orEmpty()
    val folders = snapshot?.folders.orEmpty()
    val shown = resolvePlace(place, folders)
    val subfolders = when (shown) {
        LibraryPlace.Home -> childFolders(folders, null)
        is LibraryPlace.InFolder -> childFolders(folders, shown.id)
        else -> emptyList()
    }
    return PlaceContent(
        place = shown,
        live = live,
        folders = folders,
        here = placeEntries(live, shown),
        subfolders = subfolders,
        trash = if (snapshot == null) emptyList() else groupTrash(snapshot.trashFolders, snapshot.trash),
    )
}
