package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Width of selection lines, in dp. */
private const val LINE_DP = 1.5f

/** How far the halo reaches past each side of a line or handle, in dp. */
private const val HALO_DP = 1.5f

/**
 * Paints for the marks a selection adds to the page, set from the paper with [setPaper]: lines and rings in the
 * accent colour over a solid halo, and handles as accent dots ringed by the halo. Dashed lines keep a solid halo,
 * so the gaps show the halo instead of whatever is under them. Render thread only.
 */
internal class SelectionInk(private val density: Float) {
    private val dash = DashPathEffect(floatArrayOf(7f * density, 5f * density), 0f)
    private val line = stroke(LINE_DP * density)
    private val halo = stroke((LINE_DP + 2f * HALO_DP) * density)
    private val fill = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val ring = stroke(HALO_DP * density)

    /** The accent colour, for marks drawn inside a handle. */
    var accent = 0
        private set

    /** The halo colour, for marks drawn inside a handle. */
    var edge = 0
        private set

    fun setPaper(paper: Int) {
        val dark = isDarkPaper(paper)
        accent = SelectionColours.accent(dark)
        edge = SelectionColours.halo(dark)
        line.color = accent
        fill.color = accent
        halo.color = edge
        ring.color = edge
    }

    fun drawPath(canvas: Canvas, path: Path, dashed: Boolean) {
        canvas.drawPath(path, halo)
        line.pathEffect = if (dashed) dash else null
        canvas.drawPath(path, line)
    }

    fun drawRect(canvas: Canvas, rect: RectF, dashed: Boolean) {
        canvas.drawRect(rect, halo)
        line.pathEffect = if (dashed) dash else null
        canvas.drawRect(rect, line)
    }

    fun drawLine(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float, dashed: Boolean) {
        canvas.drawLine(x0, y0, x1, y1, halo)
        line.pathEffect = if (dashed) dash else null
        canvas.drawLine(x0, y0, x1, y1, line)
    }

    /** An outline circle, like the eraser's. */
    fun drawCircle(canvas: Canvas, x: Float, y: Float, r: Float) {
        canvas.drawCircle(x, y, r, halo)
        line.pathEffect = null
        canvas.drawCircle(x, y, r, line)
    }

    /** A handle of radius [r]: an accent dot with the halo round its edge. */
    fun drawHandle(canvas: Canvas, x: Float, y: Float, r: Float) {
        canvas.drawCircle(x, y, r, fill)
        canvas.drawCircle(x, y, r, ring)
    }

    private fun stroke(width: Float) = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.BUTT
    }
}
