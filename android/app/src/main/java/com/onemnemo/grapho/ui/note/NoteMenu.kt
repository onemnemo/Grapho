package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NotebookInfo
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoMenuLabel
import com.onemnemo.grapho.ui.components.GraphoMenuQuick
import com.onemnemo.grapho.ui.components.GraphoMenuQuickRow
import com.onemnemo.grapho.ui.components.TooltipSide

private val PATTERN_NAMES = listOf(
    R.string.paper_blank,
    R.string.paper_lined,
    R.string.paper_grid,
    R.string.paper_dotted,
)

/**
 * The note bar's More button and its menu, with a quick row of icon actions on top, then the groups: this note,
 * tools and view. Page background opens its own popover from here. On a compact bar the insert choices are
 * folded in as one row, which opens the insert menu under this button ([insertOpen] and [onInsertOpen]). A
 * vertical bar has no room for its own Layers button, so [layersInMenu] adds it under this note ([layersOpen]
 * and [onLayers]). [tip] is the side the button's tip opens on, and [onOpenChange] hears when the menu or its
 * page background popover opens and closes. Every action closes the menu before it runs.
 */
@Composable
internal fun NoteMenu(
    info: NotebookInfo?,
    toggles: NoteToggles,
    actions: NoteActions,
    insertInMenu: Boolean,
    insertOpen: Boolean,
    onInsertOpen: (Boolean) -> Unit,
    layersInMenu: Boolean = false,
    layersOpen: Boolean = false,
    onLayers: () -> Unit = {},
    tip: TooltipSide? = TooltipSide.Below,
    onOpenChange: (Boolean) -> Unit = {},
    insertImages: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(false) }
    LaunchedEffect(open, background) { onOpenChange(open || background) }
    // A bar that leaves the screen takes its menu with it.
    DisposableEffect(Unit) { onDispose { onOpenChange(false) } }
    fun run(action: () -> Unit) {
        open = false
        action()
    }
    val pattern = info?.background?.pattern ?: 0
    val paper = info?.background?.color ?: PaperColors.WHITE.argb

    Box {
        GraphoIconButton(
            R.drawable.ic_more_horizontal,
            stringResource(R.string.note_more),
            onClick = { open = true },
            selected = open,
            tooltipSide = tip,
        )
        GraphoMenu(expanded = open, onDismiss = { open = false }) {
            GraphoMenuQuickRow {
                GraphoMenuQuick(
                    stringResource(R.string.note_share),
                    R.drawable.ic_share,
                    onClick = { run(actions.onShare) },
                )
                GraphoMenuQuick(
                    stringResource(R.string.note_export),
                    R.drawable.ic_file_up,
                    onClick = { run(actions.onExport) },
                )
            }
            GraphoMenuLabel(stringResource(R.string.note_menu_this_note))
            GraphoMenuItem(
                stringResource(R.string.note_page_background),
                icon = R.drawable.ic_page,
                value = stringResource(PATTERN_NAMES[pattern.coerceIn(0, PATTERN_NAMES.lastIndex)]),
                onClick = { run { background = true } },
            )
            GraphoMenuItem(
                stringResource(R.string.note_rename_note),
                icon = R.drawable.ic_rename,
                onClick = { run(actions.onRename) },
            )
            if (layersInMenu) {
                GraphoMenuItem(
                    stringResource(R.string.layers_title),
                    icon = R.drawable.ic_layers,
                    checked = layersOpen,
                    onClick = { run(onLayers) },
                )
            }
            if (insertInMenu) {
                GraphoMenuItem(
                    stringResource(R.string.note_bar_insert),
                    icon = R.drawable.ic_plus,
                    onClick = { run { onInsertOpen(true) } },
                )
            }
            GraphoMenuLabel(stringResource(R.string.note_menu_tools))
            GraphoMenuItem(
                stringResource(R.string.note_ruler),
                icon = R.drawable.ic_ruler,
                toggle = toggles.rulerOn,
                onClick = { run { toggles.onRuler(!toggles.rulerOn) } },
            )
            GraphoMenuItem(
                stringResource(R.string.note_protractor),
                icon = R.drawable.ic_protractor,
                toggle = toggles.protractorOn,
                onClick = { run { toggles.onProtractor(!toggles.protractorOn) } },
            )
            GraphoMenuItem(
                stringResource(R.string.note_draw_with_finger),
                icon = R.drawable.ic_finger,
                toggle = toggles.fingerDraws,
                onClick = { run { toggles.onFingerDraws(!toggles.fingerDraws) } },
            )
            GraphoMenuItem(
                stringResource(R.string.note_customise_wheel),
                icon = R.drawable.ic_wheel,
                onClick = { run(actions.onCustomiseWheel) },
            )
            GraphoMenuLabel(stringResource(R.string.note_menu_view))
            GraphoMenuItem(
                stringResource(R.string.note_read_mode),
                icon = R.drawable.ic_read_mode,
                onClick = { run(actions.onReadMode) },
            )
            GraphoMenuItem(
                stringResource(if (toggles.fullScreen) R.string.note_exit_full_screen else R.string.note_full_screen),
                icon = if (toggles.fullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                onClick = { run { toggles.onFullScreen(!toggles.fullScreen) } },
            )
            GraphoMenuDivider()
            GraphoMenuItem(
                stringResource(R.string.settings_title),
                icon = R.drawable.ic_settings,
                onClick = { run(actions.onSettings) },
            )
        }
        PageBackgroundPopover(
            expanded = background,
            onDismiss = { background = false },
            pattern = pattern,
            paper = paper,
            onPattern = actions.onPaper,
            onPaper = actions.onPaperColor,
            onCustom = actions.onCustomPaperColor,
        )
        if (insertInMenu) {
            InsertMenu(
                expanded = insertOpen,
                onDismiss = { onInsertOpen(false) },
                infinite = info?.infinite == true,
                actions = actions,
                images = insertImages,
            )
        }
    }
}
