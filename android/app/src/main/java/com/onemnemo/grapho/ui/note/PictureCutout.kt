package com.onemnemo.grapho.ui.note

/** The white band drawn around what a background removal keeps. [code] is what the engine is told. */
internal enum class CutoutOutline(val code: Int) { None(0), Thin(1), Thick(2) }

/**
 * How the background of a picture is taken out. [strength] is 0 to 100, and [DEFAULT_STRENGTH] is the usual cut.
 */
internal data class CutoutOptions(
    val strength: Int = DEFAULT_STRENGTH,
    val outline: CutoutOutline = CutoutOutline.None,
) {
    companion object {
        const val DEFAULT_STRENGTH = 50
        const val MAX_STRENGTH = 100
    }
}
