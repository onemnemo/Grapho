package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** What a tap, a long press and the menu of one note do. The screen decides, so the grid stays plain. */
class NoteHandlers(
    val actionsFor: (NotebookEntry) -> ItemActions,
    val onTap: (NotebookEntry) -> Unit,
    val onLongPress: (NotebookEntry) -> Unit,
)

/** The picked notes by file name and folders by [folderKey], while the person is choosing. */
class Picks(val selecting: Boolean, val selected: Set<String>) {
    fun has(entry: NotebookEntry) = entry.file.name in selected

    fun hasKey(key: String) = key in selected
}

/** Items before the first group: the header. */
const val HEADER_SLOTS = 1

/** Room under the last row so the selection bar never covers a note. */
private val BottomRoom = 150.dp

/** Vertical gap between rows of covers, a little more than the horizontal one so names breathe. */
private val CoverRowGap = GraphoSpace.space3xl
private val ListRowGap = GraphoSpace.spaceNudge

/**
 * The notes as a grid of covers or a list of rows, in groups when the sort menu says so. [header] is the first
 * item, so it scrolls with the notes, and the groups start at the second slot, see [HEADER_SLOTS]. A [query]
 * marks its first match in each name. [endRoom] keeps the last column clear of the alphabet rail.
 */
@Composable
fun NotesGrid(
    groups: List<EntryGroup>,
    view: LibraryView,
    width: WidthClass,
    gutter: Dp,
    columns: Int,
    state: LazyGridState,
    picks: Picks,
    handlers: NoteHandlers,
    modifier: Modifier = Modifier,
    query: String = "",
    endRoom: Dp = 0.dp,
    header: @Composable () -> Unit,
) {
    val grid = view.mode == ViewMode.Grid
    val sides = if (grid) gutter else gutter - GraphoSpace.spaceMd
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (grid) columns else 1),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = sides, end = sides + endRoom, bottom = BottomRoom),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.space2xl),
        verticalArrangement = Arrangement.spacedBy(if (grid) CoverRowGap else ListRowGap),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(horizontal = if (grid) 0.dp else GraphoSpace.spaceMd)) { header() }
        }
        for (group in groups) {
            if (group.name != GroupName.All) {
                item(key = "group-${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                    GroupHeading(group, Modifier.padding(horizontal = if (grid) 0.dp else GraphoSpace.spaceMd))
                }
            }
            items(group.items, key = { it.file.name }) { entry ->
                if (grid) {
                    CoverTile(
                        entry = entry,
                        selecting = picks.selecting,
                        selected = picks.has(entry),
                        query = query,
                        actions = handlers.actionsFor(entry),
                        onClick = { handlers.onTap(entry) },
                        onLongClick = { handlers.onLongPress(entry) },
                    )
                } else {
                    LibraryRow(
                        entry = entry,
                        width = width,
                        selecting = picks.selecting,
                        selected = picks.has(entry),
                        query = query,
                        actions = handlers.actionsFor(entry),
                        onClick = { handlers.onTap(entry) },
                        onLongClick = { handlers.onLongPress(entry) },
                    )
                }
            }
        }
    }
}

/** The label over a group: a day, a kind or a letter, with how many notes it holds. */
@Composable
fun GroupHeading(group: EntryGroup, modifier: Modifier = Modifier) {
    val c = Grapho.colors
    val label = when (group.name) {
        GroupName.Letter -> group.letter
        GroupName.All -> ""
        GroupName.Today -> stringResource(R.string.library_group_today)
        GroupName.Yesterday -> stringResource(R.string.library_group_yesterday)
        GroupName.ThisWeek -> stringResource(R.string.library_group_this_week)
        GroupName.Earlier -> stringResource(R.string.library_group_earlier)
        GroupName.Notebooks -> stringResource(R.string.library_group_notebooks)
        GroupName.Pdfs -> stringResource(R.string.library_group_pdfs)
        GroupName.Canvases -> stringResource(R.string.library_group_canvases)
    }
    Row(
        modifier.padding(top = GraphoSpace.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(label, style = Grapho.type.title, color = c.textDefault, maxLines = 1)
        Text(group.items.size.toString(), style = Grapho.type.tabular, color = c.textSubtle, maxLines = 1)
    }
}
