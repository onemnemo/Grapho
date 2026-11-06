package com.onemnemo.grapho.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.LoadingScreen
import com.onemnemo.grapho.ui.components.NoticeHost
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.SplashPhase
import com.onemnemo.grapho.ui.components.rememberSplashPhase
import com.onemnemo.grapho.ui.library.LibraryPlace
import com.onemnemo.grapho.ui.library.LibraryScreen
import com.onemnemo.grapho.ui.note.NoteScreen
import com.onemnemo.grapho.ui.settings.AppSettings
import com.onemnemo.grapho.ui.settings.SettingsOverlay
import com.onemnemo.grapho.ui.theme.Grapho
import java.io.File

/**
 * Library and note, switched by plain state, with Settings over either. System back from a note returns to the
 * library, and from Settings returns to whichever it opened over.
 */
@Composable
fun GraphoApp(store: NotebookStore, settings: AppSettings) {
    var openFile by remember { mutableStateOf<File?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Bumped when Settings changes what the library keeps, so it reads its view again.
    var libraryRevision by remember { mutableIntStateOf(0) }
    // Set when Settings asks the open note to start customising the wheel, cleared once it has.
    var wheelRequested by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf<LibrarySnapshot?>(null) }
    // Where in the library the person is, kept here so a notebook opened from a folder returns to that folder.
    var place by remember { mutableStateOf<LibraryPlace>(LibraryPlace.Home) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var purgeReported by remember { mutableStateOf(false) }
    val notices = remember { Notices() }
    val context = LocalContext.current
    val appScope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        try {
            library = store.snapshot()
        } catch (e: Exception) {
            notices.show(context.getString(R.string.library_read_failed), isError = true)
            library = LibrarySnapshot(emptyList(), emptyList(), emptyList(), emptyList())
        }
        // The library purges what has been in Trash too long when it opens, and says so once.
        if (!purgeReported) {
            purgeReported = true
            val purged = try {
                store.takePurged()
            } catch (e: Exception) {
                0
            }
            if (purged > 0) {
                notices.show(context.resources.getQuantityString(R.plurals.library_purged, purged, purged))
            }
        }
    }

    // Recent sorts by when a notebook was opened. A failure to record it must not stop the note from opening.
    LaunchedEffect(openFile) {
        openFile?.let { store.markOpened(it) }
    }

    val startPhase = rememberSplashPhase(library == null)

    Box(Modifier.fillMaxSize().background(Grapho.colors.surfaceBase)) {
        val file = openFile
        if (file == null) {
            LibraryScreen(
                library = library,
                place = place,
                onPlace = { place = it },
                store = store,
                notices = notices,
                appScope = appScope,
                onOpen = { openFile = it },
                reload = { reloadKey++ },
                viewRevision = libraryRevision,
                onSettings = { settingsOpen = true },
            )
            if (startPhase != SplashPhase.Hidden) LoadingScreen(cheering = startPhase == SplashPhase.Cheering)
        } else {
            val entry = library?.entries?.firstOrNull { it.file == file }
            NoteScreen(
                file = file,
                openingTitle = entry?.title,
                openingInfinite = entry?.infinite,
                notices = notices,
                settings = settings,
                onSettings = { settingsOpen = true },
                customiseWheel = wheelRequested,
                onWheelCustomised = { wheelRequested = false },
                onBack = { openFile = null },
                // The note is saved by now, so the list can pick up its new date.
                onClosed = { error ->
                    reloadKey++
                    if (error != null) notices.show(context.getString(error), isError = true)
                },
            )
        }
        if (settingsOpen) {
            SettingsOverlay(
                settings = settings,
                onClose = { settingsOpen = false },
                onLibraryChanged = { libraryRevision++ },
                onCustomiseWheel = if (file == null) {
                    null
                } else {
                    {
                        settingsOpen = false
                        wheelRequested = true
                    }
                },
            )
        }
        NoticeHost(
            notices,
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
