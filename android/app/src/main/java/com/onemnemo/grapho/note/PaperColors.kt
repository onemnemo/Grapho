package com.onemnemo.grapho.note

import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import kotlin.math.abs

/** A named paper colour (opaque ARGB) offered when choosing a page colour. */
class PaperColor(@StringRes val nameRes: Int, val argb: Int)

/** The paper colours offered to the person. Black is pure so an OLED panel can switch off. */
object PaperColors {
    val WHITE = PaperColor(R.string.paper_colour_white, 0xFFFFFFFF.toInt())
    val CREAM = PaperColor(R.string.paper_colour_cream, 0xFFF6F0E1.toInt())
    val SLATE = PaperColor(R.string.paper_colour_slate, 0xFF3A3B40.toInt())
    val DARK = PaperColor(R.string.paper_colour_dark, 0xFF1C1D21.toInt())
    val BLACK = PaperColor(R.string.paper_colour_black, 0xFF000000.toInt())

    val presets: List<PaperColor> = listOf(WHITE, CREAM, SLATE, DARK, BLACK)
}

/** Colours around and on the page that follow the paper: desk, page edge, pattern and the add-page target. */
object PaperChrome {
    private const val LIGHT_DESK = 0xFFEEECE8.toInt()
    private const val LIGHT_EDGE = 0xFFDAD7D0.toInt()
    private const val LIGHT_PATTERN_INK = 0xFF6C768E.toInt()
    private const val LIGHT_ADD_FILL = 0xFFE6E4DF.toInt()
    private const val LIGHT_ADD_STROKE = 0xFFC9C6BF.toInt()
    private const val LIGHT_ADD_PLUS = 0xFF7C808A.toInt()
    private const val DARK_ADD_PLUS = 0xFF9A9EA8.toInt()
    private const val LIGHT_IMAGE_FILL = 0xFFE4E2DC.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    fun desk(paper: Int): Int = if (isDarkPaper(paper)) mix(paper, BLACK, 0.5f) else LIGHT_DESK

    fun edge(paper: Int): Int = if (isDarkPaper(paper)) mix(paper, WHITE, 0.14f) else LIGHT_EDGE

    /** Zero (transparent) on dark paper, where a shadow would only muddy the desk. */
    fun shadow(paper: Int): Int = if (isDarkPaper(paper)) 0 else 0x14000000

    /** A subtle line or dot colour, tinted by the paper so cream stays warm. */
    fun pattern(paper: Int): Int =
        if (isDarkPaper(paper)) mix(paper, WHITE, 0.18f) else mix(paper, LIGHT_PATTERN_INK, 0.3f)

    fun addFill(paper: Int): Int = if (isDarkPaper(paper)) mix(desk(paper), WHITE, 0.08f) else LIGHT_ADD_FILL

    fun addStroke(paper: Int): Int = if (isDarkPaper(paper)) mix(desk(paper), WHITE, 0.2f) else LIGHT_ADD_STROKE

    fun addPlus(paper: Int): Int = if (isDarkPaper(paper)) DARK_ADD_PLUS else LIGHT_ADD_PLUS

    /** What an image shows in place of its picture until that is decoded. */
    fun imageFill(paper: Int): Int = if (isDarkPaper(paper)) mix(paper, WHITE, 0.12f) else LIGHT_IMAGE_FILL

    /** Opaque blend of [from] toward [to] by [t] (0 to 1). */
    private fun mix(from: Int, to: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t + 0.5f).toInt()
        }
        return BLACK or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

/** Hue (degrees), saturation and lightness (both 0 to 1) of a colour, for the custom paper picker. */
class Hsl(val hue: Float, val saturation: Float, val lightness: Float) {
    /** The opaque ARGB colour. */
    fun toArgb(): Int {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val h = (hue % 360f + 360f) % 360f / 60f
        val x = c * (1f - abs(h % 2f - 1f))
        val m = lightness - c / 2f
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun byte(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (byte(r) shl 16) or (byte(g) shl 8) or byte(b)
    }

    companion object {
        fun from(argb: Int): Hsl {
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val hi = maxOf(r, g, b)
            val lo = minOf(r, g, b)
            val l = (hi + lo) / 2f
            val d = hi - lo
            if (d == 0f) return Hsl(0f, 0f, l)
            val s = d / (1f - abs(2f * l - 1f))
            val h = when (hi) {
                r -> ((g - b) / d + 6f) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            }
            return Hsl(h * 60f, s, l)
        }
    }
}
