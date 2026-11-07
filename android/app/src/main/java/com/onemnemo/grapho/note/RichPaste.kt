package com.onemnemo.grapho.note

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan

/** Words and formatting brought in from outside, in the same terms a text box saves them. */
internal data class Pasted(val text: String, val rich: RichText)

/**
 * Reads text from the clipboard into a [Pasted]. What a web page or document carries is kept when a box can show it:
 * bold, italic, underline, strikethrough, code, colours, headings, lists and quotes. Font sizes in pixels, fonts and
 * the page's own spacing are dropped, so a paste always takes the size of the box it lands in. Lines that begin like
 * Markdown, such as `- `, `1. ` or `# `, become what they say.
 */
internal object RichPaste {
    private const val BIG = 1.15f
    private const val HEADING1_AT = 1.6f
    private const val HEADING2_AT = 1.3f
    private const val GREY_SPREAD = 40
    private const val INDENT_SPACES = 2
    private const val OPAQUE = 0xFF000000.toInt()
    private val NUMBERED = Regex("^\\d{1,3}[.)] +")
    private val BULLETS = listOf("- ", "* ", "• ", "– ")
    private val HEADINGS = listOf("# ", "## ", "### ")

    /** A mark found on a stretch of the text, in UTF-16 offsets. */
    private data class Found(val start: Int, val end: Int, val span: RichSpan)

    /** The text of [styled] with what it carries that a box can show. */
    fun fromStyled(styled: CharSequence): Pasted {
        val clean = StringBuilder()
        val into = IntArray(styled.length + 1)
        for (i in styled.indices) {
            into[i] = clean.length
            if (styled[i] != '\r') clean.append(styled[i])
        }
        into[styled.length] = clean.length
        val text = clean.toString().trimEnd('\n', ' ')
        val lines = RichSpans.lineRanges(text)
        val found = ArrayList<Found>()
        val kinds = HashMap<Int, RichPara>()
        if (styled is Spanned) {
            for (span in styled.getSpans(0, styled.length, Any::class.java)) {
                val a = into[styled.getSpanStart(span)].coerceIn(0, text.length)
                val b = into[styled.getSpanEnd(span)].coerceIn(0, text.length)
                if (a >= b) continue
                markOf(span)?.let { found.add(Found(a, b, it)) }
                val line = lines.indexOfLast { it[0] <= a }.coerceAtLeast(0)
                paraOf(span, a, b, lines[line])?.let { kinds.putIfAbsent(line, it) }
            }
        }
        return structured(text, found, lines.indices.map { kinds[it] ?: RichPara() })
    }

    /** The plain [text], with Markdown looking lines turned into what they say. */
    fun fromPlain(text: String): Pasted {
        val clean = text.replace("\r", "").trimEnd('\n', ' ')
        return structured(clean, emptyList(), RichSpans.lineRanges(clean).map { RichPara() })
    }

    private fun markOf(span: Any): RichSpan? {
        val bits = when {
            span is StyleSpan -> (if (span.style and Typeface.BOLD != 0) Marks.BOLD else 0) or
                (if (span.style and Typeface.ITALIC != 0) Marks.ITALIC else 0)

            span is UnderlineSpan -> Marks.UNDERLINE

            span is StrikethroughSpan -> Marks.STRIKE

            span is TypefaceSpan && span.family == "monospace" -> Marks.CODE

            span is SuperscriptSpan -> Marks.SUPER

            span is SubscriptSpan -> Marks.SUB

            else -> 0
        }
        return when {
            bits != 0 -> RichSpan(0, 0, bits)

            span is ForegroundColorSpan && !grey(span.foregroundColor) ->
                RichSpan(0, 0, color = span.foregroundColor or OPAQUE)

            span is BackgroundColorSpan && !grey(span.backgroundColor) ->
                RichSpan(0, 0, highlight = span.backgroundColor or OPAQUE)

            else -> null
        }
    }

    private fun grey(argb: Int): Boolean {
        val hi = maxOf(Color.red(argb), Color.green(argb), Color.blue(argb))
        val lo = minOf(Color.red(argb), Color.green(argb), Color.blue(argb))
        return hi - lo < GREY_SPREAD
    }

    // A line is a heading when a bigger size runs the whole way along it; bullets and quotes say so directly.
    private fun paraOf(span: Any, a: Int, b: Int, line: IntArray): RichPara? = when {
        span is BulletSpan -> RichPara(ParaKinds.BULLET)

        span is QuoteSpan -> RichPara(ParaKinds.QUOTE)

        span is RelativeSizeSpan && span.sizeChange >= BIG && a <= line[0] && b >= line[1] -> RichPara(
            when {
                span.sizeChange >= HEADING1_AT -> ParaKinds.HEADING1
                span.sizeChange >= HEADING2_AT -> ParaKinds.HEADING2
                else -> ParaKinds.HEADING3
            },
        )

        else -> null
    }

    // Takes the Markdown looking start off each line that is still plain, gives the line the kind it means, and
    // moves the marks to where their words now are.
    private fun structured(text: String, found: List<Found>, kinds: List<RichPara>): Pasted {
        val lines = RichSpans.lineRanges(text)
        val out = StringBuilder()
        val into = IntArray(text.length + 1)
        val paras = ArrayList<RichPara>()
        for ((i, line) in lines.withIndex()) {
            var para = kinds[i]
            var cut = 0
            if (para.kind == ParaKinds.BODY) {
                val whole = text.substring(line[0], line[1])
                val lead = whole.takeWhile { it == ' ' }.length
                val rest = whole.substring(lead)
                val indent = (lead / INDENT_SPACES).coerceAtMost(ParaKinds.MAX_INDENT)
                val heading = HEADINGS.indexOfFirst { rest.startsWith(it) }
                val number = NUMBERED.find(rest)?.value
                val bullet = BULLETS.firstOrNull { rest.startsWith(it) }
                val check = CHECKS.firstOrNull { rest.startsWith(it) }
                val mark = when {
                    check != null -> check.also { para = RichPara(ParaKinds.CHECK, indent, check == CHECKS[1]) }
                    bullet != null -> bullet.also { para = RichPara(ParaKinds.BULLET, indent) }
                    number != null -> number.also { para = RichPara(ParaKinds.NUMBERED, indent) }
                    heading >= 0 -> HEADINGS[heading].also { para = RichPara(ParaKinds.HEADING1 + heading) }
                    rest.startsWith("> ") -> "> ".also { para = RichPara(ParaKinds.QUOTE) }
                    else -> null
                }
                if (mark != null) cut = lead + mark.length
            }
            for (at in line[0]..line[1]) {
                into[at] = out.length + (at - line[0] - cut).coerceAtLeast(0)
            }
            out.append(text, line[0] + cut, line[1])
            if (i < lines.size - 1) {
                out.append('\n')
                into[line[1] + 1] = out.length
            }
            paras.add(para)
        }
        val shown = out.toString()
        val offsets = RichSpans.Offsets(shown)
        val spans = found.mapNotNull {
            val a = into[it.start]
            val b = into[it.end]
            if (b > a) it.span.copy(start = offsets.point(a), end = offsets.point(b)) else null
        }
        return Pasted(shown, RichText(spans, paras.dropLastWhile { it == RichPara() }))
    }

    private val CHECKS = listOf("[ ] ", "[x] ")
}
