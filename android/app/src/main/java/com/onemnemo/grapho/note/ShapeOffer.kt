package com.onemnemo.grapho.note

import android.graphics.RectF
import com.onemnemo.grapho.engine.NativeInk

/** The stroke the person drew, and the clean shape that would replace it. */
internal class OfferedShape(
    val page: Int,
    /** The shape's samples as `[x, y, pressure, timeMs, ...]` in page-local document units. */
    val local: FloatArray,
    val tool: ToolState,
    val startedAtMs: Long,
)

/**
 * The shape offered for the newest stroke, if there is one. A stroke is looked at when it ends; the offer lives until
 * something else touches the page, the person takes it, or it is dismissed. [minPx] is how big a stroke must be to be
 * offered at all, so handwriting is left alone.
 */
internal class ShapeOffer(private val minPx: Float) {
    var offered: OfferedShape? = null
        private set

    /**
     * Looks at the finished stroke [drawn] (view pixels) and keeps an offer for it when the core recognises a shape.
     * [toLocal] converts samples from view pixels to the page's document units. Returns what to show, or null.
     */
    fun consider(
        page: Int,
        drawn: FloatArray,
        tool: ToolState,
        startedAtMs: Long,
        toLocal: (FloatArray) -> FloatArray,
    ): ShapeSuggestion? {
        offered = null
        val bounds = boundsOf(drawn) ?: return null
        if (!worthSuggesting(bounds.width(), bounds.height(), minPx)) return null
        val found = NativeInk.nativeSuggest(drawn)
        if (found.size < HEADER) return null
        val guess = ShapeGuess.fromCode(found[0].toInt()) ?: return null
        val count = found[1].toInt()
        if (count <= 0 || HEADER + count > found.size) return null
        offered = OfferedShape(page, toLocal(found.copyOfRange(HEADER, HEADER + count)), tool, startedAtMs)
        return ShapeSuggestion(guess, bounds)
    }

    /** Forgets the offer. True when there was one. */
    fun clear(): Boolean {
        val had = offered != null
        offered = null
        return had
    }

    private fun boundsOf(samples: FloatArray): RectF? {
        if (samples.size < STRIDE) return null
        val r = RectF(samples[0], samples[1], samples[0], samples[1])
        var i = 0
        while (i + STRIDE <= samples.size) {
            r.union(samples[i], samples[i + 1])
            i += STRIDE
        }
        return r
    }

    private companion object {
        const val HEADER = 2
        const val STRIDE = 4
    }
}
