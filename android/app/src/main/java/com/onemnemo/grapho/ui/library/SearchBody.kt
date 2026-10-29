package com.onemnemo.grapho.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Rows of recent searches stay narrow, like a list in a popover, so they never stretch across a tablet. */
private val RecentsMaxWidth = GraphoSpace.sizeSheetWidthDefault

/**
 * What shows while searching: recent searches before anything is typed, then the matching notes as covers
 * with the match marked in their names. Search reads titles only, because no text of a note is indexed yet.
 */
@Composable
fun SearchBody(
    query: String,
    results: List<NotebookEntry>,
    recent: List<String>,
    onPickRecent: (String) -> Unit,
    onClear: () -> Unit,
    view: LibraryView,
    width: WidthClass,
    gutter: Dp,
    columns: Int,
    state: LazyGridState,
    picks: Picks,
    handlers: NoteHandlers,
) {
    if (query.isBlank()) {
        Recents(recent, onPickRecent, gutter)
        return
    }
    if (results.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LibraryEmpty(EmptyKind.NoMatch, onPrimary = null, onSecondary = onClear)
        }
        return
    }
    NotesGrid(
        groups = listOf(EntryGroup(GroupName.All, "", results)),
        view = view.copy(mode = ViewMode.Grid),
        width = width,
        gutter = gutter,
        columns = columns,
        state = state,
        picks = picks,
        handlers = handlers,
        query = query.trim(),
    ) {
        val count = results.size
        Text(
            LocalContext.current.resources.getQuantityString(R.plurals.library_results, count, count),
            style = Grapho.type.heading,
            color = Grapho.colors.textMuted,
            modifier = Modifier.padding(vertical = GraphoSpace.spaceSm),
        )
    }
}

@Composable
private fun Recents(recent: List<String>, onPick: (String) -> Unit, gutter: Dp) {
    val c = Grapho.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = gutter),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        Text(
            stringResource(R.string.library_recent_searches),
            style = Grapho.type.heading,
            color = c.textMuted,
            modifier = Modifier.padding(vertical = GraphoSpace.spaceMd),
        )
        if (recent.isEmpty()) {
            Text(stringResource(R.string.library_search_hint), style = Grapho.type.body, color = c.textSubtle)
        }
        for (entry in recent) RecentRow(entry) { onPick(entry) }
    }
}

@Composable
private fun RecentRow(text: String, onClick: () -> Unit) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "recentFill",
    )
    Row(
        Modifier
            .widthIn(max = RecentsMaxWidth)
            .fillMaxWidth()
            .heightIn(min = Grapho.metrics.sizeRowList)
            .pressable(scaleOnPress = false, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.item, inset = true)
            .clip(GraphoShapes.item)
            .background(fill)
            .padding(horizontal = GraphoSpace.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIcon(R.drawable.ic_clock, null, c.textSubtle)
        Text(text, style = Grapho.type.label, color = c.textDefault, maxLines = 1)
    }
}
