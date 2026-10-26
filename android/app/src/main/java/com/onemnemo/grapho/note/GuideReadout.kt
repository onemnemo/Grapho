package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * The angle shown beside the pen while a ray is drawn from a protractor's centre. It is drawn into the front buffer
 * with the stroke, as plain text on a halo rather than a chip, and sits above the pen so the hand does not cover it.
 * [draw] runs on the render thread only; [box] only reads sizes.
 */
internal class GuideReadout(private val density: Float) {
    private val text = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = TEXT_DP * density
        typeface = Typeface.DEFAULT_BOLD
    }
    private val halo = Paint(text).apply {
        style = Paint.Style.STROKE
        strokeWidth = HALO_DP * density
        strokeJoin = Paint.Join.ROUND
    }

    /** Puts the area the readout covers for a pen at ([x], [y]) in [out]. */
    fun box(x: Float, y: Float, out: RectF) {
        val cy = y - ABOVE_DP * density
        out.set(
            x - WIDTH_DP * density / 2,
            cy - HEIGHT_DP * density / 2,
            x + WIDTH_DP * density / 2,
            cy + HEIGHT_DP * density / 2,
        )
    }

    fun draw(canvas: Canvas, label: String, box: RectF, dark: Boolean) {
        text.color = GuideColours.text(dark)
        halo.color = GuideColours.halo(dark)
        val baseline = box.centerY() - (text.ascent() + text.descent()) / 2
        canvas.drawText(label, box.centerX(), baseline, halo)
        canvas.drawText(label, box.centerX(), baseline, text)
    }

    private companion object {
        const val TEXT_DP = 17f
        const val HALO_DP = 4f
        const val WIDTH_DP = 150f
        const val HEIGHT_DP = 30f
        const val ABOVE_DP = 46f
    }
}
