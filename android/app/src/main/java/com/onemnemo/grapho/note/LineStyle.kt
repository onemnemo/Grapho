package com.onemnemo.grapho.note

/**
 * How a stroke's line is drawn. The [code] is what the core stores and takes over the native interface,
 * so the values never change; [key] is the name saved pens use.
 */
enum class LineStyle(val code: Int, val key: String) {
    SOLID(0, "solid"),
    DASHED(1, "dashed"),
    DOTTED(2, "dotted"),
    ;

    companion object {
        /** The style for a core [code], or [SOLID] for one this version does not know. */
        fun fromCode(code: Int): LineStyle = entries.firstOrNull { it.code == code } ?: SOLID

        /** The style saved under [key], or [SOLID] when it is missing or unknown. */
        fun fromKey(key: String?): LineStyle = entries.firstOrNull { it.key == key } ?: SOLID
    }
}
