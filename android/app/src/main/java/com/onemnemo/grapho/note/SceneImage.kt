package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import org.json.JSONArray
import org.json.JSONException

/**
 * An image placed on a layer, ready to draw. [matrix] maps the unit square of the shown (cropped) part of the picture
 * onto the page, in page-local units. [crop] is that part as fractions of the whole picture: left, top, right, bottom.
 * Never changed after creation.
 */
class SceneImage(val id: StrokeId, val source: String, val matrix: Matrix, val crop: RectF) {
    /** The box the image covers on the page, in page-local units. */
    val bounds = RectF(0f, 0f, 1f, 1f).also { matrix.mapRect(it) }
}

/** Reads the engine's per layer image list, which lines up with the layers [StrokeLayout] reads. */
internal object ImageLayout {
    private const val TAG = "ImageLayout"

    /** One list per visible layer, bottom to top, images in drawing order. Empty if the page has none. */
    fun parse(json: String): List<List<SceneImage>> {
        if (json.isEmpty()) return emptyList()
        return try {
            val layers = JSONArray(json)
            List(layers.length()) { layer ->
                val images = layers.getJSONArray(layer)
                List(images.length()) { index ->
                    val o = images.getJSONObject(index)
                    val m = o.getJSONArray("m")
                    val c = o.getJSONArray("crop")
                    val matrix = Matrix()
                    matrix.setValues(
                        floatArrayOf(
                            m.getDouble(0).toFloat(),
                            m.getDouble(1).toFloat(),
                            m.getDouble(2).toFloat(),
                            m.getDouble(3).toFloat(),
                            m.getDouble(4).toFloat(),
                            m.getDouble(5).toFloat(),
                            0f,
                            0f,
                            1f,
                        ),
                    )
                    val crop = RectF(
                        c.getDouble(0).toFloat(),
                        c.getDouble(1).toFloat(),
                        c.getDouble(2).toFloat(),
                        c.getDouble(3).toFloat(),
                    )
                    SceneImage(StrokeId(o.getLong("hi"), o.getLong("lo")), o.getString("source"), matrix, crop)
                }
            }
        } catch (e: JSONException) {
            Log.w(TAG, "the page's images could not be read", e)
            emptyList()
        }
    }
}
