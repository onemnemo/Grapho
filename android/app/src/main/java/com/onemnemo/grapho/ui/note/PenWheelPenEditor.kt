package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.PenGlyph
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel

/** The pens in list order, each a row with its glyph, name and buttons; tapping a row opens its editor below it. */
@Composable
internal fun PensTab(state: ToolbarState, store: PenStore, notices: Notices) {
    var open by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    for (i in store.pens.indices) {
        val pen = store.pens[i]
        PenEntry(
            pen = pen,
            open = open == i,
            canUp = i > 0,
            canDown = i < store.pens.lastIndex,
            onTap = { open = if (open == i) null else i },
            onMove = { d ->
                val to = store.move(i, d)
                val was = open
                open = when (was) {
                    i -> to
                    to -> i
                    else -> was
                }
            },
            onDelete = {
                val was = open
                deletePen(context, store, i, notices)
                open = when {
                    was == null || was == i -> null
                    was > i -> was - 1
                    else -> was
                }
            },
        )
        if (open == i) PenEditor(state, store, i)
    }
}

@Composable
private fun PenEntry(
    pen: PenPreset,
    open: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onTap: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val editLabel = stringResource(R.string.wheel_edit_named, pen.name)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (open) GraphoWheel.selectedFill else GraphoWheel.well)
            .pressable(scaleOnPress = false, onClick = onTap)
            .padding(horizontal = 8.dp)
            .semantics { contentDescription = editLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            PenGlyph(
                kind = pen.kind,
                color = pen.color.copy(alpha = pen.alpha / 255f),
                outline = GraphoWheel.textMuted,
                modifier = Modifier
                    .size(width = 12.dp, height = 30.dp)
                    .graphicsLayer { rotationZ = 90f },
            )
        }
        Text(
            pen.name,
            style = Grapho.type.label,
            color = GraphoWheel.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconAction(R.drawable.ic_chevron_up, stringResource(R.string.wheel_move_up_named, pen.name), canUp) { onMove(-1) }
        IconAction(R.drawable.ic_chevron_down, stringResource(R.string.wheel_move_down_named, pen.name), canDown) { onMove(1) }
        IconAction(R.drawable.ic_trash, stringResource(R.string.wheel_delete_item, pen.name), onClick = onDelete)
    }
}
