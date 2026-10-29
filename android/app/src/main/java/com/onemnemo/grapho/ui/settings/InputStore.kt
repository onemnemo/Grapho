package com.onemnemo.grapho.ui.settings

import android.content.Context
import com.onemnemo.grapho.note.ShapeDetection

/** The input choices that outlive a note, in shared preferences. */
class InputStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadFingerDraws(): Boolean = prefs.getBoolean(FINGER_DRAWS, false)

    fun saveFingerDraws(on: Boolean) = prefs.edit().putBoolean(FINGER_DRAWS, on).apply()

    fun loadShapeDetection(): ShapeDetection = ShapeDetection.fromKey(prefs.getString(SHAPE_DETECTION, null))

    fun saveShapeDetection(mode: ShapeDetection) = prefs.edit().putString(SHAPE_DETECTION, mode.key).apply()

    private companion object {
        const val PREFS = "grapho_input"
        const val FINGER_DRAWS = "finger_draws"
        const val SHAPE_DETECTION = "shape_detection"
    }
}
