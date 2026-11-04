package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AlignmentSpan
import android.text.style.ReplacementSpan
import kotlin.math.ceil
import kotlin.math.min
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
import ru.noties.jlatexmath.awt.AndroidGraphics2D
import ru.noties.jlatexmath.awt.Color

/**
 * Turns the math in a text box into drawn formulas. This is the one place that knows the LaTeX library, so the rest of
 * the app only sees a [CharSequence] to lay out.
 */
internal object MathText {
    /** Stands in for a formula in the laid out text. */
    private const val SLOT = "￼"

    /**
     * The text of a box ready for a layout [width] wide, with each formula as one slot drawn by a span. Display math
     * gets a centred line of its own. Math that does not parse is left as it was typed. Plain text comes back as is.
     */
    fun prepare(text: String, size: Float, width: Float): CharSequence {
        if (!MathRuns.mayHaveMath(text)) return text
        val runs = MathRuns.split(text)
        if (runs.none { it is MathRun.Math }) return runs.joinToString("") { (it as MathRun.Plain).text }
        val out = SpannableStringBuilder()
        // A display formula already ends its line, so a break typed right after it is dropped instead of leaving a
        // blank line.
        var afterDisplay = false
        for (run in runs) {
            when (run) {
                is MathRun.Plain -> out.append(if (afterDisplay) run.text.removePrefix("\n") else run.text)
                is MathRun.Math -> appendMath(out, run, size, width)
            }
            afterDisplay = run is MathRun.Math && run.display && out.endsWith('\n')
        }
        // A display formula at the very end needs no break after it.
        if (afterDisplay) out.delete(out.length - 1, out.length)
        return out
    }

    private fun appendMath(out: SpannableStringBuilder, run: MathRun.Math, size: Float, width: Float) {
        val icon = formula(run.latex, size, run.display)
        if (icon == null) {
            out.append(if (run.display) "$$${run.latex}$$" else "$${run.latex}$")
            return
        }
        if (run.display && out.isNotEmpty() && !out.endsWith('\n')) out.append('\n')
        val start = out.length
        out.append(SLOT)
        out.setSpan(FormulaSpan(icon, width), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (run.display) {
            out.append('\n')
            out.setSpan(
                AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                start,
                out.length,
                Spanned.SPAN_PARAGRAPH,
            )
        }
    }

    /** The formula for [latex] at font [size], or null when it does not parse. */
    private fun formula(latex: String, size: Float, display: Boolean): TeXIcon? = try {
        TeXFormula(latex).TeXIconBuilder()
            .setStyle(if (display) TeXConstants.STYLE_DISPLAY else TeXConstants.STYLE_TEXT)
            .setSize(size)
            .setTrueValues(true)
            .build()
    } catch (e: Exception) {
        // A half written formula is normal while typing, so it shows as source instead of failing.
        null
    }
}

/**
 * Draws one formula in a line of text, on the baseline, in the colour the text is drawn in. A formula wider than
 * [maxWidth] is shrunk to fit, since it cannot wrap.
 */
private class FormulaSpan(private val icon: TeXIcon, maxWidth: Float) : ReplacementSpan() {
    private val scale = min(1f, maxWidth / icon.trueIconWidth.coerceAtLeast(1f))
    private val above = (icon.trueIconHeight - icon.trueIconDepth) * scale
    private val below = icon.trueIconDepth * scale
    private val wide = icon.trueIconWidth * scale
    private val graphics = AndroidGraphics2D()

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        if (fm != null) {
            paint.getFontMetricsInt(fm)
            fm.ascent = min(fm.ascent, -ceil(above).toInt())
            fm.top = min(fm.top, fm.ascent)
            fm.descent = maxOf(fm.descent, ceil(below).toInt())
            fm.bottom = maxOf(fm.bottom, fm.descent)
        }
        return ceil(wide).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val save = canvas.save()
        canvas.translate(x, y - above)
        if (scale != 1f) canvas.scale(scale, scale)
        icon.setForeground(Color(paint.color))
        graphics.setCanvas(canvas)
        icon.paintIcon(null, graphics, 0, 0)
        canvas.restoreToCount(save)
    }
}
