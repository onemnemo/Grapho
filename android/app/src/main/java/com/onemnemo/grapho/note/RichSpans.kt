package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import android.text.style.ParagraphStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import kotlin.math.roundToInt

/** A text colour picked for a stretch of text. [argb] is the colour as picked, [shown] the one that reads on the paper. */
internal class InkSpan(val argb: Int, shown: Int) : ForegroundColorSpan(shown)

/** A marker colour behind a stretch of text. */
internal class MarkerSpan(val argb: Int) : BackgroundColorSpan(argb)

/** A font size for a stretch of text, as a [ratio] of the size of the box. */
internal class SizeSpan(val ratio: Float) : RelativeSizeSpan(ratio)

/** The bigger, bolder look of a heading line. It is not a choice of the writer, so it is never saved as one. */
internal class HeadingLook(private val scale: Float) : MetricAffectingSpan() {
    override fun updateMeasureState(paint: TextPaint) = change(paint)

    override fun updateDrawState(paint: TextPaint) = change(paint)

    private fun change(paint: TextPaint) {
        paint.textSize *= scale
        val italic = paint.typeface?.isItalic == true
        paint.typeface = Typeface.create(paint.typeface, if (italic) Typeface.BOLD_ITALIC else Typeface.BOLD)
    }
}

/**
 * What a line is: a plain one, a heading, a quote or a list line, with how far it is indented. It makes room at the
 * start of the line and draws the bullet, number, tick box or quote bar there. [em] is the size of the box's text in
 * the unit it is drawn in, which the room and the marks are measured in, and [number] what a numbered line shows.
 */
internal class ParaSpan(var kind: Int, var indent: Int, var checked: Boolean, var em: Float, var number: Int = 1) :
    LeadingMarginSpan,
    ParagraphStyle {
    private val gutter: Float
        get() = when {
            ParaKinds.isList(kind) -> LIST_GUTTER
            kind == ParaKinds.QUOTE -> QUOTE_GUTTER
            else -> 0f
        }

    private val stepped: Float
        get() = if (ParaKinds.isList(kind)) indent * INDENT_STEP else 0f

    /** How far from the start of the line the text begins, in the unit of [em]. */
    fun margin(): Float = em * (stepped + gutter)

    override fun getLeadingMargin(first: Boolean): Int = margin().roundToInt()

    @Suppress("LongParameterList")
    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) {
        val oldStyle = p.style
        val oldAlpha = p.alpha
        val oldWidth = p.strokeWidth
        val oldAlign = p.textAlign
        val left = x + dir * em * stepped
        val centre = left + dir * em * gutter * MARK_CENTRE
        val cy = baseline - em * MARK_RISE
        when {
            kind == ParaKinds.QUOTE -> {
                p.style = Paint.Style.FILL
                p.alpha = QUOTE_ALPHA
                val bar = em * QUOTE_BAR
                val edge = x + dir * em * QUOTE_INSET
                c.drawRect(
                    minOf(edge, edge + dir * bar),
                    top.toFloat(),
                    maxOf(edge, edge + dir * bar),
                    bottom.toFloat(),
                    p,
                )
            }

            !first -> Unit

            kind == ParaKinds.BULLET -> {
                p.style = Paint.Style.FILL
                c.drawCircle(centre, cy, em * BULLET_RADIUS, p)
            }

            kind == ParaKinds.NUMBERED -> {
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                c.drawText("$number.", centre, baseline.toFloat(), p)
            }

            kind == ParaKinds.CHECK -> drawBox(c, p, centre, cy)
        }
        p.style = oldStyle
        p.alpha = oldAlpha
        p.strokeWidth = oldWidth
        p.textAlign = oldAlign
    }

    private fun drawBox(c: Canvas, p: Paint, cx: Float, cy: Float) {
        val half = em * BOX_HALF
        p.style = Paint.Style.STROKE
        p.strokeWidth = (em * BOX_STROKE).coerceAtLeast(1f)
        c.drawRoundRect(cx - half, cy - half, cx + half, cy + half, half * BOX_ROUND, half * BOX_ROUND, p)
        if (checked) {
            val tick = floatArrayOf(
                cx - half * TICK_X,
                cy,
                cx - half * TICK_DIP,
                cy + half * TICK_Y,
                cx - half * TICK_DIP,
                cy + half * TICK_Y,
                cx + half * TICK_X,
                cy - half * TICK_Y,
            )
            c.drawLines(tick, p)
        }
    }

    companion object {
        /** How far a tick box counts as touched either side of its middle, in ems, for [gutterEnd]. */
        const val LIST_GUTTER = 1.6f
        const val QUOTE_GUTTER = 0.9f
        const val INDENT_STEP = 1.5f
        private const val MARK_CENTRE = 0.42f
        private const val MARK_RISE = 0.3f
        private const val BULLET_RADIUS = 0.12f
        private const val BOX_HALF = 0.4f
        private const val BOX_STROKE = 0.08f
        private const val BOX_ROUND = 0.3f
        private const val TICK_X = 0.6f
        private const val TICK_Y = 0.6f
        private const val TICK_DIP = 0.1f
        private const val QUOTE_BAR = 0.12f
        private const val QUOTE_INSET = 0.2f
        private const val QUOTE_ALPHA = 140

        /** Where the marks of a line end, in ems from the start of the line, as [x] to touch for a tick box. */
        fun gutterEnd(indent: Int): Float = indent * INDENT_STEP + LIST_GUTTER
    }
}

/**
 * Turns a box's words and formatting into spans and back. The same spans are drawn by the page and edited in the
 * field, so what is typed looks like what is saved. Offsets in the engine count characters, and those in text count
 * UTF-16 units; [Offsets] turns one into the other.
 */
internal object RichSpans {
    private val HEADING_SCALE = floatArrayOf(1f, HEADING1_SCALE, HEADING2_SCALE, HEADING3_SCALE)
    private const val HEADING1_SCALE = 1.7f
    private const val HEADING2_SCALE = 1.4f
    private const val HEADING3_SCALE = 1.18f
    const val SPAN_COMPOSING = 0x100

    const val EXCLUSIVE = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    const val GROWING = Spanned.SPAN_EXCLUSIVE_INCLUSIVE

    /** Character offsets of the engine against UTF-16 offsets of a string. */
    class Offsets(private val text: CharSequence) {
        private val flat = text.length == Character.codePointCount(text, 0, text.length)

        fun unit(point: Int): Int {
            if (flat) return point.coerceIn(0, text.length)
            return Character.offsetByCodePoints(
                text,
                0,
                point.coerceIn(0, Character.codePointCount(text, 0, text.length)),
            )
        }

        fun point(unit: Int): Int = if (flat) unit else Character.codePointCount(text, 0, unit)
    }

    /** The start of each line of [text], and for each the end before its line break. */
    fun lineRanges(text: CharSequence): List<IntArray> {
        val out = ArrayList<IntArray>()
        var start = 0
        for (i in text.indices) {
            if (text[i] == '\n') {
                out.add(intArrayOf(start, i))
                start = i + 1
            }
        }
        out.add(intArrayOf(start, text.length))
        return out
    }

    fun markSpan(bit: Int): Any = when (bit) {
        Marks.BOLD -> StyleSpan(Typeface.BOLD)
        Marks.ITALIC -> StyleSpan(Typeface.ITALIC)
        Marks.UNDERLINE -> UnderlineSpan()
        Marks.STRIKE -> StrikethroughSpan()
        Marks.CODE -> TypefaceSpan("monospace")
        Marks.SUPER -> SuperscriptSpan()
        else -> SubscriptSpan()
    }

    val MARK_BITS = intArrayOf(
        Marks.BOLD,
        Marks.ITALIC,
        Marks.UNDERLINE,
        Marks.STRIKE,
        Marks.CODE,
        Marks.SUPER,
        Marks.SUB,
    )

    fun bitOf(span: Any): Int = when {
        span is StyleSpan -> (if (span.style and Typeface.BOLD != 0) Marks.BOLD else 0) or
            (if (span.style and Typeface.ITALIC != 0) Marks.ITALIC else 0)

        span is UnderlineSpan -> Marks.UNDERLINE

        span is StrikethroughSpan -> Marks.STRIKE

        span is TypefaceSpan && span.family == "monospace" -> Marks.CODE

        span is SuperscriptSpan -> Marks.SUPER

        span is SubscriptSpan -> Marks.SUB

        else -> 0
    }

    fun isMarkSpan(span: Any, flags: Int): Boolean =
        flags and SPAN_COMPOSING == 0 && (bitOf(span) != 0 || span is InkSpan || span is MarkerSpan || span is SizeSpan)

    /**
     * The words and formatting as spans, ready for a layout or a field. [boxSize] is the size of the box in points and
     * [em] that size in the unit drawn in. [editing] makes the marks grow as text is typed at their end.
     */
    @Suppress("LongParameterList")
    fun build(
        text: String,
        rich: RichText,
        boxSize: Float,
        em: Float,
        paper: Int,
        editing: Boolean,
    ): SpannableStringBuilder {
        val out = SpannableStringBuilder(text)
        applyTo(out, rich, boxSize, em, paper, editing)
        return out
    }

    /** Lays [rich] over the words already in [out]. */
    @Suppress("LongParameterList")
    fun applyTo(out: Spannable, rich: RichText, boxSize: Float, em: Float, paper: Int, editing: Boolean) {
        val offsets = Offsets(out)
        val flags = if (editing) GROWING else EXCLUSIVE
        for (s in rich.spans) {
            val a = offsets.unit(s.start)
            val b = offsets.unit(s.end)
            if (a >= b) continue
            for (bit in MARK_BITS) if (s.marks and bit != 0) out.setSpan(markSpan(bit), a, b, flags)
            s.color?.let { out.setSpan(InkSpan(it, displayColor(it, paper)), a, b, flags) }
            s.highlight?.let { out.setSpan(MarkerSpan(it), a, b, flags) }
            s.size?.let { out.setSpan(SizeSpan(it / boxSize), a, b, flags) }
        }
        for ((i, line) in lineRanges(out).withIndex()) {
            val p = rich.para(i)
            val end = if (line[1] < out.length) line[1] + 1 else line[1]
            out.setSpan(
                ParaSpan(p.kind, p.indent, p.checked, em, rich.numberAt(i)),
                line[0],
                end,
                Spanned.SPAN_PARAGRAPH,
            )
        }
        decorate(out)
    }

    /** Gives the heading lines their bigger look, and takes it off any line that is no longer one. */
    fun decorate(text: Spannable) {
        for (old in text.getSpans(0, text.length, HeadingLook::class.java)) text.removeSpan(old)
        for (span in text.getSpans(0, text.length, ParaSpan::class.java)) {
            if (!ParaKinds.isHeading(span.kind)) continue
            text.setSpan(
                HeadingLook(HEADING_SCALE[span.kind]),
                text.getSpanStart(span),
                text.getSpanEnd(span),
                EXCLUSIVE,
            )
        }
    }

    /** The formatting held by [text]'s spans, for the engine. [boxSize] is the size of the box in points. */
    fun read(text: Spanned, boxSize: Float): RichText {
        val offsets = Offsets(text)
        val edges = sortedSetOf(0, text.length)
        val marks = text.getSpans(0, text.length, Any::class.java).filter {
            isMarkSpan(it, text.getSpanFlags(it))
        }
        for (m in marks) {
            edges.add(text.getSpanStart(m).coerceIn(0, text.length))
            edges.add(text.getSpanEnd(m).coerceIn(0, text.length))
        }
        val points = edges.toList()
        val spans = ArrayList<RichSpan>()
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            if (a >= b) continue
            var bits = 0
            var colour: Int? = null
            var highlight: Int? = null
            var size: Float? = null
            for (m in marks) {
                if (text.getSpanStart(m) > a || text.getSpanEnd(m) < b) continue
                bits = bits or bitOf(m)
                when (m) {
                    is InkSpan -> colour = m.argb
                    is MarkerSpan -> highlight = m.argb
                    is SizeSpan -> size = m.ratio * boxSize
                }
            }
            if (bits == 0 && colour == null && highlight == null && size == null) continue
            spans.add(RichSpan(offsets.point(a), offsets.point(b), bits, colour, highlight, size))
        }
        val byStart = text.getSpans(0, text.length, ParaSpan::class.java).associateBy { text.getSpanStart(it) }
        val paras = lineRanges(text).map { line ->
            val p = byStart[line[0]]
            RichPara(p?.kind ?: ParaKinds.BODY, p?.indent ?: 0, p?.checked == true && p.kind == ParaKinds.CHECK)
        }
        return RichText(spans, paras.dropLastWhile { it == RichPara() })
    }

    /** Tells the line starts that the text is now [em] big, in whatever unit the field draws in. */
    fun setEm(text: Spanned, em: Float) {
        for (span in text.getSpans(0, text.length, ParaSpan::class.java)) span.em = em
    }
}
