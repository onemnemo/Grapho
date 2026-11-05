package com.onemnemo.grapho.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.LibrarySnapshot
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.components.BusyOverlay
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.RenameDialog
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val RailRoom = 32.dp

/**
 * The library: header, the place that is open (Home, a folder, Starred, All folders or Recently deleted), search,
 * selection and the sheets behind New and Edit. The place lives in the caller, so opening a notebook and coming
 * back lands in the same folder. [appScope] outlives this screen, so an Undo still works after a notebook opens.
 */
@Composable
fun LibraryScreen(
    library: LibrarySnapshot?,
    place: LibraryPlace,
    onPlace: (LibraryPlace) -> Unit,
    store: NotebookStore,
    notices: Notices,
    appScope: CoroutineScope,
    onOpen: (File) -> Unit,
    reload: () -> Unit,
    viewRevision: Int,
    onSettings: () -> Unit,
) {
    val c = Grapho.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { LibraryPrefs(context) }
    // Settings can change the stored view, so it is read again when the revision moves.
    var view by remember(viewRevision) { mutableStateOf(prefs.loadView()) }
    var recent by remember { mutableStateOf(prefs.loadRecent()) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }
    var renameTarget by remember { mutableStateOf<NotebookEntry?>(null) }
    var renameFolder by remember { mutableStateOf<LibraryFolder?>(null) }
    val libraryName = stringResource(R.string.library_title)
    val reloadNow by rememberUpdatedState(reload)
    val actions = remember(store, notices, appScope) {
        LibraryActions(context, store, notices, appScope) { reloadNow() }
    }
    val content = remember(library, place) { placeContent(library, place) }
    val current = content.place
    val live = content.live
    val folders = content.folders
    val intoFolder = (current as? LibraryPlace.InFolder)?.id
    val files = rememberLibraryFiles(store, notices, onOpen, reload, intoFolder)
    val homeState = rememberLazyGridState()
    val searchState = rememberLazyGridState()

    val sorted = remember(content.here, view.sortKey, view.sortDir) {
        sortEntries(content.here, view.sortKey, view.sortDir)
    }
    val everyone = remember(live, view.sortKey, view.sortDir) { sortEntries(live, view.sortKey, view.sortDir) }
    val results = remember(everyone, query) { searchEntries(everyone, query) }
    val groups = remember(sorted, view.group) { groupEntries(sorted, view.group) }
    val shown = if (searching && query.isNotBlank()) results else sorted
    val everyKey = when {
        searching && query.isNotBlank() -> results.map { it.file.name }
        current == LibraryPlace.AllFolders -> folders.map { folderKey(it.id) }
        else -> content.subfolders.map { folderKey(it.id) } + sorted.map { it.file.name }
    }
    val pickedItems = remember(live, selected) { live.filter { it.file.name in selected } }
    val pickedFolders = remember(folders, selected) { folders.filter { folderKey(it.id) in selected } }
    val deletedCount = remember(content.trash) { trashCount(content.trash) }

    // A place that is gone after a reload, such as a folder deleted from inside, falls back to Home.
    LaunchedEffect(place, current) { if (current != place) onPlace(current) }

    // A note or folder that is gone after a reload cannot stay picked.
    LaunchedEffect(library) {
        val present = live.map { it.file.name }.toSet() + folders.map { folderKey(it.id) }
        selected = selected.intersect(present)
    }

    // Each place starts at its top.
    LaunchedEffect(current) { homeState.scrollToItem(0) }

    fun changeView(next: LibraryView) {
        view = next
        prefs.saveView(next)
    }

    fun rememberSearch() {
        recent = pushRecent(recent, query)
        prefs.saveRecent(recent)
    }

    fun endSelection() {
        selecting = false
        selected = emptySet()
    }

    fun closeSearch() {
        searching = false
        query = ""
    }

    fun go(target: LibraryPlace) {
        endSelection()
        closeSearch()
        onPlace(target)
    }

    fun pick(entry: NotebookEntry) {
        selecting = true
        selected = selected + entry.file.name
    }

    // A tap while choosing toggles a folder, and a long press starts choosing with it.
    fun touchFolder(folder: LibraryFolder) {
        val key = folderKey(folder.id)
        selected = if (selecting) toggled(selected, key) else selected + key
        selecting = true
    }

    fun placeName(id: String?): String = folders.firstOrNull { it.id == id }?.name ?: libraryName

    val parent = placeParent(current, folders)
    BackHandler(enabled = selecting || searching || parent != null) {
        if (selecting) {
            endSelection()
        } else if (searching) {
            closeSearch()
        } else {
            parent?.let { go(it) }
        }
    }

    val handlers = NoteHandlers(
        actionsFor = { entry ->
            ItemActions(
                onOpen = { onOpen(entry.file) },
                onRename = { renameTarget = entry },
                onEditLook = { sheet = SheetRequest.Edit(entry) },
                onSelect = { pick(entry) },
                onExport = { files.export(entry) },
                onExportPdf = { files.exportPdf(entry) },
                onDelete = { actions.trash(listOf(entry), emptyList()) },
                starred = entry.starred,
                onToggleStar = { actions.setStarred(listOf(entry), !entry.starred) },
                moveTargets = { moveTargets(folders, emptySet(), setOf(entry.folderId)) },
                onMoveTo = { id -> actions.move(listOf(entry), emptyList(), id, placeName(id)) },
            )
        },
        onTap = { entry ->
            if (selecting) {
                selected = toggled(selected, entry.file.name)
            } else {
                if (searching) rememberSearch()
                onOpen(entry.file)
            }
        },
        onLongPress = { pick(it) },
    )
    val folderActions = { folder: LibraryFolder ->
        FolderActions(
            onOpen = { go(LibraryPlace.InFolder(folder.id)) },
            onRename = { renameFolder = folder },
            onSelect = { touchFolder(folder) },
            onDelete = { actions.trash(emptyList(), listOf(folder)) },
            moveTargets = { moveTargets(folders, setOf(folder.id), setOf(folder.parentId)) },
            onMoveTo = { id -> actions.move(emptyList(), listOf(folder), id, placeName(id)) },
        )
    }
    val header = HeaderActions(
        onQueryChange = { query = it },
        onSearchOpen = {
            // Search looks through every notebook, which Recently deleted and All folders do not list.
            if (current == LibraryPlace.Deleted || current == LibraryPlace.AllFolders) onPlace(LibraryPlace.Home)
            searching = true
            endSelection()
        },
        onSearchClose = ::closeSearch,
        onSearchDone = ::rememberSearch,
        onSettings = onSettings,
        onNew = {
            when (it) {
                NewKind.Notebook -> sheet = SheetRequest.NewNotebook(intoFolder)
                NewKind.Canvas -> sheet = SheetRequest.NewCanvas(intoFolder)
                NewKind.Folder -> sheet = SheetRequest.NewFolder(intoFolder)
                NewKind.Pdf -> files.importPdf()
                NewKind.File -> files.importNotebook()
            }
        },
        onPlace = ::go,
        onSelectAll = { selected = allOrNone(selected, everyKey) },
        onDone = ::endSelection,
    )
    val trail = trailOf(current, folders, ::go)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.surfaceBase)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val width = widthClass(maxWidth.value)
        val gutter = when (width) {
            WidthClass.Compact -> GraphoSpace.layoutGutterCompact
            WidthClass.Medium -> GraphoSpace.layoutGutterMedium
            WidthClass.Expanded -> GraphoSpace.layoutGutterExpanded
        }
        val columns = gridColumns((maxWidth - gutter * 2).value, GraphoSpace.space2xl.value, view.coverSize)
        val active = if (searching) searchState else homeState
        val scrolled by remember(active) {
            derivedStateOf { active.firstVisibleItemIndex > 0 || active.firstVisibleItemScrollOffset > 2 }
        }
        val picks = Picks(selecting, selected)
        val mode = when {
            selecting -> HeaderMode.Selecting
            searching -> HeaderMode.Searching
            else -> HeaderMode.Home
        }
        val letters = remember(groups) {
            groups.filter { it.name == GroupName.Letter }.map { it.letter }.toSet()
        }
        val starts = remember(groups) { groupStarts(groups, HEADER_SLOTS) }
        val activeLetter by remember(starts) {
            derivedStateOf {
                activeGroupId(starts, homeState.firstVisibleItemIndex)?.removePrefix(GroupName.Letter.name)
            }
        }
        val showRail = view.group == GroupBy.Letter && !searching && letters.isNotEmpty()

        Column(Modifier.fillMaxSize()) {
            LibraryHeader(
                state = HeaderState(
                    mode = mode,
                    query = query,
                    selectedCount = selected.size,
                    allSelected = everyKey.isNotEmpty() && selected.size >= everyKey.size,
                    trail = trail,
                    place = current,
                    deletedCount = deletedCount,
                ),
                actions = header,
                width = width,
                gutter = gutter,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(GraphoSpace.borderWidthHairline)
                    .background(if (scrolled) c.borderSubtle else Color.Transparent),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    library == null -> Unit

                    current == LibraryPlace.Deleted -> DeletedBody(content.trash, actions, gutter)

                    current == LibraryPlace.AllFolders -> AllFoldersBody(
                        folders = folders,
                        entries = live,
                        deletedCount = deletedCount,
                        picks = picks,
                        gutter = gutter,
                        actionsFor = folderActions,
                        onOpen = { go(LibraryPlace.InFolder(it.id)) },
                        onPick = ::touchFolder,
                        onNewFolder = { sheet = SheetRequest.NewFolder(null) },
                        onDeleted = { go(LibraryPlace.Deleted) },
                    )

                    current == LibraryPlace.Home && live.isEmpty() && folders.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LibraryEmpty(
                                EmptyKind.Library,
                                onPrimary = { sheet = SheetRequest.NewNotebook(null) },
                                onSecondary = files.importPdf,
                            )
                        }

                    searching -> SearchBody(
                        query = query,
                        results = results,
                        recent = recent,
                        onPickRecent = { query = it },
                        onClear = { query = "" },
                        view = view,
                        width = width,
                        gutter = gutter,
                        columns = columns,
                        state = searchState,
                        picks = picks,
                        handlers = handlers,
                    )

                    intoFolder != null && content.here.isEmpty() && content.subfolders.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LibraryEmpty(
                                EmptyKind.Folder,
                                onPrimary = { sheet = SheetRequest.NewNotebook(intoFolder) },
                                onSecondary = null,
                            )
                        }

                    current == LibraryPlace.Starred && content.here.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LibraryEmpty(EmptyKind.Starred, onPrimary = null, onSecondary = null)
                        }

                    else -> {
                        NotesGrid(
                            groups = groups,
                            view = view,
                            width = width,
                            gutter = gutter,
                            columns = columns,
                            state = homeState,
                            picks = picks,
                            handlers = handlers,
                            endRoom = if (showRail) (RailRoom - gutter).coerceAtLeast(0.dp) else 0.dp,
                        ) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
                            ) {
                                if (content.subfolders.isNotEmpty()) {
                                    FolderStrip(
                                        folders = content.subfolders,
                                        picks = picks,
                                        countsOf = { folderCounts(it.id, live, folders) },
                                        actionsFor = folderActions,
                                        onOpen = { go(LibraryPlace.InFolder(it.id)) },
                                        onPick = ::touchFolder,
                                        onSeeAll = if (current == LibraryPlace.Home) {
                                            { go(LibraryPlace.AllFolders) }
                                        } else {
                                            null
                                        },
                                    )
                                }
                                if (sorted.isNotEmpty()) {
                                    SectionHeader(
                                        title = stringResource(
                                            if (current == LibraryPlace.Starred) {
                                                R.string.library_starred
                                            } else {
                                                R.string.library_notes
                                            },
                                        ),
                                        count = sorted.size,
                                        view = view,
                                        onView = ::changeView,
                                        compact = width == WidthClass.Compact,
                                    )
                                }
                            }
                        }
                        if (showRail) {
                            AlphabetRail(
                                available = letters,
                                active = activeLetter,
                                onSelect = { letter ->
                                    val at = starts["${GroupName.Letter.name}$letter"] ?: return@AlphabetRail
                                    scope.launch { homeState.scrollToItem(at) }
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(
                                        end = GraphoSpace.spaceXs,
                                        top = GraphoSpace.spaceLg,
                                        bottom = GraphoSpace.spaceLg,
                                    ),
                            )
                        }
                    }
                }
            }
        }
        val allStarred = pickedItems.isNotEmpty() && pickedItems.all { it.starred }
        SelectionBar(
            visible = selecting,
            count = selected.size,
            canExport = pickedItems.isNotEmpty() || pickedFolders.isNotEmpty(),
            canStar = pickedItems.isNotEmpty(),
            allStarred = allStarred,
            moveTargets = {
                moveTargets(
                    folders,
                    pickedFolders.map { it.id }.toSet(),
                    pickedItems.map { it.folderId }.toSet() + pickedFolders.map { it.parentId }.toSet(),
                )
            },
            onExport = {
                val single = pickedItems.singleOrNull()
                if (single != null && pickedFolders.isEmpty()) {
                    files.export(single)
                } else {
                    files.exportPackage(pickedFolders, pickedItems)
                }
            },
            onMoveTo = { id ->
                actions.move(pickedItems, pickedFolders, id, placeName(id))
                endSelection()
            },
            onStar = { actions.setStarred(pickedItems, !allStarred) },
            onDelete = { actions.trash(pickedItems, pickedFolders, onDone = ::endSelection) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = GraphoSpace.space2xl),
        )
        files.busy?.let { BusyOverlay(stringResource(it)) }
    }

    LibrarySheets(
        request = sheet,
        folders = folders,
        store = store,
        actions = actions,
        notices = notices,
        onOpen = onOpen,
        reload = reload,
        onDismiss = { sheet = null },
    )

    renameTarget?.let { entry ->
        RenameDialog(
            initial = entry.title,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                scope.launch {
                    try {
                        store.rename(entry.file, name)
                        reload()
                    } catch (e: Exception) {
                        notices.show(e.userMessage(context, R.string.library_rename_failed), isError = true)
                    }
                }
            },
        )
    }

    renameFolder?.let { folder ->
        RenameDialog(
            initial = folder.name,
            onDismiss = { renameFolder = null },
            onConfirm = { name ->
                renameFolder = null
                actions.renameFolder(folder.id, name)
            },
        )
    }
}
