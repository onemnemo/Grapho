package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LayerEditor
import com.onemnemo.grapho.note.LayerInfo
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoInlineTextField
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberInlineEditState
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * One layer: eye, name, lock and its menu. Tapping anywhere else on the row makes it the active layer. While
 * [renaming], the name is a field in place, and [onRenameDone] says it has been left.
 */
@Composable
internal fun LayerRow(
    layer: LayerInfo,
    index: Int,
    count: Int,
    active: Boolean,
    mergeable: Boolean,
    editor: LayerEditor,
    renaming: Boolean,
    onRename: () -> Unit,
    onRenameDone: () -> Unit,
) {
    val c = Grapho.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Grapho.metrics.sizeRowList)
            .clip(GraphoShapes.item)
            .background(if (active) c.selectionFill else Color.Transparent)
            .semantics { selected = active }
            .pressable(scaleOnPress = false, onClick = { editor.setActive(index) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIconButton(
            if (layer.visible) R.drawable.ic_eye else R.drawable.ic_eye_off,
            stringResource(if (layer.visible) R.string.layer_hide else R.string.layer_show),
            onClick = { editor.setVisible(index, !layer.visible) },
            tint = if (layer.visible) c.textDefault else c.textDisabled,
        )
        if (renaming) {
            NameField(layer.name, onCommit = { editor.rename(index, it) }, onDone = onRenameDone)
        } else {
            Text(
                layer.name,
                style = Grapho.type.label,
                color = if (layer.visible) c.textDefault else c.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = GraphoSpace.spaceXs),
            )
        }
        GraphoIconButton(
            if (layer.locked) R.drawable.ic_lock else R.drawable.ic_lock_open,
            stringResource(if (layer.locked) R.string.layer_unlock else R.string.layer_lock),
            onClick = { editor.setLocked(index, !layer.locked) },
            tint = if (layer.locked) c.textDefault else c.textDisabled,
        )
        LayerMenu(layer, index, count, mergeable, editor, onRename)
    }
}

/** The layer's name as an inline field that takes focus at once and reports [onDone] when focus leaves it. */
@Composable
private fun RowScope.NameField(name: String, onCommit: (String) -> Unit, onDone: () -> Unit) {
    var entered by remember { mutableStateOf(false) }
    Box(
        Modifier
            .weight(1f)
            .padding(horizontal = GraphoSpace.spaceXs)
            .onFocusChanged { state ->
                if (state.hasFocus) {
                    entered = true
                } else if (entered) {
                    onDone()
                }
            },
    ) {
        GraphoInlineTextField(value = name, onCommit = onCommit, state = rememberInlineEditState(editing = true))
    }
}
