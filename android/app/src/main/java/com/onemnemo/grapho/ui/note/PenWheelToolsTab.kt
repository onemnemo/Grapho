package com.onemnemo.grapho.ui.note

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel

private const val GROUP_NAME_MAX = 24

/** What the Tools tab is showing besides the store: the note about a full wheel, and which tool is choosing a group. */
private class ToolsUi(val store: WheelToolStore) {
    var full by mutableStateOf(false)
    var choosing by mutableStateOf<String?>(null)
}

/**
 * The tools on the wheel in ring order, each place a tool or a group with its tools under it, then the
 * tools that are off. The header counts the places used out of [MAX_WHEEL_TOOLS].
 */
@Composable
internal fun ToolsTab(store: WheelToolStore) {
    val ui = remember(store) { ToolsUi(store) }
    val slots = store.slots.toList()
    val groups = slots.filterIsInstance<WheelSlot.Group>()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.wheel_tools_count, store.shown, MAX_WHEEL_TOOLS),
            style = Grapho.type.caption,
            color = GraphoWheel.textMuted,
            modifier = Modifier.weight(1f),
        )
        Chip(stringResource(R.string.wheel_new_group), null) {
            ui.choosing = null
            ui.full = false
            store.newGroup()
        }
    }
    if (ui.full) Text(stringResource(R.string.wheel_full_note), style = Grapho.type.caption, color = GraphoWheel.text)
    for ((i, slot) in slots.withIndex()) {
        when (slot) {
            is WheelSlot.Single -> TOOLS.firstOrNull { it.id == slot.toolId }?.let { tool ->
                key(tool.id) {
                    ToolRow(
                        ui = ui,
                        tool = tool,
                        on = true,
                        groups = groups,
                        inGroup = null,
                        canUp = i > 0,
                        canDown = i < slots.lastIndex,
                        indent = false,
                        onMove = { store.move(i, it) },
                    )
                }
            }
            is WheelSlot.Group -> key(slot.id) { GroupBlock(ui, slot, groups, i, slots.lastIndex) }
        }
    }
    val on = toolsOn(slots)
    for (tool in TOOLS.filter { it.id !in on }) {
        key(tool.id) {
            ToolRow(ui, tool, false, groups, null, canUp = false, canDown = false, indent = false, onMove = {})
        }
    }
}

@Composable
private fun GroupBlock(ui: ToolsUi, group: WheelSlot.Group, groups: List<WheelSlot.Group>, index: Int, last: Int) {
    val store = ui.store
    var name by remember { mutableStateOf(group.name) }
    var confirming by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val tools = group.toolIds.mapNotNull { id -> TOOLS.firstOrNull { it.id == id } }
    val nameLabel = stringResource(R.string.wheel_group_name)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (confirming) {
            ConfirmRow(
                message = stringResource(R.string.wheel_delete_named, group.name),
                modifier = Modifier.height(52.dp).clip(shape).background(GraphoWheel.well),
                onCancel = { confirming = false },
                onConfirm = {
                    ui.full = false
                    store.removeGroup(group.id)
                },
            )
        } else {
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(shape).background(GraphoWheel.well).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    GraphoIcon(R.drawable.ic_layers, null, GraphoWheel.text, size = 22.dp)
                }
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(GROUP_NAME_MAX)
                        if (name.isNotBlank()) store.rename(group.id, name.trim())
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
                IconAction(R.drawable.ic_chevron_up, stringResource(R.string.wheel_move_up_named, group.name), index > 0) { store.move(index, -1) }
                IconAction(R.drawable.ic_chevron_down, stringResource(R.string.wheel_move_down_named, group.name), index < last) { store.move(index, 1) }
                IconAction(R.drawable.ic_trash, stringResource(R.string.wheel_delete_item, group.name)) { confirming = true }
            }
        }
        for ((at, tool) in tools.withIndex()) {
            key(tool.id) {
                ToolRow(
                    ui = ui,
                    tool = tool,
                    on = true,
                    groups = groups,
                    inGroup = group.id,
                    canUp = at > 0,
                    canDown = at < tools.lastIndex,
                    indent = true,
                    onMove = { store.moveIn(group.id, tool.id, it) },
                )
            }
        }
        if (tools.isEmpty()) {
            Text(
                stringResource(R.string.wheel_group_empty),
                style = Grapho.type.caption,
                color = GraphoWheel.textMuted,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}

/** One tool: its icon and name, then when it is on its move and group buttons, and the switch. */
@Composable
private fun ToolRow(
    ui: ToolsUi,
    tool: WheelTool,
    on: Boolean,
    groups: List<WheelSlot.Group>,
    inGroup: String?,
    canUp: Boolean,
    canDown: Boolean,
    indent: Boolean,
    onMove: (Int) -> Unit,
) {
    val store = ui.store
    val toolLabel = tool.label()
    Column(
        Modifier.padding(start = if (indent) 16.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GraphoWheel.well)
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { tool.icon() }
            Text(
                toolLabel,
                style = Grapho.type.label,
                color = if (on) GraphoWheel.text else GraphoWheel.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (on) {
                IconAction(R.drawable.ic_chevron_up, stringResource(R.string.wheel_move_up_named, toolLabel), canUp) { onMove(-1) }
                IconAction(R.drawable.ic_chevron_down, stringResource(R.string.wheel_move_down_named, toolLabel), canDown) { onMove(1) }
                IconAction(R.drawable.ic_folder, stringResource(R.string.wheel_move_to_group, toolLabel)) {
                    ui.choosing = if (ui.choosing == tool.id) null else tool.id
                }
            }
            Toggle(on, enabled = !on || store.toolsOnCount > 1, label = toolLabel) {
                ui.choosing = null
                ui.full = !store.toggle(tool.id) && !on
            }
        }
        if (on && ui.choosing == tool.id) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (inGroup != null) {
                    Chip(stringResource(R.string.wheel_on_ring), null) { chooseGroup(ui, tool.id, null) }
                }
                for (group in groups.filter { it.id != inGroup }) {
                    Chip(group.name, R.drawable.ic_layers) { chooseGroup(ui, tool.id, group.id) }
                }
                Chip(stringResource(R.string.wheel_new_group), R.drawable.ic_plus) { chooseGroup(ui, tool.id, store.newGroup()) }
            }
        }
    }
}

private fun chooseGroup(ui: ToolsUi, toolId: String, groupId: String?) {
    ui.full = !ui.store.setGroup(toolId, groupId)
    ui.choosing = null
}

@Composable
private fun Chip(label: String, @DrawableRes icon: Int?, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GraphoWheel.selectedFill)
            .pressable(scaleOnPress = false, onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) GraphoIcon(icon, null, GraphoWheel.text, size = 16.dp)
        Text(label, style = Grapho.type.caption, color = GraphoWheel.text, maxLines = 1)
    }
}

@Composable
private fun Toggle(on: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    val description = stringResource(if (on) R.string.wheel_toggle_on else R.string.wheel_toggle_off, label)
    Box(
        Modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (on) GraphoWheel.text else GraphoWheel.plate)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressable(enabled = enabled, scaleOnPress = false, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Spacer(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (on) GraphoWheel.body else GraphoWheel.text),
        )
    }
}
