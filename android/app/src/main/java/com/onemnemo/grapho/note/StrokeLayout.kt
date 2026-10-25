package com.onemnemo.grapho.note

/** Opacity of a fully opaque layer, as the engine reports it. */
internal const val LAYER_OPAQUE = 255

/** The stroke ids of one visible layer, bottom to top, and the layer's opacity from 0 to [LAYER_OPAQUE]. */
internal class LayerIds(val alpha: Int, val ids: List<StrokeId>)

/** Reads the engine's per layer stroke list: `[alpha, n, n x (hi, lo)]` for each visible layer, bottom to top. */
internal object StrokeLayout {
    fun parse(raw: LongArray): List<LayerIds> {
        val out = ArrayList<LayerIds>()
        var i = 0
        while (i + 1 < raw.size) {
            val alpha = raw[i].toInt().coerceIn(0, LAYER_OPAQUE)
            val n = raw[i + 1].toInt().coerceAtLeast(0)
            i += 2
            val start = i
            val count = minOf(n, (raw.size - start) / 2)
            out.add(LayerIds(alpha, List(count) { StrokeId(raw[start + 2 * it], raw[start + 2 * it + 1]) }))
            i = start + 2 * count
        }
        return out
    }

    /** How many strokes the visible layers hold in all. */
    fun count(raw: LongArray): Int {
        var total = 0
        var i = 0
        while (i + 1 < raw.size) {
            val n = raw[i + 1].toInt().coerceIn(0, (raw.size - i - 2) / 2)
            total += n
            i += 2 + 2 * n
        }
        return total
    }
}
