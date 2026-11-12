package com.onemnemo.grapho.note

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The marks a stretch of text can carry. The bits are the engine's. */
internal object Marks {
    const val BOLD = 1
    const val ITALIC = 1 shl 1
    const val UNDERLINE = 1 shl 2
    const val STRIKE = 1 shl 3
    const val CODE = 1 shl 4
    const val SUPER = 1 shl 5
    const val SUB = 1 shl 6
}

/** What a line of a text box is. The codes are the engine's order. */
object ParaKinds {
    const val BODY = 0
    const val HEADING1 = 1
    const val HEADING2 = 2
    const val HEADING3 = 3
    const val BULLET = 4
    const val NUMBERED = 5
    const val CHECK = 6
    const val QUOTE = 7

    /** The names the file uses, by code. */
    val NAMES = listOf("body", "heading1", "heading2", "heading3", "bullet", "numbered", "check", "quote")

    fun isList(kind: Int) = kind == BULLET || kind == NUMBERED || kind == CHECK

    fun isHeading(kind: Int) = kind in HEADING1..HEADING3

    /** The most a line is indented, in steps. */
    const val MAX_INDENT = 6
}

/** The typefaces a box can use. The numbers are what the file keeps, so the order only ever grows. */
object Fonts {
    /** The system family names, by number. */
    val FAMILIES = listOf("sans-serif", "serif", "monospace", "sans-serif-condensed", "casual", "cursive")

    fun family(id: Int): String = FAMILIES.getOrElse(id) { FAMILIES[0] }
}

/**
 * A stretch of the text, in characters, with its own look. [color] and [highlight] are Android colours, [size] is in
 * points. What is left out comes from the box.
 */
data class RichSpan(
    val start: Int,
    val end: Int,
    val marks: Int = 0,
    val color: Int? = null,
    val highlight: Int? = null,
    val size: Float? = null,
    val font: Int? = null,
)

/** One line of the text between line breaks. */
data class RichPara(val kind: Int = ParaKinds.BODY, val indent: Int = 0, val checked: Boolean = false)

/**
 * The formatting of a text box, as the engine keeps it: stretches of the text that look different and what kind of
 * line each line is. A line past the end of [paras] is a plain one.
 */
data class RichText(
    val spans: List<RichSpan> = emptyList(),
    val paras: List<RichPara> = emptyList(),
    val font: Int = 0,
) {
    val isEmpty: Boolean get() = spans.isEmpty() && paras.isEmpty() && font == 0

    fun para(line: Int): RichPara = paras.getOrNull(line) ?: RichPara()

    /** The number a numbered line shows: how many lines of its indent have been numbered since the run began. */
    fun numberAt(line: Int): Int {
        val me = para(line)
        var n = 1
        for (j in line - 1 downTo 0) {
            val p = para(j)
            if (p.indent > me.indent && ParaKinds.isList(p.kind)) continue
            if (p.kind == ParaKinds.NUMBERED && p.indent == me.indent) n++ else break
        }
        return n
    }

    /** The JSON the engine reads, or an empty string for a plain box. */
    fun toJson(): String {
        if (isEmpty) return ""
        val o = JSONObject()
        if (spans.isNotEmpty()) {
            o.put(
                "spans",
                JSONArray().apply {
                    for (s in spans) {
                        put(
                            JSONObject().apply {
                                put("start", s.start)
                                put("end", s.end)
                                put("marks", s.marks)
                                s.color?.let { put("color", rgba(it)) }
                                s.highlight?.let { put("highlight", rgba(it)) }
                                s.size?.let { put("size", it.toDouble()) }
                                s.font?.let { put("font", it) }
                            },
                        )
                    }
                },
            )
        }
        if (font != 0) o.put("font", font)
        if (paras.isNotEmpty()) {
            o.put(
                "paras",
                JSONArray().apply {
                    for (p in paras) {
                        put(
                            JSONObject().apply {
                                put("kind", ParaKinds.NAMES[p.kind])
                                if (p.indent != 0) put("indent", p.indent)
                                if (p.checked) put("checked", true)
                            },
                        )
                    }
                },
            )
        }
        return o.toString()
    }

    companion object {
        /** Reads the engine's `rich` object. Anything it cannot read is taken as plain. */
        fun parse(o: JSONObject?): RichText {
            if (o == null) return RichText()
            return try {
                val spans = o.optJSONArray("spans")
                val paras = o.optJSONArray("paras")
                RichText(
                    spans = List(spans?.length() ?: 0) { i ->
                        val s = spans!!.getJSONObject(i)
                        RichSpan(
                            start = s.getInt("start"),
                            end = s.getInt("end"),
                            marks = s.optInt("marks", 0),
                            color = if (s.has("color")) argb(s.getLong("color")) else null,
                            highlight = if (s.has("highlight")) argb(s.getLong("highlight")) else null,
                            size = if (s.has("size")) s.getDouble("size").toFloat() else null,
                            font = if (s.has("font")) s.getInt("font") else null,
                        )
                    },
                    paras = List(paras?.length() ?: 0) { i ->
                        val p = paras!!.getJSONObject(i)
                        RichPara(
                            kind = ParaKinds.NAMES.indexOf(p.optString("kind", "body")).coerceAtLeast(0),
                            indent = p.optInt("indent", 0).coerceIn(0, ParaKinds.MAX_INDENT),
                            checked = p.optBoolean("checked", false),
                        )
                    },
                    font = o.optInt("font", 0),
                )
            } catch (_: JSONException) {
                RichText()
            }
        }

        // The engine keeps 0xRRGGBBAA as an unsigned number; Android wants 0xAARRGGBB.
        private fun argb(rgba: Long): Int {
            val v = rgba and 0xFFFFFFFFL
            return (((v shl 24) and 0xFF000000L) or (v ushr 8)).toInt()
        }

        private fun rgba(argb: Int): Long {
            val v = argb.toLong() and 0xFFFFFFFFL
            return ((v shl 8) and 0xFFFFFF00L) or (v ushr 24)
        }
    }
}
