package com.onemnemo.grapho.note

/**
 * Colours of the selection box, the crop frame, the eraser's circle and their handles on the page. Each mark is an
 * accent drawn over a soft halo of the opposite tone, so it reads on the paper and on ink or pictures under it. On
 * light paper the accent stays a mid blue, never black.
 */
internal object SelectionColours {
    const val WHITE = 0xFFFFFFFF.toInt()
    private const val LIGHT_ACCENT = 0xFF2F5BE0.toInt()
    private const val DARK_ACCENT = 0xFF8AA6FF.toInt()
    private const val LIGHT_HALO = 0xE6FFFFFF.toInt()
    private const val DARK_HALO = 0xCC15171C.toInt()

    fun accent(darkPaper: Boolean): Int = if (darkPaper) DARK_ACCENT else LIGHT_ACCENT

    /** The edge round the accent, light on light paper and dark on dark paper. */
    fun halo(darkPaper: Boolean): Int = if (darkPaper) DARK_HALO else LIGHT_HALO
}
