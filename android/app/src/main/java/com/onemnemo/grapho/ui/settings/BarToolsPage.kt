package com.onemnemo.grapho.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoSwitch
import com.onemnemo.grapho.ui.note.NoteBarTool
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The tools on the note bar. The ones that are on come first, in bar order, each with a switch and arrows to move
 * it; the ones that are off follow with their switch. Changes apply at once. Fill is not listed because it stays on
 * the pen wheel.
 */
@Composable
internal fun BarToolsPage(settings: AppSettings) {
    val on = settings.barTools
    val off = NoteBarTool.entries.filter { it !in on }
    SettingsPage {
        Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
            Text(
                stringResource(R.string.settings_bar_tools_hint),
                style = Grapho.type.caption,
                color = Grapho.colors.textSubtle,
                modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceXs),
            )
            on.forEachIndexed { index, tool ->
                ToolRow(
                    tool = tool,
                    on = true,
                    canMoveUp = index > 0,
                    canMoveDown = index < on.lastIndex,
                    onMove = { settings.changeBarTools(barToolsMoved(on, tool, it)) },
                    onSwitch = { settings.changeBarTools(barToolsWithout(on, tool)) },
                )
            }
            for (tool in off) {
                ToolRow(
                    tool = tool,
                    on = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMove = {},
                    onSwitch = { settings.changeBarTools(barToolsWith(on, tool)) },
                )
            }
        }
    }
}

@Composable
private fun ToolRow(
    tool: NoteBarTool,
    on: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onSwitch: () -> Unit,
) {
    val c = Grapho.colors
    val name = stringResource(tool.label)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Grapho.metrics.sizeRowList)
            .padding(horizontal = GraphoSpace.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIcon(tool.icon, null, if (on) c.textDefault else c.textSubtle)
        Text(
            name,
            style = Grapho.type.label,
            color = if (on) c.textDefault else c.textMuted,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(start = GraphoSpace.spaceXs),
        )
        if (on) {
            GraphoIconButton(
                R.drawable.ic_chevron_up,
                stringResource(R.string.wheel_move_up_named, name),
                onClick = { onMove(-1) },
                enabled = canMoveUp,
            )
            GraphoIconButton(
                R.drawable.ic_chevron_down,
                stringResource(R.string.wheel_move_down_named, name),
                onClick = { onMove(1) },
                enabled = canMoveDown,
            )
        }
        GraphoSwitch(checked = on, onCheckedChange = { onSwitch() }, label = name)
    }
}
