package com.onemnemo.grapho.note

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val DARK_PAPER_BELOW = 0.4f
private const val NEAR_BLACK_BELOW = 0.06f
private const val NEAR_WHITE_ABOVE = 0.8f

/** The most colourful an ink can be (max channel minus min, 0 to 255) and still count as black or white. */
private const val MAX_CHROMA = 40

private const val LIGHT_INK = 0xECECEC
private const val DARK_INK = 0x1A1A1A

/** WCAG relative luminance of an opaque ARGB colour, 0 (black) to 1 (white). Alpha is ignored. */
fun luminance(argb: Int): Float {
    fun channel(shift: Int): Float {
        val c = ((argb shr shift) and 0xFF) / 255f
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(16) + 0.7152f * channel(8) + 0.0722f * channel(0)
}

/** True when ink needs to be light to read on [paperArgb]. */
fun isDarkPaper(paperArgb: Int): Boolean = luminance(paperArgb) < DARK_PAPER_BELOW

private fun chroma(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return max(r, max(g, b)) - min(r, min(g, b))
}

/**
 * The colour to draw ink with on a given paper. Near-black ink turns near-white on dark
 * paper and near-white ink turns near-black on light paper, so default pens stay legible.
 * Only what is drawn changes; the stored stroke colour is never rewritten. Alpha is kept.
 */
fun displayColor(inkArgb: Int, paperArgb: Int): Int {
    if (chroma(inkArgb) > MAX_CHROMA) return inkArgb
    val lum = luminance(inkArgb)
    val alpha = inkArgb and 0xFF000000.toInt()
    return when {
        isDarkPaper(paperArgb) && lum < NEAR_BLACK_BELOW -> alpha or LIGHT_INK
        !isDarkPaper(paperArgb) && lum > NEAR_WHITE_ABOVE -> alpha or DARK_INK
        else -> inkArgb
    }
}
