package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.TooltipSide
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The least room the title needs beside the tools, or it hides. */
internal val BAR_TITLE_MIN = GraphoSpace.space5xl + GraphoSpace.space3xl

/** The room a rule between two groups of tools takes on the bar, gaps included. */
internal val BAR_RULE_SLOT = GraphoSpace.spaceMd

/**
 * The tools on the bar, in order, with a quiet rule where the group changes. The one for the canvas's current tool
 * shows as selected. Image is not a canvas tool: it opens the picture choices under its own button
 * ([imageOpen] and [onImageOpen]). On a [vertical] bar the rule lies across the column, and each tip opens on
 * [tip], away from the edge. [onShapesPlaced] and [onTextPlaced] hear where the Shapes and Text buttons are, so
 * their options cards open from them.
 */
@Composable
internal fun NoteBarTools(
    tools: List<NoteBarTool>,
    mode: ToolMode,
    onTool: (NoteBarTool) -> Unit,
    imageOpen: Boolean,
    onImageOpen: (Boolean) -> Unit,
    actions: NoteActions,
    vertical: Boolean = false,
    tip: TooltipSide? = TooltipSide.Below,
    onShapesPlaced: (Rect) -> Unit = {},
    onTextPlaced: (Rect) -> Unit = {},
) {
    val active = NoteBarTool.forMode(mode)
    tools.forEachIndexed { index, tool ->
        if (index > 0 && tools[index - 1].group != tool.group) GroupRule(vertical)
        if (tool == NoteBarTool.Image) {
            Box {
                GraphoIconButton(
                    tool.icon,
                    stringResource(tool.label),
                    onClick = { onImageOpen(true) },
                    selected = imageOpen,
                    tooltipSide = tip,
                )
                InsertMenu(
                    expanded = imageOpen,
                    onDismiss = { onImageOpen(false) },
                    infinite = false,
                    actions = actions,
                    pages = false,
                )
            }
        } else {
            val placed = when (tool) {
                NoteBarTool.Shapes -> Modifier.onGloballyPositioned { onShapesPlaced(it.boundsInWindow()) }
                NoteBarTool.Text -> Modifier.onGloballyPositioned { onTextPlaced(it.boundsInWindow()) }
                else -> Modifier
            }
            Box(placed) {
                GraphoIconButton(
                    tool.icon,
                    stringResource(tool.label),
                    onClick = { onTool(tool) },
                    selected = tool == active,
                    tooltipSide = tip,
                )
            }
        }
    }
}

@Composable
private fun GroupRule(vertical: Boolean) {
    if (vertical) {
        Box(Modifier.height(BAR_RULE_SLOT).width(GraphoSpace.spaceXl), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .height(GraphoSpace.borderWidthHairline)
                    .fillMaxWidth()
                    .background(Grapho.colors.borderStrong),
            )
        }
    } else {
        Box(Modifier.width(BAR_RULE_SLOT).height(GraphoSpace.spaceXl), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(GraphoSpace.borderWidthHairline)
                    .fillMaxHeight()
                    .background(Grapho.colors.borderStrong),
            )
        }
    }
}

/**
 * Picks [tool] for the canvas. Picking the one that is already on opens the pen wheel at it, or puts the wheel
 * away when it is open. For Shapes and Text the second pick opens or closes their options instead, since the wheel
 * has nothing of its own for them.
 */
internal fun pickBarTool(toolbar: ToolbarState, ui: NoteUiState, tool: NoteBarTool) {
    val mode = tool.mode ?: return
    if (toolbar.mode != mode) {
        toolbar.mode = mode
    } else if (mode == ToolMode.SHAPE) {
        ui.shapeOptionsOpen = !ui.shapeOptionsOpen
    } else if (mode == ToolMode.TEXT) {
        ui.textOptionsOpen = !ui.textOptionsOpen
    } else {
        ui.wheelOpen = !ui.wheelOpen
    }
}
