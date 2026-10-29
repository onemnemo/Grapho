package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuItem

/**
 * The places of the library behind the folder button of the header: Home, Starred, All folders and Recently
 * deleted. The current place carries the check, and Recently deleted shows how much is in it.
 */
@Composable
fun PlacesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    place: LibraryPlace,
    deletedCount: Int,
    onChoose: (LibraryPlace) -> Unit,
) {
    fun go(target: LibraryPlace): () -> Unit = {
        onDismiss()
        onChoose(target)
    }
    GraphoMenu(expanded = expanded, onDismiss = onDismiss) {
        GraphoMenuItem(
            stringResource(R.string.library_place_home),
            icon = R.drawable.ic_notebook,
            checked = place == LibraryPlace.Home,
            onClick = go(LibraryPlace.Home),
        )
        GraphoMenuItem(
            stringResource(R.string.library_starred),
            icon = R.drawable.ic_star,
            checked = place == LibraryPlace.Starred,
            onClick = go(LibraryPlace.Starred),
        )
        GraphoMenuItem(
            stringResource(R.string.library_all_folders),
            icon = R.drawable.ic_folder,
            checked = place == LibraryPlace.AllFolders,
            onClick = go(LibraryPlace.AllFolders),
        )
        GraphoMenuItem(
            stringResource(R.string.library_deleted),
            icon = R.drawable.ic_trash,
            checked = place == LibraryPlace.Deleted,
            value = if (deletedCount > 0) deletedCount.toString() else null,
            onClick = go(LibraryPlace.Deleted),
        )
    }
}
