package com.onemnemo.grapho.note

/** The matrix that mirrors a selection about its own middle. Pure, so it can be tested without a device. */
internal object SelectionFlip {
    /**
     * The first six values of an android.graphics.Matrix that mirror across the vertical line through [cx] when
     * [horizontal], or across the horizontal line through [cy] otherwise.
     */
    fun matrix(horizontal: Boolean, cx: Float, cy: Float): FloatArray = if (horizontal) {
        floatArrayOf(-1f, 0f, 2f * cx, 0f, 1f, 0f)
    } else {
        floatArrayOf(1f, 0f, 0f, 0f, -1f, 2f * cy)
    }
}
