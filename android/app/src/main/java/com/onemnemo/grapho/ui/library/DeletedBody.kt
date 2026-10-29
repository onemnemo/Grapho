package com.onemnemo.grapho.ui.library

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.RestoreMode
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val ThumbWidth = 44.dp
private val ThumbHeight = 52.dp
private val ThumbShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 5.dp, bottomEnd = 5.dp)
private val RowMinHeight = 64.dp
private val BottomRoom = 48.dp

/** What the person is being asked to remove for good: one row, or everything in Recently deleted. */
private sealed interface ForeverAsk {
    data class One(val row: TrashRow) : ForeverAsk
    data object All : ForeverAsk
}

/**
 * Recently deleted: what was deleted in the last [TRASH_DAYS] days, each row with Restore and Delete now, and
 * Restore all and Delete all over the list. Removing for good always asks first.
 */
@Composable
fun DeletedBody(rows: List<TrashRow>, actions: LibraryActions, gutter: Dp) {
    val context = LocalContext.current
    var asking by remember { mutableStateOf<ForeverAsk?>(null) }
    var choosing by remember { mutableStateOf<NotebookEntry?>(null) }
    val now = remember(rows) { System.currentTimeMillis() }
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LibraryEmpty(EmptyKind.Deleted, onPrimary = null, onSecondary = null)
        }
    } else {
        DeletedList(
            rows = rows,
            now = now,
            gutter = gutter,
            onRestoreAll = { actions.restoreAll(rows) },
            onDeleteAll = { asking = ForeverAsk.All },
            onRestore = { row ->
                when (row) {
                    is TrashRow.Folder -> actions.restoreFolder(row.folder)
                    is TrashRow.Item -> actions.restoreEntry(row.entry) { choosing = it }
                }
            },
            onDelete = { asking = ForeverAsk.One(it) },
        )
    }

    asking?.let { ask ->
        val title = when (ask) {
            ForeverAsk.All -> context.getString(R.string.library_delete_all_title)
            is ForeverAsk.One -> context.getString(R.string.library_delete_forever_title, rowName(ask.row, context))
        }
        DeleteForeverDialog(
            title = title,
            count = if (ask is ForeverAsk.One) ask.row.count else trashCount(rows),
            onDismiss = { asking = null },
            onConfirm = {
                asking = null
                if (ask is ForeverAsk.One) actions.deleteForever(listOf(ask.row)) else actions.deleteAll()
            },
        )
    }
    choosing?.let { entry ->
        RestoreChoiceDialog(
            entry = entry,
            onDismiss = { choosing = null },
            onWithFolder = {
                choosing = null
                actions.restore(entry, RestoreMode.WithFolder)
            },
            onToLibrary = {
                choosing = null
                actions.restore(entry, RestoreMode.ToLibrary)
            },
        )
    }
}

@Composable
private fun DeletedList(
    rows: List<TrashRow>,
    now: Long,
    gutter: Dp,
    onRestoreAll: () -> Unit,
    onDeleteAll: () -> Unit,
    onRestore: (TrashRow) -> Unit,
    onDelete: (TrashRow) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = BottomRoom),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        item(key = "head") {
            Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm)) {
                Text(
                    stringResource(R.string.library_deleted_hint),
                    style = Grapho.type.body,
                    color = Grapho.colors.textMuted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm)) {
                    GraphoButton(
                        stringResource(R.string.library_restore_all),
                        onClick = onRestoreAll,
                        kind = ButtonKind.Secondary,
                        iconRes = R.drawable.ic_restore,
                    )
                    GraphoButton(
                        stringResource(R.string.library_delete_all),
                        onClick = onDeleteAll,
                        kind = ButtonKind.Danger,
                        iconRes = R.drawable.ic_trash,
                    )
                }
            }
        }
        items(rows, key = { it.key }) { row ->
            DeletedRow(row, now, onRestore = { onRestore(row) }, onDelete = { onDelete(row) })
        }
    }
}

private fun rowName(row: TrashRow, context: Context): String = when (row) {
    is TrashRow.Folder -> row.folder.name
    is TrashRow.Item -> row.entry.nameIn(context)
}

@Composable
private fun DeletedRow(row: TrashRow, now: Long, onRestore: () -> Unit, onDelete: () -> Unit) {
    val c = Grapho.colors
    val context = LocalContext.current
    val name = rowName(row, context)
    val days = context.resources.getQuantityString(
        R.plurals.library_days_left,
        daysLeft(row.deletedAt, now),
        daysLeft(row.deletedAt, now),
    )
    val inside = (row as? TrashRow.Folder)?.inside?.size ?: 0
    val caption = if (inside > 0) {
        stringResource(
            R.string.library_meta,
            context.resources.getQuantityString(R.plurals.library_trash_inside, inside, inside),
            days,
        )
    } else {
        days
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = RowMinHeight),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(ThumbWidth, ThumbHeight).clip(ThumbShape), contentAlignment = Alignment.Center) {
            when (row) {
                is TrashRow.Folder -> FolderArt(folderColour(row.folder.id), Modifier.width(ThumbWidth))

                is TrashRow.Item -> when (row.entry.kind) {
                    EntryKind.Notebook -> CoverArt(CoverPalette.resolve(row.entry.coverColor), null)
                    EntryKind.Pdf -> PdfArt()
                    EntryKind.Canvas -> CanvasArt(row.entry.background)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = Grapho.type.label, color = c.textDefault, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                caption,
                style = Grapho.type.caption,
                color = c.textSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GraphoButton(
            stringResource(R.string.library_restore),
            onClick = onRestore,
            kind = ButtonKind.Secondary,
            size = ButtonSize.Small,
        )
        GraphoIconButton(R.drawable.ic_trash, stringResource(R.string.library_delete_now), onDelete)
    }
}
