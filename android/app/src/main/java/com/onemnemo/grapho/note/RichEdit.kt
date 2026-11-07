package com.onemnemo.grapho.note

import android.text.Editable
import android.text.Spanned

/**
 * What the writer's selection or cursor looks like: the [marks] all of it carries, the [colour], [highlight] and
 * [size] when one value holds across it, and the [kind] of the line it starts in. The format bar lights its buttons
 * from this.
 */
internal data class FormatState(
    val marks: Int = 0,
    val colour: Int? = null,
    val highlight: Int? = null,
    val size: Float? = null,
    val kind: Int = ParaKinds.BODY,
    val hasSelection: Boolean = false,
)

/** Changes to the spans of an open text box: marks over a selection or at the cursor, and the kind of a line. */
internal object RichEdit {
    private const val EXCLUSIVE = RichSpans.EXCLUSIVE
    private const val GROWING = RichSpans.GROWING
    private const val POINT = Spanned.SPAN_INCLUSIVE_INCLUSIVE
    private const val END_MASK = 0x0F
    private const val END_GROWS = 0x02

    private fun spansIn(e: Spanned, a: Int, b: Int, test: (Any) -> Boolean): List<Any> =
        e.getSpans(a, b, Any::class.java)
            .filter { test(it) && e.getSpanFlags(it) and RichSpans.SPAN_COMPOSING == 0 }

    // Whether a span applies to what is typed at [at]: it lies on the cursor, or it reaches it and grows there.
    private fun reaches(e: Spanned, span: Any, at: Int): Boolean {
        val start = e.getSpanStart(span)
        val end = e.getSpanEnd(span)
        val grows = e.getSpanFlags(span) and END_MASK == END_GROWS
        return if (start == end) start == at else start < at && (end > at || (end == at && grows))
    }

    // Whether every character from [a] to [b] has a span that [test] accepts, or at a cursor, whether typing would.
    private fun covered(e: Spanned, a: Int, b: Int, test: (Any) -> Boolean): Boolean {
        val spans = spansIn(e, a, b, test)
        if (a == b) return spans.any { reaches(e, it, a) }
        var at = a
        while (at < b) {
            val here = at
            at = spans.filter { e.getSpanStart(it) <= here && e.getSpanEnd(it) > here }
                .maxOfOrNull { e.getSpanEnd(it) } ?: return false
        }
        return true
    }

    private fun <T : Any> single(e: Spanned, a: Int, b: Int, cls: Class<T>, value: (T) -> Any): Any? {
        val found = spansIn(e, a, b) { cls.isInstance(it) }
        if (a == b) return found.firstOrNull { reaches(e, it, a) }?.let { value(cls.cast(it)!!) }
        if (!covered(e, a, b) { cls.isInstance(it) }) return null
        return found.map { value(cls.cast(it)!!) }.toSet().singleOrNull()
    }

    /** What the selection from [a] to [b] looks like. */
    fun state(e: Spanned, a: Int, b: Int): FormatState {
        var marks = 0
        for (bit in RichSpans.MARK_BITS) {
            if (covered(e, a, b) { RichSpans.bitOf(it) and bit != 0 }) marks = marks or bit
        }
        val kind = paraSpanAt(e, a)?.kind ?: ParaKinds.BODY
        return FormatState(
            marks = marks,
            colour = single(e, a, b, InkSpan::class.java) { it.argb } as Int?,
            highlight = single(e, a, b, MarkerSpan::class.java) { it.argb } as Int?,
            size = single(e, a, b, SizeSpan::class.java) { it.ratio } as Float?,
            kind = kind,
            hasSelection = a != b,
        )
    }

    private fun paraSpanAt(e: Spanned, at: Int): ParaSpan? = e.getSpans(at, at, ParaSpan::class.java)
        .minByOrNull { e.getSpanStart(it) }

    // Takes the spans [test] accepts out of [a] to [b]. What lies outside stays, as the same span on the left and as
    // [again] on the right.
    private fun cut(e: Editable, a: Int, b: Int, test: (Any) -> Boolean, again: (Any) -> Any) {
        for (span in spansIn(e, a, b, test)) {
            val start = e.getSpanStart(span)
            val end = e.getSpanEnd(span)
            val flags = e.getSpanFlags(span)
            e.removeSpan(span)
            if (start < a) e.setSpan(span, start, a, EXCLUSIVE)
            if (end > b) e.setSpan(again(span), b, end, flags)
        }
    }

    /** Turns [bit] on over the selection, or off if all of it has it. At a cursor it applies to what comes next. */
    fun toggle(e: Editable, bit: Int, a: Int, b: Int) = set(
        e,
        bit,
        a,
        b,
        !covered(e, a, b) {
            RichSpans.bitOf(it) and
                bit !=
                0
        },
    )

    /** Turns [bit] on or off over the selection, or for what comes next at a cursor. */
    fun set(e: Editable, bit: Int, a: Int, b: Int, on: Boolean) {
        val test: (Any) -> Boolean = { RichSpans.bitOf(it) and bit != 0 }
        val has = covered(e, a, b, test)
        if (a != b) {
            if (on && has) return
            cut(e, a, b, test) { RichSpans.markSpan(bit) }
            if (on) e.setSpan(RichSpans.markSpan(bit), a, b, GROWING)
            return
        }
        if (has == on) return
        if (on) {
            e.setSpan(RichSpans.markSpan(bit), a, a, POINT)
            return
        }
        for (span in spansIn(e, a, a, test).filter { reaches(e, it, a) }) {
            val start = e.getSpanStart(span)
            val end = e.getSpanEnd(span)
            val flags = e.getSpanFlags(span)
            e.removeSpan(span)
            if (start < a) e.setSpan(span, start, a, EXCLUSIVE)
            if (end > a) e.setSpan(RichSpans.markSpan(bit), a, end, flags)
        }
    }

    /** Colours the selection with [argb], shown as [shown], or what comes next at a cursor. Null takes it off. */
    fun colour(e: Editable, argb: Int?, shown: Int, a: Int, b: Int) =
        valued(e, a, b, { it is InkSpan }, { argb?.let { c -> InkSpan(c, shown) } })

    /** Puts a marker colour behind the selection, or takes it off with null. */
    fun highlight(e: Editable, argb: Int?, a: Int, b: Int) =
        valued(e, a, b, { it is MarkerSpan }, { argb?.let { c -> MarkerSpan(c) } })

    /** Sets the selection to [points] in a box of [boxSize] points, or takes the size off with null. */
    fun size(e: Editable, points: Float?, boxSize: Float, a: Int, b: Int) =
        valued(e, a, b, { it is SizeSpan }, { points?.let { p -> SizeSpan(p / boxSize) } })

    private fun valued(e: Editable, a: Int, b: Int, test: (Any) -> Boolean, make: () -> Any?) {
        cut(e, a, b, test) { old ->
            when (old) {
                is InkSpan -> InkSpan(old.argb, old.foregroundColor)
                is MarkerSpan -> MarkerSpan(old.argb)
                is SizeSpan -> SizeSpan(old.ratio)
                else -> old
            }
        }
        val span = make() ?: return
        e.setSpan(span, a, b, if (a == b) POINT else GROWING)
    }

    fun touched(e: Editable, a: Int, b: Int): List<Int> {
        val lines = RichSpans.lineRanges(e)
        val low = minOf(a, b)
        val high = maxOf(a, b)
        return lines.indices.filter { lines[it][0] <= high && lines[it][1] >= low }
    }

    fun spanOfLine(e: Editable, line: Int): ParaSpan? {
        val start = RichSpans.lineRanges(e)[line][0]
        return e.getSpans(start, start, ParaSpan::class.java).firstOrNull { e.getSpanStart(it) == start }
    }

    /** Gives every line between [a] and [b] the kind [kind], or plain if they all have it already. */
    fun setKind(e: Editable, kind: Int, a: Int, b: Int, em: Float) {
        val lines = touched(e, a, b)
        val spans = lines.map { spanOfLine(e, it) }
        val all = spans.all { it?.kind == kind }
        for (span in spans) {
            span ?: continue
            span.kind = if (all) ParaKinds.BODY else kind
            span.checked = false
            if (!ParaKinds.isList(span.kind)) span.indent = 0
        }
        fix(e, em)
    }

    /** Makes line [line] the kind [kind], with [indent] and [checked] if it is a list line. */
    fun setPara(e: Editable, line: Int, kind: Int, indent: Int, checked: Boolean) {
        val span = spanOfLine(e, line) ?: return
        span.kind = kind
        span.indent = if (ParaKinds.isList(kind)) indent else 0
        span.checked = checked && kind == ParaKinds.CHECK
    }

    /** Moves the list lines between [a] and [b] [by] steps. Other lines stay where they are. */
    fun indent(e: Editable, by: Int, a: Int, b: Int, em: Float) {
        for (line in touched(e, a, b)) {
            val span = spanOfLine(e, line) ?: continue
            if (ParaKinds.isList(span.kind)) span.indent = (span.indent + by).coerceIn(0, ParaKinds.MAX_INDENT)
        }
        fix(e, em)
    }

    /** Ticks or unticks the checklist line [line]. */
    fun tick(e: Editable, line: Int, em: Float) {
        val span = spanOfLine(e, line) ?: return
        if (span.kind == ParaKinds.CHECK) span.checked = !span.checked
        fix(e, em)
    }

    /**
     * Makes each line carry exactly one paragraph span after the text changed. A line made by a line break takes the
     * kind of the line it came from, a line that was joined to another keeps the first one's, and the headings and
     * the numbers are worked out again.
     */
    fun fix(e: Editable, em: Float) {
        val lines = RichSpans.lineRanges(e)
        val existing = e.getSpans(0, e.length, ParaSpan::class.java).sortedBy { e.getSpanStart(it) }
        val used = HashSet<ParaSpan>()
        var before: ParaSpan? = null
        for (line in lines) {
            val start = line[0]
            val end = if (line[1] < e.length) line[1] + 1 else line[1]
            val owner = existing.firstOrNull {
                it !in used && e.getSpanStart(it) <= start &&
                    (e.getSpanEnd(it) > start || (e.getSpanEnd(it) == start && start == e.length))
            }
            val span = owner ?: ParaSpan(
                before?.kind?.takeUnless { ParaKinds.isHeading(it) } ?: ParaKinds.BODY,
                before?.indent ?: 0,
                false,
                em,
            )
            used.add(span)
            before = span
            e.removeSpan(span)
            e.setSpan(span, start, end, Spanned.SPAN_PARAGRAPH)
        }
        for (stale in existing) if (stale !in used) e.removeSpan(stale)
        val rich = RichSpans.read(e, 1f)
        for (i in lines.indices) {
            spanOfLine(e, i)?.let {
                it.number = rich.numberAt(i)
                it.em = em
            }
        }
        RichSpans.decorate(e)
    }
}
