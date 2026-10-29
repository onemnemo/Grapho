package com.onemnemo.grapho.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoContent
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes

/**
 * What the menu of one note can do. The screen decides what each one means. [moveTargets] is only called when
 * Move to opens, so a long list of folders is not built for every tile.
 */
class ItemActions(
    val onOpen: () -> Unit,
    val onRename: () -> Unit,
    val onEditLook: () -> Unit,
    val onSelect: () -> Unit,
    val onExport: () -> Unit,
    val onExportPdf: () -> Unit,
    val onDelete: () -> Unit,
    val starred: Boolean,
    val onToggleStar: () -> Unit,
    val moveTargets: () -> List<MoveTarget>,
    val onMoveTo: (String?) -> Unit,
)

/**
 * The menu behind a tile's or row's dots. An item that opens something else closes the menu first. Move to
 * swaps the rows for the list of places in the same menu, so it needs no second anchor.
 */
@Composable
fun ItemMenu(expanded: Boolean, onDismiss: () -> Unit, kind: EntryKind, actions: ItemActions) {
    var moving by remember { mutableStateOf(false) }
    val dismiss = {
        moving = false
        onDismiss()
    }
    GraphoMenu(expanded = expanded, onDismiss = dismiss) {
        if (moving) {
            MovePage(actions.moveTargets(), onBack = { moving = false }) { id ->
                dismiss()
                actions.onMoveTo(id)
            }
        } else {
            ItemRows(kind, actions, dismiss, onMove = { moving = true })
        }
    }
}

@Composable
private fun ItemRows(kind: EntryKind, actions: ItemActions, dismiss: () -> Unit, onMove: () -> Unit) {
    fun run(action: () -> Unit): () -> Unit = {
        dismiss()
        action()
    }
    GraphoMenuItem(
        stringResource(R.string.library_open),
        icon = R.drawable.ic_notebook,
        onClick = run(actions.onOpen),
    )
    GraphoMenuItem(
        stringResource(R.string.note_rename),
        icon = R.drawable.ic_pencil,
        onClick = run(actions.onRename),
    )
    if (kind != EntryKind.Pdf) {
        val edit = if (kind == EntryKind.Canvas) R.string.library_edit_canvas else R.string.library_edit_notebook
        GraphoMenuItem(stringResource(edit), icon = R.drawable.ic_paint_bucket, onClick = run(actions.onEditLook))
    }
    GraphoMenuItem(
        stringResource(if (actions.starred) R.string.library_unstar else R.string.library_star),
        icon = if (actions.starred) R.drawable.ic_star_filled else R.drawable.ic_star,
        onClick = run(actions.onToggleStar),
    )
    GraphoMenuItem(stringResource(R.string.library_move_to), icon = R.drawable.ic_folder_move, onClick = onMove)
    GraphoMenuItem(
        stringResource(R.string.library_select),
        icon = R.drawable.ic_select,
        onClick = run(actions.onSelect),
    )
    GraphoMenuDivider()
    GraphoMenuItem(
        stringResource(R.string.note_export),
        icon = R.drawable.ic_file_up,
        onClick = run(actions.onExport),
    )
    GraphoMenuItem(
        stringResource(R.string.library_export_pdf),
        icon = R.drawable.ic_file_up,
        onClick = run(actions.onExportPdf),
    )
    GraphoMenuDivider()
    val delete = when (kind) {
        EntryKind.Notebook -> R.string.library_delete_notebook
        EntryKind.Pdf -> R.string.library_delete_pdf
        EntryKind.Canvas -> R.string.library_delete_canvas
    }
    GraphoMenuItem(
        stringResource(delete),
        icon = R.drawable.ic_trash,
        danger = true,
        onClick = run(actions.onDelete),
    )
}

/**
 * The dots button with its menu. On a cover ([onMedia]) it is a dark disc with light dots, so it reads on any
 * colour. In a row it is a bare icon that fills on touch.
 */
@Composable
fun ItemMoreButton(kind: EntryKind, actions: ItemActions, modifier: Modifier = Modifier, onMedia: Boolean = false) {
    MoreButton(modifier, onMedia) { open, close ->
        ItemMenu(expanded = open, onDismiss = close, kind = kind, actions = actions)
    }
}

/** The dots button for any menu: [menu] is shown under it while it is open and calls its second argument to close. */
@Composable
fun MoreButton(
    modifier: Modifier = Modifier,
    onMedia: Boolean = false,
    menu: @Composable (open: Boolean, close: () -> Unit) -> Unit,
) {
    val c = Grapho.colors
    var open by remember { mutableStateOf(false) }
    val interaction = rememberControlInteraction()
    val disc by animateColorAsState(
        when {
            onMedia && interaction.pressed -> GraphoContent.actionGlassBgPressed
            onMedia -> GraphoContent.mediaGlass
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        tween(GraphoMotion.motionDurationFastMs),
        label = "moreFill",
    )
    val label = stringResource(R.string.library_item_options)
    Box(
        modifier = modifier
            .size(Grapho.metrics.sizeHitMin)
            .pressable(interactionSource = interaction.source, onClick = { open = true })
            .focusRing(interaction.focused, GraphoShapes.pill)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(Grapho.metrics.sizeControlSmall)
                .clip(GraphoShapes.pill)
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            GraphoIcon(
                R.drawable.ic_more_horizontal,
                null,
                if (onMedia) GraphoContent.mediaText else c.textMuted,
            )
        }
        Box(Modifier.matchParentSize()) {
            menu(open) { open = false }
        }
    }
}
