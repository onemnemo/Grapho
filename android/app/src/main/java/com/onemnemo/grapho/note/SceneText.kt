package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import org.json.JSONArray
import org.json.JSONException

/** How the lines of a text box sit across it. The codes are the engine's. */
enum class TextAlignment(val code: Int) {
    Start(0),
    Centre(1),
    End(2),
    ;

    companion object {
        fun of(code: Int): TextAlignment = entries.firstOrNull { it.code == code } ?: Start
    }
}

/**
 * A text box placed on a layer, ready to draw. The frame is the box before it is turned, in page-local units, and
 * [rotation] the clockwise turn about its centre in radians. [argb] is an Android colour. Never changed after
 * creation.
 */
data class SceneText(
    val id: StrokeId,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val text: String,
    val size: Float,
    val argb: Int,
    val align: TextAlignment,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
) {
    /** The box the text covers on the page once turned, in page-local units. */
    val bounds: RectF = RectF(x, y, x + width, y + height).also {
        Matrix().apply { setRotate(Math.toDegrees(rotation.toDouble()).toFloat(), x + width / 2f, y + height / 2f) }
            .mapRect(it)
    }
}

/** Reads the engine's per layer text box list, which lines up with the layers [StrokeLayout] reads. */
internal object TextLayout {
    private const val TAG = "TextLayout"

    /** One list per visible layer, bottom to top, boxes in drawing order. Empty if the page has none. */
    fun parse(json: String): List<List<SceneText>> {
        if (json.isEmpty()) return emptyList()
        return try {
            val layers = JSONArray(json)
            List(layers.length()) { layer ->
                val boxes = layers.getJSONArray(layer)
                List(boxes.length()) { index ->
                    val o = boxes.getJSONObject(index)
                    SceneText(
                        id = StrokeId(o.getLong("hi"), o.getLong("lo")),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        width = o.getDouble("w").toFloat(),
                        height = o.getDouble("h").toFloat(),
                        rotation = o.getDouble("rot").toFloat(),
                        text = o.getString("text"),
                        size = o.getDouble("size").toFloat(),
                        argb = o.getInt("argb"),
                        align = TextAlignment.of(o.getInt("align")),
                        bold = o.getBoolean("bold"),
                        italic = o.optBoolean("italic", false),
                        underline = o.optBoolean("underline", false),
                    )
                }
            }
        } catch (e: JSONException) {
            Log.w(TAG, "the page's text boxes could not be read", e)
            emptyList()
        }
    }
}
