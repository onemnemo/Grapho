package com.onemnemo.grapho.note

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Which pages an export covers. */
sealed interface PageChoice {
    data object All : PageChoice

    data object Current : PageChoice

    /** [text] is what the person typed, such as `1-3, 5`. */
    data class Range(val text: String) : PageChoice
}

/** Pure arithmetic for export, kept apart from the drawing so it can be tested without a device. */
object ExportPlan {
    /** Image resolution: pixels per inch of the page. */
    private const val DPI = 300f

    /** Points per inch, since document units are points. */
    private const val POINTS_PER_INCH = 72f

    /** The longest side of an exported image, in pixels. */
    const val MAX_SIDE = 4096

    /** Clear space around the ink of an infinite canvas, in points. */
    const val INK_MARGIN = 24f

    /**
     * Zero-based page indices for a choice, in page order, or null when the choice is not valid: a range that is
     * empty, badly written, reversed or outside 1 to [pageCount]. [current] is the zero-based page in view.
     */
    fun pages(choice: PageChoice, pageCount: Int, current: Int): List<Int>? = when (choice) {
        PageChoice.All -> (0 until pageCount).toList()
        PageChoice.Current -> if (current in 0 until pageCount) listOf(current) else null
        is PageChoice.Range -> parseRange(choice.text, pageCount)
    }

    /** Parses `1-3, 5` (one-based, commas or semicolons between parts) into sorted, distinct zero-based indices. */
    fun parseRange(text: String, pageCount: Int): List<Int>? {
        val parts = text.split(',', ';').map { it.trim() }
        if (parts.all { it.isEmpty() }) return null
        val pages = sortedSetOf<Int>()
        for (part in parts) {
            if (part.isEmpty()) return null
            val ends = part.split('-').map { it.trim() }
            if (ends.size > 2) return null
            val first = pageNumber(ends[0], pageCount) ?: return null
            val last = if (ends.size == 2) pageNumber(ends[1], pageCount) ?: return null else first
            if (last < first) return null
            for (n in first..last) pages.add(n - 1)
        }
        return pages.toList()
    }

    private fun pageNumber(text: String, pageCount: Int): Int? {
        if (text.isEmpty() || text.length > MAX_DIGITS || !text.all { it in '0'..'9' }) return null
        return text.toInt().takeIf { it in 1..pageCount }
    }

    /** The pixel size of an exported image and the pixels per point it was drawn at. */
    class ImageSize(val width: Int, val height: Int, val scale: Float)

    /**
     * Image size for an area of [widthPt] by [heightPt] points: 300 dpi, scaled down so the longest side is at most
     * [MAX_SIDE] pixels. Never smaller than one pixel each way.
     */
    fun imageSize(widthPt: Float, heightPt: Float): ImageSize {
        val longest = max(widthPt, heightPt)
        val wanted = DPI / POINTS_PER_INCH
        val scale = if (longest > 0f) min(wanted, MAX_SIDE / longest) else wanted
        val w = (widthPt * scale).roundToInt().coerceIn(1, MAX_SIDE)
        val h = (heightPt * scale).roundToInt().coerceIn(1, MAX_SIDE)
        return ImageSize(w, h, scale)
    }

    /** A file name made from a title: characters that document providers reject become underscores. */
    fun safeName(title: String, fallback: String): String =
        title.trim().ifBlank { fallback }.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    /** `<title>.pdf`, the name a shared copy of the note goes out under. */
    fun pdfName(title: String, fallback: String): String = "${safeName(title, fallback)}.pdf"

    /** `<title> p<N>.png` for the zero-based [page]. */
    fun imageName(title: String, page: Int, fallback: String): String = "${safeName(title, fallback)} p${page + 1}.png"

    private const val MAX_DIGITS = 6
}
