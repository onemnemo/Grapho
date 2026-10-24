package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel

private const val COLUMNS = 7
private const val NAME_MAX = 24

/**
 * The colour manager: every set with its name, its colours in a wrapping grid and an add swatch. Tap a
 * colour to pick it and get Edit, Move left, Move right and Delete for it. [picked] is the picked
 * colour as its set and place. Tapping outside closes it.
 */
@Composable
internal fun ColourManagerPanel(
    state: ToolbarState,
    book: ColourBook,
    progress: Float,
    notices: Notices,
    picked: Pair<Int, Int>?,
    onPick: (Pair<Int, Int>?) -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDone,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            Modifier
                .padding(12.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * progress
                    scaleX = s
                    scaleY = s
                    this.alpha = progress
                },
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GraphoWheel.body)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    for ((i, set) in book.sets.withIndex()) {
                        key(set.id) {
                            SetBlock(state, book, i, set, notices, picked, onPick, onEdit)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PanelButton(stringResource(R.string.wheel_new_set), filled = false, modifier = Modifier.weight(1f)) {
                        onPick(null)
                        book.addSet()
                    }
                    PanelButton(stringResource(R.string.action_done), filled = true, modifier = Modifier.weight(1f), onClick = onDone)
                }
            }
        }
    }
}

@Composable
private fun SetBlock(
    state: ToolbarState,
    book: ColourBook,
    index: Int,
    set: ColourSet,
    notices: Notices,
    picked: Pair<Int, Int>?,
    onPick: (Pair<Int, Int>?) -> Unit,
    onEdit: () -> Unit,
) {
    val last = book.sets.lastIndex
    val context = LocalContext.current
    var name by remember { mutableStateOf(set.name) }
    val pickedHere = picked?.takeIf { it.first == index }?.second
    val nameLabel = stringResource(R.string.wheel_set_name)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            BasicTextField(
                value = name,
                onValueChange = {
                    name = it.take(NAME_MAX)
                    book.rename(index, name)
                },
                singleLine = true,
                textStyle = Grapho.type.label.copy(color = GraphoWheel.text),
                cursorBrush = SolidColor(GraphoWheel.text),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GraphoWheel.field)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .semantics { contentDescription = nameLabel },
            )
            IconAction(R.drawable.ic_chevron_up, stringResource(R.string.wheel_move_up_named, set.name), index > 0) {
                onPick(null)
                book.moveSet(index, -1)
            }
            IconAction(R.drawable.ic_chevron_down, stringResource(R.string.wheel_move_down_named, set.name), index < last) {
                onPick(null)
                book.moveSet(index, 1)
            }
            IconAction(R.drawable.ic_star, stringResource(R.string.wheel_set_first_named, set.name), index > 0) {
                onPick(null)
                book.moveSetFirst(index)
            }
            if (book.sets.size > 1) {
                IconAction(R.drawable.ic_trash, stringResource(R.string.wheel_delete_item, set.name)) {
                    onPick(null)
                    val gone = book.removeSet(index) ?: return@IconAction
                    notices.show(
                        text = context.getString(R.string.wheel_set_deleted, gone.name),
                        actionLabel = context.getString(R.string.note_undo),
                        onAction = { book.restoreSet(index, gone) },
                    )
                }
            }
        }

        // Each row holds a fixed number of cells, the last of all being the add swatch.
        val cells: List<Int?> = set.colours + null
        for (row in cells.withIndex().chunked(COLUMNS)) {
            Row {
                for ((at, cell) in row) {
                    if (cell == null) {
                        AddSwatch {
                            val added = book.add(index, currentSetting(state).color)
                            onPick(index to added)
                        }
                    } else {
                        Swatch(cell, selected = pickedHere == at) { onPick(index to at) }
                    }
                }
            }
        }

        if (pickedHere != null && pickedHere in set.colours.indices) {
            Row {
                IconAction(R.drawable.ic_pencil, stringResource(R.string.colour_edit), onClick = onEdit)
                IconAction(R.drawable.ic_chevron_left, stringResource(R.string.colour_move_left), pickedHere > 0) {
                    onPick(index to book.move(index, pickedHere, -1))
                }
                IconAction(R.drawable.ic_chevron_right, stringResource(R.string.colour_move_right), pickedHere < set.colours.lastIndex) {
                    onPick(index to book.move(index, pickedHere, 1))
                }
                IconAction(R.drawable.ic_trash, stringResource(R.string.colour_delete)) {
                    book.remove(index, pickedHere)
                    onPick(null)
                }
            }
        }
    }
}

@Composable
internal fun Swatch(argb: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.colour_item, hexOf(argb))
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(argb))
                .border(1.dp, GraphoWheel.edge, CircleShape),
        )
        if (selected) Box(Modifier.size(36.dp).border(2.dp, GraphoWheel.text, CircleShape))
    }
}

@Composable
private fun AddSwatch(onClick: () -> Unit) {
    val description = stringResource(R.string.colour_add_current)
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(GraphoWheel.selectedFill),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = Grapho.type.label, color = GraphoWheel.text)
        }
    }
}
