package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LayerEditor
import com.onemnemo.grapho.note.PageLayers
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The layers of the page in view, top layer first. On a compact window it is a panel at the foot of the page, with
 * rounded top corners; on anything wider it is a card on the end edge. It does not hide the page behind a scrim,
 * so a layer can be switched while drawing, and it closes from its own button, the bar's Layers button or Back.
 * Tap a row to make its layer the one that drawing goes to. The engine counts layers from the bottom, so rows
 * map to indices in reverse.
 */
@Composable
internal fun LayerPanel(
    editor: LayerEditor,
    state: PageLayers,
    placement: LayersPlacement,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    val context = LocalContext.current
    var renaming by remember { mutableStateOf<Int?>(null) }
    val shape = if (placement == LayersPlacement.Bottom) {
        RoundedCornerShape(topStart = GraphoSpace.radiusContainer, topEnd = GraphoSpace.radiusContainer)
    } else {
        GraphoShapes.surface
    }
    val sized = if (placement == LayersPlacement.Bottom) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(GraphoSpace.sizePanelWidthOver)
    }
    Column(
        modifier = modifier
            .then(sized)
            .heightIn(max = GraphoSpace.sizePopoverMaxHeight)
            .floatingCard(shape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = GraphoSpace.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.layers_title),
                style = Grapho.type.heading,
                color = c.textDefault,
                modifier = Modifier.weight(1f),
            )
            GraphoIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose)
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            for (index in state.layers.indices.reversed()) {
                key(index) {
                    LayerRow(
                        layer = state.layers[index],
                        index = index,
                        count = state.layers.size,
                        active = index == state.active,
                        mergeable = canMergeDown(state, index),
                        editor = editor,
                        renaming = renaming == index,
                        onRename = { renaming = index },
                        onRenameDone = { if (renaming == index) renaming = null },
                    )
                }
            }
        }
        GraphoButton(
            text = stringResource(R.string.layers_add),
            onClick = { editor.add(state.nextName { context.getString(R.string.layer_default_name, it) }) },
            modifier = Modifier.fillMaxWidth().padding(top = GraphoSpace.spaceXs),
            kind = ButtonKind.Quiet,
            size = ButtonSize.Default,
            iconRes = R.drawable.ic_plus,
            enabled = state.canAdd,
        )
    }
}

/** Merging needs a layer below, and the engine refuses it when either layer is locked. */
private fun canMergeDown(state: PageLayers, index: Int): Boolean {
    val below = state.layers.getOrNull(index - 1) ?: return false
    return !below.locked && !state.layers[index].locked
}
