package com.onemnemo.grapho.note

import java.util.Locale

/** What a library item is. [key] is the name the library file stores. */
enum class LibraryKind(val key: String) {
    Notebook("notebook"),
    Pdf("pdf"),
    Canvas("canvas"),
    ;

    companion object {
        /** The kind stored under [key], or [Notebook] for a name this version does not know. */
        fun fromKey(key: String): LibraryKind = values().firstOrNull { it.key == key } ?: Notebook

        /** An infinite canvas is a canvas, a notebook with a PDF page is a PDF, anything else is a notebook. */
        fun of(infinite: Boolean, hasPdf: Boolean): LibraryKind = when {
            infinite -> Canvas
            hasPdf -> Pdf
            else -> Notebook
        }
    }
}

/** The cover content refs the library stores. A colour is `color:#RRGGBB`. */
object CoverRef {
    private const val COLOR_PREFIX = "color:#"
    private const val OPAQUE = -0x1000000
    private const val RGB_MASK = 0xFFFFFF

    /** The ref for an ARGB colour. The alpha is dropped. */
    fun color(argb: Int): String = COLOR_PREFIX + String.format(Locale.ROOT, "%06X", argb and RGB_MASK)

    /** The opaque ARGB colour of a `color:#RRGGBB` ref, or null for no ref or any other kind of ref. */
    fun colorOf(ref: String?): Int? {
        if (ref == null || !ref.startsWith(COLOR_PREFIX)) return null
        val hex = ref.substring(COLOR_PREFIX.length)
        if (hex.length != 6 || !hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return OPAQUE or hex.toInt(16)
    }
}

/** The error codes the core uses, read back from the message of a failed library call. */
object LibraryErrors {
    const val NOT_FOUND = "not_found"
    const val IN_TRASH = "in_trash"
    const val NEEDS_CHOICE = "needs_choice"
    const val INTO_SELF = "into_self"
    const val INTO_DESCENDANT = "into_descendant"
    const val EMPTY_NAME = "empty_name"

    /** The code at the start of a message such as `not_found: no such folder or item`, or empty if there is none. */
    fun kindOf(message: String?): String {
        val head = message?.substringBefore(':', "")?.trim().orEmpty()
        return if (head.isNotEmpty() && head.all { it in 'a'..'z' || it == '_' }) head else ""
    }
}
