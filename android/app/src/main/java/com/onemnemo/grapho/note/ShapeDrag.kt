package com.onemnemo.grapho.note

import android.graphics.RectF

/**
 * One drag of the Shapes tool, from where the pen came down. Each move asks the core for the shape between the start
 * and the pen, kept inside the page the drag began on.
 */
internal class ShapeDrag(private val spec: FloatArray, private val x0: Float, private val y0: Float) {
    /** The shape for a pen at ([x], [y]) in view pixels inside [page], or null when the drag draws nothing yet. */
    fun shapeTo(stroke: WetStroke, x: Float, y: Float, page: RectF): SnappedShape? = stroke.drawShape(
        spec,
        x0.coerceIn(page.left, page.right),
        y0.coerceIn(page.top, page.bottom),
        x.coerceIn(page.left, page.right),
        y.coerceIn(page.top, page.bottom),
        page,
    )
}
