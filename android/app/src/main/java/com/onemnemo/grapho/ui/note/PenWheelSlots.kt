package com.onemnemo.grapho.ui.note

/** The most places the wheel's tool ring has. A group takes one place however many tools it holds. */
internal const val MAX_WHEEL_TOOLS = 8

/** One place on the wheel's tool ring: a tool on its own, or a named group holding any number of tools. */
internal sealed interface WheelSlot {
    data class Single(val toolId: String) : WheelSlot
    data class Group(val id: String, val name: String, val toolIds: List<String>) : WheelSlot
}

/** A group with no tools has nothing to show, so it takes no place on the wheel. */
internal fun WheelSlot.isShown(): Boolean = when (this) {
    is WheelSlot.Single -> true
    is WheelSlot.Group -> toolIds.isNotEmpty()
}

internal fun shownCount(slots: List<WheelSlot>): Int = slots.count { it.isShown() }

/** Tool ids that earlier versions saved and the wheel no longer offers. */
internal val RETIRED_TOOL_IDS = setOf("sizes")

/** [slots] without the retired tools, so a layout saved before they went keeps every other place. */
internal fun withoutRetired(slots: List<WheelSlot>): List<WheelSlot> = slots.mapNotNull {
    when (it) {
        is WheelSlot.Single -> if (it.toolId in RETIRED_TOOL_IDS) null else it
        is WheelSlot.Group -> it.copy(toolIds = it.toolIds.filter { id -> id !in RETIRED_TOOL_IDS })
    }
}

/** The tools every saved layout was made with, before the wheel kept a list of the tools it has offered. */
internal val ORIGINAL_TOOL_IDS = setOf("pen", "highlighter", "eraser", "fill", "sizes")

/**
 * [slots] with each tool in [known] that was never [offered] added at the end as a place of its own, while the
 * wheel has room. A tool the person already has, on the ring or in a group, stays where it is.
 */
internal fun withNewToolsOffered(slots: List<WheelSlot>, known: List<String>, offered: Set<String>): List<WheelSlot> {
    val next = slots.toMutableList()
    for (id in known) {
        if (id in offered || id in toolsOn(next)) continue
        if (shownCount(next) >= MAX_WHEEL_TOOLS) break
        next.add(WheelSlot.Single(id))
    }
    return next
}

/** The ids of every tool that is on the wheel, on its own or in a group, in order. */
internal fun toolsOn(slots: List<WheelSlot>): List<String> = slots.flatMap {
    when (it) {
        is WheelSlot.Single -> listOf(it.toolId)
        is WheelSlot.Group -> it.toolIds
    }
}

private fun without(slots: List<WheelSlot>, toolId: String): List<WheelSlot> = slots.mapNotNull {
    when (it) {
        is WheelSlot.Single -> if (it.toolId == toolId) null else it
        is WheelSlot.Group -> if (toolId in it.toolIds) it.copy(toolIds = it.toolIds - toolId) else it
    }
}

private fun fits(slots: List<WheelSlot>): List<WheelSlot>? = if (shownCount(slots) > MAX_WHEEL_TOOLS) null else slots

/** [slots] with [toolId] turned on at the end, or off. Null when the wheel is full, or it is the last tool on. */
internal fun withToolToggled(slots: List<WheelSlot>, toolId: String): List<WheelSlot>? {
    val on = toolsOn(slots)
    if (toolId in on) return if (on.size <= 1) null else without(slots, toolId)
    return fits(slots + WheelSlot.Single(toolId))
}

/**
 * [slots] with the tool [toolId] moved into the group [groupId], or out of its group onto the ring
 * when [groupId] is null. Null when taking it out would overfill the wheel.
 */
internal fun withToolInGroup(slots: List<WheelSlot>, toolId: String, groupId: String?): List<WheelSlot>? {
    if (toolId !in toolsOn(slots)) return null
    val from = slots.indexOfFirst { it is WheelSlot.Group && toolId in it.toolIds }
    if (groupId == null) {
        if (from < 0) return slots
        val next = without(slots, toolId).toMutableList()
        next.add(from + 1, WheelSlot.Single(toolId))
        return fits(next)
    }
    val target = slots.filterIsInstance<WheelSlot.Group>().firstOrNull { it.id == groupId } ?: return null
    if (toolId in target.toolIds) return slots
    return without(slots, toolId).map {
        if (it is WheelSlot.Group && it.id == groupId) it.copy(toolIds = it.toolIds + toolId) else it
    }
}

internal fun withNewGroup(slots: List<WheelSlot>, id: String, name: String): List<WheelSlot> =
    slots + WheelSlot.Group(id, name, emptyList())

internal fun withGroupRenamed(slots: List<WheelSlot>, id: String, name: String): List<WheelSlot> =
    slots.map { if (it is WheelSlot.Group && it.id == id) it.copy(name = name) else it }

/** [slots] without the group [id]; its tools stay on the ring on their own, as many as fit. */
internal fun withoutGroup(slots: List<WheelSlot>, id: String): List<WheelSlot> {
    val next = slots.flatMap {
        if (it is WheelSlot.Group && it.id == id) it.toolIds.map { tool -> WheelSlot.Single(tool) } else listOf(it)
    }.toMutableList()
    while (shownCount(next) > MAX_WHEEL_TOOLS) next.removeAt(next.indexOfLast { it.isShown() })
    return next
}

/** [slots] with the place at [index] moved by [delta] places. */
internal fun withSlotMoved(slots: List<WheelSlot>, index: Int, delta: Int): List<WheelSlot> {
    val to = index + delta
    if (index !in slots.indices || to !in slots.indices) return slots
    val next = slots.toMutableList()
    next.add(to, next.removeAt(index))
    return next
}

/** [slots] with [toolId] moved by [delta] places inside the group [groupId]. */
internal fun withToolMovedIn(slots: List<WheelSlot>, groupId: String, toolId: String, delta: Int): List<WheelSlot> =
    slots.map {
        if (it is WheelSlot.Group && it.id == groupId) {
            val from = it.toolIds.indexOf(toolId)
            val to = from + delta
            if (from < 0 || to !in it.toolIds.indices) {
                it
            } else {
                val ids = it.toolIds.toMutableList()
                ids.add(to, ids.removeAt(from))
                it.copy(toolIds = ids)
            }
        } else {
            it
        }
    }
