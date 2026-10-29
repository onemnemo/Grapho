package com.onemnemo.grapho.ui.note

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode

/** Tools that draw come before the ones that add something to the page, and a rule sits between the two. */
enum class BarToolGroup { Draw, Insert }

/**
 * A tool the note bar can hold. [mode] is the canvas tool it switches to, or null for a tool that is an action
 * instead (Image opens the insert menu). Fill stays on the pen wheel and is not here.
 */
enum class NoteBarTool(
    val id: String,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val group: BarToolGroup,
    val mode: ToolMode?,
) {
    Pen("pen", R.drawable.ic_pen, R.string.wheel_tool_pen, BarToolGroup.Draw, ToolMode.PEN),
    Highlighter(
        "highlighter",
        R.drawable.ic_highlighter,
        R.string.wheel_tool_highlighter,
        BarToolGroup.Draw,
        ToolMode.HIGHLIGHTER,
    ),
    Eraser("eraser", R.drawable.ic_eraser, R.string.wheel_tool_eraser, BarToolGroup.Draw, ToolMode.ERASER),
    Lasso("lasso", R.drawable.ic_lasso, R.string.wheel_tool_lasso, BarToolGroup.Draw, ToolMode.LASSO),
    Text("text", R.drawable.ic_text, R.string.wheel_tool_text, BarToolGroup.Insert, ToolMode.TEXT),
    Shapes("shapes", R.drawable.ic_shapes, R.string.wheel_tool_shapes, BarToolGroup.Insert, ToolMode.SHAPE),
    Image("image", R.drawable.ic_image, R.string.note_insert_group_image, BarToolGroup.Insert, null),
    ;

    /** The compact bar has no room for Image, which Insert in More already covers. */
    val onCompactBar: Boolean get() = this != Image

    companion object {
        /** What a new install shows, in order. */
        val defaults: List<NoteBarTool> = entries.toList()

        /** The tool that is on while the canvas is in [mode], or null for Fill, which only the wheel holds. */
        fun forMode(mode: ToolMode): NoteBarTool? = entries.firstOrNull { it.mode == mode }

        /**
         * [tools] with Shapes added once for a bar saved before the tool existed: just before Image when the bar has
         * it, else at the end. A bar that already has Shapes is left alone.
         */
        fun withShapesOffered(tools: List<NoteBarTool>): List<NoteBarTool> {
            if (Shapes in tools) return tools
            val at = tools.indexOf(Image).takeIf { it >= 0 } ?: tools.size
            return tools.take(at) + Shapes + tools.drop(at)
        }

        /**
         * [tools] with Text added once for a bar saved before the tool existed: just before Shapes when the bar has
         * it, else before Image, else at the end. A bar that already has Text is left alone.
         */
        fun withTextOffered(tools: List<NoteBarTool>): List<NoteBarTool> {
            if (Text in tools) return tools
            val before = listOf(Shapes, Image).map { tools.indexOf(it) }.firstOrNull { it >= 0 }
            val at = before ?: tools.size
            return tools.take(at) + Text + tools.drop(at)
        }

        /** Ids joined for storage. */
        fun encode(tools: List<NoteBarTool>): String = tools.joinToString(",") { it.id }

        /** Tools from stored ids, in stored order. Ids this version does not know and repeats are dropped. */
        fun decode(stored: String): List<NoteBarTool> =
            stored.split(',').mapNotNull { id -> entries.firstOrNull { it.id == id } }.distinct()
    }
}
