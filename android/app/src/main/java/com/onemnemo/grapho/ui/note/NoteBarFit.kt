package com.onemnemo.grapho.ui.note

/** The tools that fit on the bar, and whether the title still has room beside them. */
data class BarFit(val tools: List<NoteBarTool>, val showTitle: Boolean)

/**
 * The measures [fitBar] works in, all in dp. [barLengthDp] is the bar's length along its edge: its width at the top
 * or bottom, its height at the left or right. [fixedSlots] counts the buttons that are always on the bar.
 */
data class BarMeasure(
    val barLengthDp: Float,
    val fixedSlots: Int,
    val slotDp: Float,
    val separatorDp: Float,
    val paddingDp: Float,
    val titleMinDp: Float,
)

/**
 * How many buttons are always on the bar: back, undo, redo and More, plus layers on a horizontal bar and insert
 * when [insertOnBar]. A vertical bar folds layers into More.
 */
fun barFixedSlots(insertOnBar: Boolean, vertical: Boolean): Int =
    4 + (if (vertical) 0 else 1) + (if (insertOnBar) 1 else 0)

private fun toolsWidthDp(tools: List<NoteBarTool>, m: BarMeasure): Float {
    val rules = tools.zipWithNext().count { (a, b) -> a.group != b.group }
    return tools.size * m.slotDp + rules * m.separatorDp
}

/**
 * Which of [tools] fit a bar of the measured length, in order, dropping from the end rather than overflowing. A
 * [compact] bar leaves out the tools that do not belong there. The [active] tool stays on the bar by taking the
 * place of the last one that fits. The title gets what is left, and hides when that is under its minimum or
 * [wantTitle] is off. A vertical bar passes [wantTitle] false, since it has no title.
 */
fun fitBar(
    tools: List<NoteBarTool>,
    active: NoteBarTool?,
    compact: Boolean,
    wantTitle: Boolean,
    m: BarMeasure,
): BarFit {
    val pool = tools.filter { !compact || it.onCompactBar }
    val room = m.barLengthDp - m.paddingDp - m.fixedSlots * m.slotDp
    var shown = pool
    while (shown.isNotEmpty() && toolsWidthDp(shown, m) > room) {
        val count = shown.size - 1
        shown = pool.take(count)
        if (active != null && active in pool && active !in shown && count >= 1) {
            shown = (pool.take(count - 1) + active).sortedBy { pool.indexOf(it) }
        }
    }
    val leftover = room - toolsWidthDp(shown, m)
    return BarFit(shown, wantTitle && leftover >= m.titleMinDp)
}
