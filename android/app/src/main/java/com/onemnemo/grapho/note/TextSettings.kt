package com.onemnemo.grapho.note

/**
 * What a new text box looks like, and what the Text options card edits: the font [size] in points, the [argb]
 * colour, how the lines sit across the box and whether they are [bold], [italic] or [underline]. Every number is kept
 * inside its range by [coerced].
 */
data class TextSettings(
    val size: Int = DEFAULT_SIZE,
    val argb: Int = DEFAULT_COLOUR,
    val align: TextAlignment = TextAlignment.Start,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
) {
    fun coerced() = copy(size = size.coerceIn(MIN_SIZE, MAX_SIZE), argb = argb or OPAQUE)

    /** One string for storage. */
    fun encode(): String = listOf(size, argb, align.code, bold, italic, underline).joinToString(",")

    companion object {
        const val MIN_SIZE = 8
        const val MAX_SIZE = 96
        const val DEFAULT_SIZE = 18

        /** A new box is this wide, in points. */
        const val DEFAULT_WIDTH = 240f

        /** The most characters one box holds, as the core allows. */
        const val MAX_CHARS = 20_000
        private const val DEFAULT_COLOUR = 0xFF1A1D26.toInt()
        private const val OPAQUE = 0xFF000000.toInt()
        private const val OLD_FIELDS = 4
        private const val FIELDS = 6

        /** The settings saved by [encode], or the defaults for anything missing or damaged. */
        fun decode(stored: String?): TextSettings {
            val f = stored?.split(',') ?: return TextSettings()
            if (f.size != FIELDS && f.size != OLD_FIELDS) return TextSettings()
            val fallback = TextSettings()
            return TextSettings(
                size = f[0].toIntOrNull() ?: fallback.size,
                argb = f[1].toIntOrNull() ?: fallback.argb,
                align = TextAlignment.of(f[2].toIntOrNull() ?: 0),
                bold = f[3].toBooleanStrictOrNull() ?: fallback.bold,
                italic = f.getOrNull(4)?.toBooleanStrictOrNull() ?: fallback.italic,
                underline = f.getOrNull(5)?.toBooleanStrictOrNull() ?: fallback.underline,
            ).coerced()
        }
    }
}
