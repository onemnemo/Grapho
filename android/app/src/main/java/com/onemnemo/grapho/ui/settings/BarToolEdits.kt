package com.onemnemo.grapho.ui.settings

import com.onemnemo.grapho.ui.note.NoteBarTool

/**
 * [tool] switched on: it joins the bar where it sits in the usual order, before the first tool that comes after
 * it there, so a tool switched back on does not land at the far end.
 */
internal fun barToolsWith(on: List<NoteBarTool>, tool: NoteBarTool): List<NoteBarTool> {
    if (tool in on) return on
    val at = on.indexOfFirst { it.ordinal > tool.ordinal }.takeIf { it >= 0 } ?: on.size
    return on.toMutableList().apply { add(at, tool) }
}

internal fun barToolsWithout(on: List<NoteBarTool>, tool: NoteBarTool): List<NoteBarTool> = on - tool

/** [tool] moved [by] places along the bar (negative is toward the start), stopping at the ends. */
internal fun barToolsMoved(on: List<NoteBarTool>, tool: NoteBarTool, by: Int): List<NoteBarTool> {
    val from = on.indexOf(tool)
    if (from < 0) return on
    val to = (from + by).coerceIn(0, on.lastIndex)
    return on.toMutableList().apply {
        removeAt(from)
        add(to, tool)
    }
}
