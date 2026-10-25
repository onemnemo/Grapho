package com.onemnemo.grapho.note

import org.json.JSONObject

/** Why the active layer would not take a stroke. */
enum class LayerBlock { NONE, HIDDEN, LOCKED }

/** One layer of a page. [opacity] runs from 0 to 1. */
data class LayerInfo(val name: String, val visible: Boolean, val locked: Boolean, val opacity: Float, val strokes: Int)

/** The layers of one page, bottom to top, and the index of the one that edits go to. */
data class PageLayers(val active: Int, val layers: List<LayerInfo>) {
    val canAdd: Boolean get() = layers.size < MAX_LAYERS

    /** Whether drawing on the active layer would be refused. */
    val block: LayerBlock
        get() {
            val layer = layers.getOrNull(active) ?: return LayerBlock.NONE
            return when {
                !layer.visible -> LayerBlock.HIDDEN
                layer.locked -> LayerBlock.LOCKED
                else -> LayerBlock.NONE
            }
        }

    /**
     * The name for a new layer: one more than the highest number among the layers already named by [format]
     * (which turns a number into a name), or one more than the layer count when none are.
     */
    fun nextName(format: (Int) -> String): String {
        val sample = format(0)
        val prefix = sample.removeSuffix("0")
        val highest = if (prefix == sample) {
            null
        } else {
            val pattern = Regex(Regex.escape(prefix) + "(\\d+)")
            layers.mapNotNull { pattern.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() }.maxOrNull()
        }
        return format((highest ?: layers.size) + 1)
    }

    companion object {
        /** The most layers a page can hold. */
        const val MAX_LAYERS = 10

        /** Reads the engine's JSON for a page. An empty string, for an unknown page, gives null. */
        fun parse(json: String): PageLayers? {
            if (json.isEmpty()) return null
            val o = JSONObject(json)
            val list = o.getJSONArray("layers")
            return PageLayers(
                active = o.getInt("active"),
                layers = List(list.length()) {
                    val l = list.getJSONObject(it)
                    LayerInfo(
                        name = l.getString("name"),
                        visible = l.getBoolean("visible"),
                        locked = l.getBoolean("locked"),
                        opacity = l.getDouble("opacity").toFloat(),
                        strokes = l.getInt("strokes"),
                    )
                },
            )
        }
    }
}
