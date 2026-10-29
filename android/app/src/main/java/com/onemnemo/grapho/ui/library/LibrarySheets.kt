package com.onemnemo.grapho.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import kotlinx.coroutines.launch

/**
 * Which sheet is open: a new notebook, canvas or folder, which start in [folderId] or [parentId] (null for the
 * top level), or the look of a note that exists.
 */
sealed interface SheetRequest {
    data class NewNotebook(val folderId: String?) : SheetRequest
    data class NewCanvas(val folderId: String?) : SheetRequest
    data class NewFolder(val parentId: String?) : SheetRequest
    data class Edit(val entry: NotebookEntry) : SheetRequest
}

/**
 * Hosts the new notebook, canvas and folder sheets and the edit sheet, and what they do when saved. Nothing
 * shows while [request] is null. [folders] are the places the Location row offers.
 */
@Composable
fun LibrarySheets(
    request: SheetRequest?,
    folders: List<LibraryFolder>,
    store: NotebookStore,
    actions: LibraryActions,
    notices: Notices,
    onOpen: (File) -> Unit,
    reload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun createFailed(e: Exception) =
        notices.show(e.userMessage(context, R.string.library_create_failed), isError = true)
    fun saveFailed(e: Exception) = notices.show(e.userMessage(context, R.string.library_save_failed), isError = true)

    val targets = remember(folders) { moveTargets(folders) }

    key(request) {
        when (request) {
            null -> Unit

            is SheetRequest.NewNotebook -> NotebookSheet(
                mode = SheetMode.New,
                initial = NotebookDraft.fresh(request.folderId),
                onDismiss = onDismiss,
                targets = targets,
                onSave = { draft ->
                    onDismiss()
                    scope.launch {
                        try {
                            onOpen(store.create(draft.toSpec(), draft.folderId))
                        } catch (e: Exception) {
                            createFailed(e)
                        }
                    }
                },
            )

            is SheetRequest.NewCanvas -> CanvasSheet(
                mode = SheetMode.New,
                initial = CanvasDraft.fresh(request.folderId),
                onDismiss = onDismiss,
                targets = targets,
                onSave = { draft ->
                    onDismiss()
                    scope.launch {
                        try {
                            onOpen(store.create(draft.toSpec(), draft.folderId))
                        } catch (e: Exception) {
                            createFailed(e)
                        }
                    }
                },
            )

            is SheetRequest.NewFolder -> FolderSheet(
                targets = targets,
                parentId = request.parentId,
                onDismiss = onDismiss,
                onCreate = { name, parentId ->
                    onDismiss()
                    actions.createFolder(parentId, name)
                },
            )

            is SheetRequest.Edit -> {
                val entry = request.entry
                val saveLook = { title: String, paper: PageBackground, cover: Int? ->
                    onDismiss()
                    scope.launch {
                        try {
                            store.editLook(entry.file, title, paper)
                            if (cover != null) store.setCover(entry, cover)
                            reload()
                        } catch (e: Exception) {
                            saveFailed(e)
                        }
                    }
                    Unit
                }
                if (entry.infinite) {
                    CanvasSheet(
                        mode = SheetMode.Edit,
                        initial = CanvasDraft.of(entry.title, entry.background),
                        onDismiss = onDismiss,
                        onSave = { saveLook(it.name, it.paper(), null) },
                    )
                } else {
                    val start = NotebookDraft(
                        name = entry.title,
                        coverColor = CoverPalette.resolve(entry.coverColor).base.toArgb(),
                        pattern = entry.background.pattern,
                        paper = 0,
                        landscape = false,
                        paperColor = entry.background.color,
                        spacingMm = entry.background.spacingMm,
                    )
                    NotebookSheet(
                        mode = SheetMode.Edit,
                        initial = start,
                        onDismiss = onDismiss,
                        onSave = { draft ->
                            val cover = draft.coverColor.takeIf { it != start.coverColor }
                            saveLook(draft.name, draft.background(), cover)
                        },
                    )
                }
            }
        }
    }
}
