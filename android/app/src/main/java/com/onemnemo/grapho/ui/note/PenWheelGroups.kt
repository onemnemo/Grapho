package com.onemnemo.grapho.ui.note

import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.theme.GraphoWheel

private val BACK = WheelTool("back", WheelAction.Back, R.string.action_back) {
    GraphoIcon(R.drawable.ic_chevron_left, null, GraphoWheel.icon, size = 22.dp)
}

private fun toolsById(ids: List<String>): List<WheelTool> =
    ids.mapNotNull { id -> TOOLS.firstOrNull { it.id == id } }

private fun groupTool(group: WheelSlot.Group): WheelTool? {
    val inside = toolsById(group.toolIds)
    if (inside.isEmpty()) return null
    return WheelTool("group:${group.id}", WheelAction.OpenGroup(group.id, inside.map { it.action }), name = group.name) {
        GraphoIcon(R.drawable.ic_layers, null, GraphoWheel.icon, size = 22.dp)
    }
}

/**
 * What the tool ring shows: one tool per place, with each group as a single place that opens it. While the
 * group [openGroup] is open, the ring shows a back place first and then that group's tools.
 */
internal fun ringTools(slots: List<WheelSlot>, openGroup: String?): List<WheelTool> {
    val open = slots.filterIsInstance<WheelSlot.Group>().firstOrNull { it.id == openGroup }
    if (open != null) return listOf(BACK) + toolsById(open.toolIds)
    return slots.mapNotNull { slot ->
        when (slot) {
            is WheelSlot.Single -> TOOLS.firstOrNull { it.id == slot.toolId }
            is WheelSlot.Group -> groupTool(slot)
        }
    }
}

/** Whether choosing [action] would be the tool in use now. A group is in use when any tool in it is. */
internal fun isActive(state: ToolbarState, rulerOn: Boolean, action: WheelAction): Boolean = when (action) {
    is WheelAction.Mode -> state.mode == action.mode
    WheelAction.Ruler -> rulerOn
    is WheelAction.OpenGroup -> action.actions.any { isActive(state, rulerOn, it) }
    WheelAction.Back -> false
}

/** Whether [action] is the pen, highlighter, eraser, fill or lasso in use, which the ruler never is. */
internal fun isModeTool(state: ToolbarState, action: WheelAction): Boolean = when (action) {
    is WheelAction.Mode -> state.mode == action.mode
    is WheelAction.OpenGroup -> action.actions.any { isModeTool(state, it) }
    WheelAction.Ruler, WheelAction.Back -> false
}
