package com.onemnemo.grapho.ui.note

import kotlin.math.max
import kotlin.math.min

/** The scan line that sweeps a picture while a job works on it. */
internal object ScanSweep {
    /** One pass, from edge to edge, in milliseconds. */
    const val PERIOD_MS = 1600

    private const val FADE_IN = 0.12f
    private const val FADE_OUT = 0.88f

    /** How visible the line is at [t] (0 to 1 through a pass): it fades in at the start and out at the end. */
    fun alpha(t: Float): Float = when {
        t <= 0f || t >= 1f -> 0f
        t < FADE_IN -> t / FADE_IN
        t > FADE_OUT -> (1f - t) / (1f - FADE_OUT)
        else -> 1f
    }

    /** Where the line's leading edge is at [t], along [extent], so a line [thickness] thick ends flush with the edge. */
    fun offset(t: Float, extent: Float, thickness: Float): Float = t.coerceIn(0f, 1f) * max(0f, extent - thickness)
}

/** Where the pill goes, in view pixels. */
internal class PillSpot(val x: Float, val y: Float)

/** Places pills under the picture they speak for. */
internal object PillPlacement {
    /**
     * Puts a pill [pillWidth] by [pillHeight] centred under a picture whose box is [left] to [right] and ending at
     * [bottom], [gap] below it. It stays inside the window less the margins, and when there is no room under the
     * picture it sits just inside the picture's lower edge instead of leaving the window.
     */
    fun below(
        left: Float,
        right: Float,
        bottom: Float,
        gap: Float,
        pillWidth: Float,
        pillHeight: Float,
        windowWidth: Float,
        windowHeight: Float,
        margins: EdgeInsets,
    ): PillSpot {
        val xLow = min(margins.left, max(0f, windowWidth - pillWidth))
        val xHigh = max(xLow, windowWidth - pillWidth - margins.right)
        val x = ((left + right) / 2f - pillWidth / 2f).coerceIn(xLow, xHigh)
        val yLow = min(margins.top, max(0f, windowHeight - pillHeight))
        val yHigh = max(yLow, windowHeight - pillHeight - margins.bottom)
        val under = bottom + gap
        val y = (if (under <= yHigh) under else bottom - gap - pillHeight).coerceIn(yLow, yHigh)
        return PillSpot(x, y)
    }
}

/**
 * The offer to look at a picture's original after its background was removed. It holds only while the cut-out is
 * still the newest edit, since the look goes through undo and redo. [original] is the id of the picture the cut-out
 * replaced, which undo brings back. [originalSource] and [cutSource] name the embedded pictures of the two.
 */
internal class CompareOffer(
    val page: Int,
    val ids: LongArray,
    var generation: Int,
    val original: LongArray,
    val originalSource: String = "",
    val cutSource: String = "",
) {
    fun stillCurrent(generationNow: Int): Boolean = generation == generationNow
}
