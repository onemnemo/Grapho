package com.onemnemo.grapho.note

/** A piece of a text box: plain words, or LaTeX math written between dollar signs. */
internal sealed interface MathRun {
    data class Plain(val text: String) : MathRun

    /** Math in `$...$`, set in the line, or in `$$...$$`, set on a line of its own when [display]. */
    data class Math(val latex: String, val display: Boolean) : MathRun
}

/**
 * Splits the text of a box into plain and math runs, the way Markdown editors read dollar signs. `$$...$$` is display
 * math. `$...$` is inline math when the first dollar has no space after it, the last has no space before it and no
 * digit after it, and both sit on one line, so "costs $5 and $10" stays plain. `\$` is a plain dollar sign. A dollar
 * with no partner stays as it is.
 */
internal object MathRuns {
    /** True when [text] could hold math at all, so plain boxes skip the work. */
    fun mayHaveMath(text: String): Boolean = text.indexOf('$') >= 0

    fun split(text: String): List<MathRun> {
        if (!mayHaveMath(text)) return listOf(MathRun.Plain(text))
        val runs = ArrayList<MathRun>()
        val plain = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length && text[i + 1] == '$') {
                plain.append('$')
                i += 2
                continue
            }
            if (c != '$') {
                plain.append(c)
                i++
                continue
            }
            val twin = text.startsWith("$$", i)
            val math = if (twin) display(text, i) else inline(text, i)
            if (math == null) {
                val skip = if (twin) 2 else 1
                plain.append(text, i, i + skip)
                i += skip
                continue
            }
            if (plain.isNotEmpty()) {
                runs.add(MathRun.Plain(plain.toString()))
                plain.clear()
            }
            runs.add(math.first)
            i = math.second
        }
        if (plain.isNotEmpty() || runs.isEmpty()) runs.add(MathRun.Plain(plain.toString()))
        return runs
    }

    /** The display math opening at [start] and the index just past it, or null when it never closes or is empty. */
    private fun display(text: String, start: Int): Pair<MathRun, Int>? {
        val end = closing(text, start + 2, "$$") ?: return null
        val latex = text.substring(start + 2, end).trim()
        if (latex.isEmpty()) return null
        return MathRun.Math(latex, display = true) to end + 2
    }

    /** The inline math opening at [start] and the index just past it, or null when it is not math. */
    private fun inline(text: String, start: Int): Pair<MathRun, Int>? {
        val first = start + 1
        if (first >= text.length || text[first].isWhitespace()) return null
        var i = first
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\n' -> return null

                c == '\\' -> i++

                c == '$' -> {
                    val closes = !text[i - 1].isWhitespace() && (i + 1 >= text.length || !text[i + 1].isDigit())
                    if (closes) return MathRun.Math(text.substring(first, i), display = false) to i + 1
                }
            }
            i++
        }
        return null
    }

    /** Where [mark] next appears from [from], stepping over escaped characters. */
    private fun closing(text: String, from: Int, mark: String): Int? {
        var i = from
        while (i < text.length) {
            if (text[i] == '\\') {
                i += 2
                continue
            }
            if (text.startsWith(mark, i)) return i
            i++
        }
        return null
    }
}
