package com.onemnemo.grapho.ui.library

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoSearchField
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** What the header shows: the title and tools, the search field, or the count of what is picked. */
enum class HeaderMode { Home, Searching, Selecting }

/**
 * What the screen hands the header. Searching and selecting come from the screen's own state. A [trail] makes the
 * title a path with Back, and [place] and [deletedCount] feed the places menu.
 */
class HeaderState(
    val mode: HeaderMode,
    val query: String,
    val selectedCount: Int,
    val allSelected: Boolean,
    val trail: HeaderTrail?,
    val place: LibraryPlace,
    val deletedCount: Int,
)

class HeaderActions(
    val onQueryChange: (String) -> Unit,
    val onSearchOpen: () -> Unit,
    val onSearchClose: () -> Unit,
    val onSearchDone: () -> Unit,
    val onSettings: () -> Unit,
    val onNew: (NewKind) -> Unit,
    val onPlace: (LibraryPlace) -> Unit,
    val onSelectAll: () -> Unit,
    val onDone: () -> Unit,
)

/**
 * Title, search and New. Searching widens the field over the title with a Cancel beside it. Choosing swaps the
 * title for the count and the tools for Select all and Done.
 */
@Composable
fun LibraryHeader(
    state: HeaderState,
    actions: HeaderActions,
    width: WidthClass,
    gutter: Dp,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = state.mode,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = GraphoSpace.space2xl, bottom = GraphoSpace.spaceLg),
        animationSpec = tween(GraphoMotion.motionDurationBaseMs),
        label = "libraryHeader",
    ) { mode ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = HEADER_ROW),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (mode) {
                HeaderMode.Home -> HomeBar(state, actions, width)
                HeaderMode.Searching -> SearchBar(state, actions)
                HeaderMode.Selecting -> SelectBar(state, actions, width)
            }
        }
    }
}

private val HEADER_ROW = GraphoSpace.space3xl + GraphoSpace.space2xl

@Composable
private fun RowScope.HomeBar(state: HeaderState, actions: HeaderActions, width: WidthClass) {
    val c = Grapho.colors
    var newOpen by remember { mutableStateOf(false) }
    var placesOpen by remember { mutableStateOf(false) }
    val style = if (width == WidthClass.Compact) Grapho.type.title else Grapho.type.display
    val trail = state.trail
    if (trail == null) {
        Text(
            stringResource(R.string.library_title),
            style = style,
            color = c.textDefault,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    } else {
        GraphoIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), trail.onBack)
        TrailTitle(trail, style, showPath = width != WidthClass.Compact, modifier = Modifier.weight(1f))
    }
    Box {
        ToolDisc(R.drawable.ic_folder, stringResource(R.string.library_places)) { placesOpen = true }
        PlacesMenu(
            expanded = placesOpen,
            onDismiss = { placesOpen = false },
            place = state.place,
            deletedCount = state.deletedCount,
            onChoose = actions.onPlace,
        )
    }
    ToolDisc(R.drawable.ic_search, stringResource(R.string.library_search), actions.onSearchOpen)
    ToolDisc(R.drawable.ic_settings, stringResource(R.string.settings_title), actions.onSettings)
    Box {
        GraphoButton(
            stringResource(R.string.library_new),
            onClick = { newOpen = true },
            iconRes = R.drawable.ic_plus,
        )
        NewMenu(expanded = newOpen, onDismiss = { newOpen = false }, onChoose = actions.onNew)
    }
}

/** The path of a place: each place above as a quiet button, then its own name. A compact window keeps only the name. */
@Composable
private fun TrailTitle(trail: HeaderTrail, style: TextStyle, showPath: Boolean, modifier: Modifier = Modifier) {
    val c = Grapho.colors
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showPath) {
            for (crumb in trail.crumbs) {
                GraphoButton(crumb.label, onClick = crumb.onClick, kind = ButtonKind.Quiet, size = ButtonSize.Small)
                GraphoIcon(R.drawable.ic_chevron_right, null, c.textSubtle)
            }
        }
        Text(trail.current, style = style, color = c.textDefault, maxLines = 1)
    }
}

@Composable
private fun RowScope.SearchBar(state: HeaderState, actions: HeaderActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    GraphoSearchField(
        value = state.query,
        onValueChange = actions.onQueryChange,
        placeholder = stringResource(R.string.library_search_placeholder),
        modifier = Modifier.weight(1f),
        focusRequester = focus,
        onSearch = actions.onSearchDone,
    )
    GraphoButton(stringResource(R.string.action_cancel), onClick = actions.onSearchClose, kind = ButtonKind.Quiet)
}

@Composable
private fun RowScope.SelectBar(state: HeaderState, actions: HeaderActions, width: WidthClass) {
    val c = Grapho.colors
    GraphoIconButton(R.drawable.ic_close, stringResource(R.string.library_selection_close), actions.onDone)
    Text(
        stringResource(R.string.library_selected_count, state.selectedCount),
        style = if (width == WidthClass.Compact) Grapho.type.title else Grapho.type.display,
        color = c.textDefault,
        maxLines = 1,
        modifier = Modifier.weight(1f),
    )
    val toggle = if (state.allSelected) R.string.library_select_none else R.string.library_select_all
    GraphoButton(stringResource(toggle), onClick = actions.onSelectAll, kind = ButtonKind.Secondary)
    GraphoButton(stringResource(R.string.library_done), onClick = actions.onDone)
}

/** A round tool on the surface fill, like the collapsed search field of the design. */
@Composable
private fun ToolDisc(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.surfaceControlHover
            else -> c.surfaceControl
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "toolFill",
    )
    Box(
        modifier = Modifier
            .size(Grapho.metrics.sizeControlDefault)
            .pressable(interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.pill)
            .semantics { contentDescription = label }
            .clip(GraphoShapes.pill)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        GraphoIcon(icon, null, c.textMuted)
    }
}
