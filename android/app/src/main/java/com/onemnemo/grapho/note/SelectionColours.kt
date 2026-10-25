package com.onemnemo.grapho.note

/** Colours of the selection box, the crop frame and their handles on the page, chosen to read on the paper. */
internal object SelectionColours {
    const val WHITE = 0xFFFFFFFF.toInt()
    private const val LIGHT_LINE = 0xFF50545E.toInt()
    private const val DARK_LINE = 0xFFC9CCD4.toInt()
    private const val LIGHT_ACCENT = 0xFF2F5BE0.toInt()
    private const val DARK_ACCENT = 0xFF8AA6FF.toInt()

    fun line(darkPaper: Boolean): Int = if (darkPaper) DARK_LINE else LIGHT_LINE

    fun accent(darkPaper: Boolean): Int = if (darkPaper) DARK_ACCENT else LIGHT_ACCENT
}
