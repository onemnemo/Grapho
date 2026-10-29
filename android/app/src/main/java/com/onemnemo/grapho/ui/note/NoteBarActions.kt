package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NotebookInfo
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.TooltipSide

/**
 * What sits on the right of the note bar, in order: layers, insert, undo, redo and More. [insertOnBar] is false on
 * a compact window, where insert moves into More to leave room for the title. A [vertical] bar folds layers into More
 * as well, and every tip opens on [tip], away from the edge ([onMoreOpen] hears when the menu opens and closes).
 * [onLayersPlaced] hears where the button the layers panel opens from is: Layers, or More when layers is in it.
 * With the Image tool on the bar ([imageTool]) insert leaves pictures to it, and is left out when nothing remains.
 */
@Composable
internal fun NoteBarActions(
    info: NotebookInfo?,
    insertOnBar: Boolean,
    layersOpen: Boolean,
    onLayers: () -> Unit,
    insertOpen: Boolean,
    onInsertOpen: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    toggles: NoteToggles,
    actions: NoteActions,
    vertical: Boolean = false,
    tip: TooltipSide? = TooltipSide.Below,
    onMoreOpen: (Boolean) -> Unit = {},
    onLayersPlaced: (Rect) -> Unit = {},
    imageTool: Boolean = false,
) {
    val hasInsert = insertHasItems(imageTool, info?.infinite == true)
    val placed = Modifier.onGloballyPositioned { onLayersPlaced(it.boundsInWindow()) }
    if (!vertical) {
        Box(placed) {
            GraphoIconButton(
                R.drawable.ic_layers,
                stringResource(R.string.layers_title),
                onClick = onLayers,
                selected = layersOpen,
                tooltipSide = tip,
            )
        }
    }
    if (insertOnBar && hasInsert) {
        Box {
            GraphoIconButton(
                R.drawable.ic_plus,
                stringResource(R.string.note_bar_insert),
                onClick = { onInsertOpen(true) },
                selected = insertOpen,
                tooltipSide = tip,
            )
            InsertMenu(
                expanded = insertOpen,
                onDismiss = { onInsertOpen(false) },
                infinite = info?.infinite == true,
                actions = actions,
                images = !imageTool,
            )
        }
    }
    GraphoIconButton(
        R.drawable.ic_undo,
        stringResource(R.string.note_undo),
        onClick = onUndo,
        enabled = info?.canUndo == true,
        tooltipSide = tip,
    )
    GraphoIconButton(
        R.drawable.ic_redo,
        stringResource(R.string.note_redo),
        onClick = onRedo,
        enabled = info?.canRedo == true,
        tooltipSide = tip,
    )
    Box(if (vertical) placed else Modifier) {
        NoteMenu(
            info = info,
            toggles = toggles,
            actions = actions,
            insertInMenu = !insertOnBar && hasInsert,
            insertImages = !imageTool,
            insertOpen = insertOpen,
            onInsertOpen = onInsertOpen,
            layersInMenu = vertical,
            layersOpen = layersOpen,
            onLayers = onLayers,
            tip = tip,
            onOpenChange = onMoreOpen,
        )
    }
}
