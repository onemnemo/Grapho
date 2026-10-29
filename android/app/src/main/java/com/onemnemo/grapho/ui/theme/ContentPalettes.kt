package com.onemnemo.grapho.ui.theme

import androidx.compose.ui.graphics.Color
import com.onemnemo.grapho.R

/** Cover and folder colours. They look the same in both themes. */
object FolderColors {
    val grey = Color(0xFFD6D3CD)
    val blue = Color(0xFFA9C4F5)
    val lilac = Color(0xFFCDB9F3)
    val pink = Color(0xFFF5AECB)
    val green = Color(0xFFA9DDB2)
    val orange = Color(0xFFF8C68F)
    val yellow = Color(0xFFF2DC86)
    val teal = Color(0xFF9FE0DE)
    val ink = Color(0xFF17181C)

    /** Name (a string resource) and colour, default first. */
    val all: List<Pair<Int, Color>> = listOf(
        R.string.colour_grey to grey,
        R.string.colour_blue to blue,
        R.string.colour_lilac to lilac,
        R.string.colour_pink to pink,
        R.string.colour_green to green,
        R.string.colour_orange to orange,
        R.string.colour_yellow to yellow,
        R.string.colour_teal to teal,
    )
}

/** Pen and highlighter colours are content, so they never follow the theme. */
object PenColors {
    val pens: List<Pair<Int, Color>> = listOf(
        R.string.colour_black to Color(0xFF17181C),
        R.string.colour_blue to Color(0xFF2F5BE0),
        R.string.colour_red to Color(0xFFD23B3B),
        R.string.colour_green to Color(0xFF16A34A),
        R.string.colour_orange to Color(0xFFEA580C),
        R.string.colour_violet to Color(0xFF7A4FD0),
        R.string.colour_graphite to Color(0xFF5B5F69),
    )

    val highlighters: List<Pair<Int, Color>> = listOf(
        R.string.colour_yellow to Color(0xFFF5D33F),
        R.string.colour_green to Color(0xFF86EFAC),
        R.string.colour_pink to Color(0xFFF9A8D4),
    )

    /** Highlighter ink is drawn at 45% opacity. */
    const val HIGHLIGHT_ALPHA = 0x73
}
