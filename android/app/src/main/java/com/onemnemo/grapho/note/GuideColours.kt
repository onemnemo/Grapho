package com.onemnemo.grapho.note

/** Colours of the ruler, the protractor and their readouts on the page. They mirror the ink and accent tokens. */
internal object GuideColours {
    private const val LIGHT_INK = 0xFF17181C.toInt()
    private const val DARK_INK = 0xFFECEDEF.toInt()
    private const val LIGHT_ACCENT = 0xFF2F5BE0.toInt()
    private const val DARK_ACCENT = 0xFF8AA6FF.toInt()
    private const val LIGHT_PAPER = 0xFFFFFFFF.toInt()
    private const val DARK_PAPER = 0xFF1C1D21.toInt()

    /** Tinted enough to see the body, clear enough to read the page through it. */
    fun body(dark: Boolean): Int = if (dark) 0x338AA6FF else 0x2E2F5BE0

    fun edge(dark: Boolean): Int = if (dark) 0xCC8AA6FF.toInt() else 0xCC2F5BE0.toInt()

    fun tick(dark: Boolean): Int = if (dark) 0xDDECEDEF.toInt() else 0xDD17181C.toInt()

    fun faint(dark: Boolean): Int = if (dark) 0x99ECEDEF.toInt() else 0x9917181C.toInt()

    fun accent(dark: Boolean): Int = if (dark) DARK_ACCENT else LIGHT_ACCENT

    fun text(dark: Boolean): Int = if (dark) DARK_INK else LIGHT_INK

    /** Behind text, so a number stays legible over ticks and ink. */
    fun halo(dark: Boolean): Int = if (dark) 0xE61C1D21.toInt() else 0xE6FFFFFF.toInt()

    fun button(dark: Boolean): Int = if (dark) DARK_PAPER else LIGHT_PAPER
}
