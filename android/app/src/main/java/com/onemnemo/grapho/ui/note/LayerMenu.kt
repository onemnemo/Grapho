package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LayerEditor
import com.onemnemo.grapho.note.LayerInfo
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

/**
 * A layer's overflow button and menu. [index] counts from the bottom, so moving a layer up in the panel
 * is a move to the next higher index. Opacity opens a second page with a slider that applies as it moves.
 */
@Composable
internal fun LayerMenu(
    layer: LayerInfo,
    index: Int,
    count: Int,
    mergeable: Boolean,
    editor: LayerEditor,
    onRename: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var opacityPage by remember { mutableStateOf(false) }
    fun run(action: () -> Unit) {
        open = false
        action()
    }

    Box {
        GraphoIconButton(R.drawable.ic_more_vertical, stringResource(R.string.layer_options), onClick = {
            opacityPage = false
            open = true
        })
        GraphoMenu(expanded = open, onDismiss = { open = false }) {
            if (opacityPage) {
                OpacityPage(layer, index, editor, onBack = { opacityPage = false })
            } else {
                GraphoMenuItem(stringResource(R.string.layer_rename), onClick = { run(onRename) })
                GraphoMenuItem(stringResource(R.string.layer_opacity), onClick = { opacityPage = true })
                GraphoMenuDivider()
                GraphoMenuItem(
                    stringResource(R.string.layer_move_up),
                    icon = R.drawable.ic_chevron_up,
                    enabled = index < count - 1,
                    onClick = { run { editor.move(index, index + 1) } },
                )
                GraphoMenuItem(
                    stringResource(R.string.layer_move_down),
                    icon = R.drawable.ic_chevron_down,
                    enabled = index > 0,
                    onClick = { run { editor.move(index, index - 1) } },
                )
                GraphoMenuItem(
                    stringResource(R.string.layer_merge_down),
                    enabled = mergeable,
                    onClick = { run { editor.mergeDown(index) } },
                )
                GraphoMenuDivider()
                GraphoMenuItem(
                    stringResource(R.string.layer_delete),
                    icon = R.drawable.ic_trash,
                    enabled = count > 1,
                    danger = true,
                    onClick = { run { editor.remove(index) } },
                )
            }
        }
    }
}

@Composable
private fun OpacityPage(layer: LayerInfo, index: Int, editor: LayerEditor, onBack: () -> Unit) {
    val c = Grapho.colors
    var value by remember { mutableFloatStateOf(layer.opacity) }
    val label = stringResource(R.string.layer_opacity)
    GraphoMenuItem(stringResource(R.string.action_back), icon = R.drawable.ic_chevron_left, onClick = onBack)
    GraphoMenuDivider()
    Text(
        stringResource(R.string.layer_opacity_percent, (value * 100f).roundToInt()),
        style = Grapho.type.tabular,
        color = c.textMuted,
        modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceXs),
    )
    GraphoSlider(
        value = value,
        onValueChange = {
            value = it
            editor.setOpacity(index, it, commit = false)
        },
        label = label,
        onValueChangeFinished = { editor.setOpacity(index, value, commit = true) },
        modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
    )
}
