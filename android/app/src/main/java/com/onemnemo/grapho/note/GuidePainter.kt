package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the ruler and protractor over the page. The tick paths are built once, in points around the guide's centre, so
 * they never depend on the zoom: the canvas is moved, turned and scaled to the guide and the same paths are replayed,
 * which keeps every tick on the paper's millimetres. Only the choice of which ticks to show changes with zoom. Render
 * thread only.
 */
internal class GuidePainter(private val density: Float) {
    private val fill = Paint().apply { style = Paint.Style.FILL }
    private val line = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val text = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    private val halo = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        strokeJoin = Paint.Join.ROUND
    }
    private val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }

    private val rulerMm = Path()
    private val rulerHalf = Path()
    private val rulerCm = Path()
    private val arcMinor = Path()
    private val arcMid = Path()
    private val arcMajor = Path()
    private val body = Path()
    private val box = RectF()
    private val numbers = Array(RULER_CM + 1) { it.toString() }
    private val degrees = Array(DEGREE_LABELS) { "${it * 10}" }

    init {
        buildRuler()
        buildProtractor()
    }

    fun draw(canvas: Canvas, vp: Viewport, paper: Int, guides: GuideSet) {
        val dark = isDarkPaper(paper)
        guides.protractor?.let { drawProtractor(canvas, vp, it, dark) }
        guides.ruler?.let { drawRuler(canvas, vp, it, dark) }
    }

    private fun buildRuler() {
        val half = GuideMath.RULER_LENGTH / 2
        val edge = GuideMath.RULER_THICKNESS / 2
        for (i in 0..RULER_CM * 10) {
            val x = -half + i * MM
            val (path, length) = when {
                i % 10 == 0 -> rulerCm to CM_TICK * MM
                i % 5 == 0 -> rulerHalf to HALF_TICK * MM
                else -> rulerMm to MM_TICK * MM
            }
            path.moveTo(x, -edge)
            path.lineTo(x, -edge + length)
            path.moveTo(x, edge)
            path.lineTo(x, edge - length)
        }
    }

    private fun drawRuler(canvas: Canvas, vp: Viewport, p: GuidePlace, dark: Boolean) {
        val s = vp.scale
        val cx = p.x * s + vp.offsetX
        val cy = p.y * s + vp.offsetY
        val half = GuideMath.RULER_LENGTH / 2
        val edge = GuideMath.RULER_THICKNESS / 2
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(Math.toDegrees(p.angle.toDouble()).toFloat())
        canvas.scale(s, s)
        fill.color = GuideColours.body(dark)
        canvas.drawRect(-half, -edge, half, edge, fill)
        line.color = GuideColours.edge(dark)
        line.strokeWidth = EDGE_DP * density / s
        canvas.drawRect(-half, -edge, half, edge, line)
        line.color = GuideColours.tick(dark)
        line.strokeWidth = TICK_DP * density / s
        val mmPx = MM * s
        if (mmPx >= MIN_TICK_PX) canvas.drawPath(rulerMm, line)
        if (mmPx * 5 >= MIN_TICK_PX) canvas.drawPath(rulerHalf, line)
        canvas.drawPath(rulerCm, line)
        if (mmPx * 10 >= MIN_LABEL_PX * density) {
            text.color = GuideColours.text(dark)
            text.textSize = RULER_TEXT_DP * density / s
            val baseline = -edge + (CM_TICK * MM) + text.textSize * 1.05f
            for (cm in 0..RULER_CM) canvas.drawText(numbers[cm], -half + cm * 10 * MM, baseline, text)
        }
        canvas.restore()
        drawReading(canvas, cx, cy, "${p.label}°", dark)
        placeButton(canvas, cx, cy, p.angle, GuideMath.RULER_CLOSE * s, 0f, dark)
    }

    private fun buildProtractor() {
        val r = GuideMath.PROTRACTOR_RADIUS
        box.set(-r, -r, r, r)
        body.moveTo(-r, 0f)
        body.arcTo(box, 180f, 180f)
        body.close()
        for (deg in 0..180) {
            val (path, length) = when {
                deg % 10 == 0 -> arcMajor to CM_TICK * MM
                deg % 5 == 0 -> arcMid to HALF_TICK * MM
                else -> arcMinor to MM_TICK * MM
            }
            val t = Math.toRadians(deg.toDouble())
            val (c, s) = cos(t).toFloat() to sin(t).toFloat()
            path.moveTo(r * c, -r * s)
            path.lineTo((r - length) * c, -(r - length) * s)
        }
    }

    private fun drawProtractor(canvas: Canvas, vp: Viewport, p: GuidePlace, dark: Boolean) {
        val s = vp.scale
        val r = GuideMath.PROTRACTOR_RADIUS
        val cx = p.x * s + vp.offsetX
        val cy = p.y * s + vp.offsetY
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(Math.toDegrees(p.angle.toDouble()).toFloat())
        canvas.scale(s, s)
        fill.color = GuideColours.body(dark)
        canvas.drawPath(body, fill)
        line.color = GuideColours.edge(dark)
        line.strokeWidth = EDGE_DP * density / s
        canvas.drawPath(body, line)
        line.color = GuideColours.tick(dark)
        line.strokeWidth = TICK_DP * density / s
        val degreePx = r * s * RADIANS_PER_DEGREE
        if (degreePx >= MIN_TICK_PX) canvas.drawPath(arcMinor, line)
        if (degreePx * 5 >= MIN_TICK_PX) canvas.drawPath(arcMid, line)
        canvas.drawPath(arcMajor, line)
        if (degreePx * 10 >= MIN_LABEL_PX * density) drawDegreeLabels(canvas, s, dark)
        line.color = GuideColours.accent(dark)
        line.strokeWidth = CENTRE_DP * density / s
        val mark = CENTRE_MARK * MM
        canvas.drawLine(-mark, 0f, mark, 0f, line)
        canvas.drawLine(0f, -mark, 0f, mark, line)
        canvas.restore()
        placeButton(canvas, cx, cy, p.angle, 0f, -GuideMath.PROTRACTOR_CLOSE * s, dark)
    }

    // Each scale reads outward along its own radius, one counting up from the base's right end and one from its left.
    private fun drawDegreeLabels(canvas: Canvas, s: Float, dark: Boolean) {
        val r = GuideMath.PROTRACTOR_RADIUS
        for (i in 0 until DEGREE_LABELS) {
            val t = Math.toRadians((i * 10).toDouble())
            val (c, sn) = cos(t).toFloat() to sin(t).toFloat()
            val turn = 90f - i * 10
            text.color = GuideColours.text(dark)
            text.textSize = DEGREE_TEXT_DP * density / s
            labelAt(canvas, degrees[i], (r - OUTER_LABEL * MM) * c, -(r - OUTER_LABEL * MM) * sn, turn)
            text.color = GuideColours.faint(dark)
            text.textSize = (DEGREE_TEXT_DP - 1) * density / s
            val inner = degrees[DEGREE_LABELS - 1 - i]
            labelAt(canvas, inner, (r - INNER_LABEL * MM) * c, -(r - INNER_LABEL * MM) * sn, turn)
        }
    }

    private fun labelAt(canvas: Canvas, label: String, x: Float, y: Float, turn: Float) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(turn)
        canvas.drawText(label, 0f, text.textSize * 0.35f, text)
        canvas.restore()
    }

    private fun drawReading(canvas: Canvas, x: Float, y: Float, label: String, dark: Boolean) {
        bold.textSize = READING_DP * density
        bold.color = GuideColours.text(dark)
        halo.textSize = bold.textSize
        halo.strokeWidth = HALO_DP * density
        halo.color = GuideColours.halo(dark)
        val baseline = y - (bold.ascent() + bold.descent()) / 2
        canvas.drawText(label, x, baseline, halo)
        canvas.drawText(label, x, baseline, bold)
    }

    // The close button, at ([u], [v]) pixels from the guide's centre along its axes, drawn upright.
    private fun placeButton(canvas: Canvas, cx: Float, cy: Float, angle: Float, u: Float, v: Float, dark: Boolean) {
        val c = cos(angle)
        val s = sin(angle)
        val x = cx + u * c - v * s
        val y = cy + u * s + v * c
        val radius = BUTTON_DP * density
        fill.color = GuideColours.button(dark)
        canvas.drawCircle(x, y, radius, fill)
        line.color = GuideColours.edge(dark)
        line.strokeWidth = EDGE_DP * density
        canvas.drawCircle(x, y, radius, line)
        line.color = GuideColours.text(dark)
        line.strokeWidth = CROSS_DP * density
        line.strokeCap = Paint.Cap.ROUND
        val a = radius * 0.4f
        canvas.drawLine(x - a, y - a, x + a, y + a, line)
        canvas.drawLine(x - a, y + a, x + a, y - a, line)
        line.strokeCap = Paint.Cap.BUTT
    }

    private companion object {
        const val MM = NotebookInfo.PT_PER_MM
        const val RULER_CM = 30
        const val DEGREE_LABELS = 19
        const val RADIANS_PER_DEGREE = 0.017453292f

        // Tick lengths and label positions, in millimetres.
        const val MM_TICK = 2.2f
        const val HALF_TICK = 3.5f
        const val CM_TICK = 5f
        const val OUTER_LABEL = 9f
        const val INNER_LABEL = 15f
        const val CENTRE_MARK = 3.5f

        // Sizes that stay the same on screen at any zoom, in dp.
        const val EDGE_DP = 1.2f
        const val TICK_DP = 0.9f
        const val CENTRE_DP = 1.4f
        const val CROSS_DP = 1.6f
        const val HALO_DP = 4f
        const val RULER_TEXT_DP = 10f
        const val DEGREE_TEXT_DP = 9f
        const val READING_DP = 18f
        const val BUTTON_DP = 11f

        /** Ticks closer together than this on screen are left out. */
        const val MIN_TICK_PX = 3.2f
        const val MIN_LABEL_PX = 16f
    }
}
