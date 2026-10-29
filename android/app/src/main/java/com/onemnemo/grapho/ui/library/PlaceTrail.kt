package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder

/** One step of the path above where you are. */
class Crumb(val label: String, val onClick: () -> Unit)

/** The path in the header of a place other than Home: the places above it, its own name, and Back. */
class HeaderTrail(val crumbs: List<Crumb>, val current: String, val onBack: () -> Unit)

/** The trail for [place], or null at Home, where the header shows the plain title. */
@Composable
fun trailOf(place: LibraryPlace, folders: List<LibraryFolder>, onPlace: (LibraryPlace) -> Unit): HeaderTrail? {
    val library = stringResource(R.string.library_title)
    val all = stringResource(R.string.library_all_folders)
    val starred = stringResource(R.string.library_starred)
    val deleted = stringResource(R.string.library_deleted)
    val parent = placeParent(place, folders) ?: return null
    val home = Crumb(library) { onPlace(LibraryPlace.Home) }
    val back = { onPlace(parent) }
    return when (place) {
        LibraryPlace.Home -> null

        is LibraryPlace.InFolder -> {
            val path = folderPath(folders, place.id)
            val above = path.dropLast(1).map { folder ->
                Crumb(folder.name) { onPlace(LibraryPlace.InFolder(folder.id)) }
            }
            HeaderTrail(listOf(home) + above, path.lastOrNull()?.name.orEmpty(), back)
        }

        LibraryPlace.Starred -> HeaderTrail(listOf(home), starred, back)

        LibraryPlace.AllFolders -> HeaderTrail(listOf(home), all, back)

        LibraryPlace.Deleted -> HeaderTrail(
            listOf(home, Crumb(all) { onPlace(LibraryPlace.AllFolders) }),
            deleted,
            back,
        )
    }
}
