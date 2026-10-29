package com.onemnemo.grapho.ui.settings

import android.content.Context

/** What a gesture does on the note screen. [key] is what is stored, so it never changes with the name. */
enum class GestureAction(val key: String) {
    Undo("undo"),
    Redo("redo"),
    Off("off"),
}

/**
 * The gestures the note screen knows. [key] is the stored name of the gesture and never changes.
 */
enum class NoteGesture(val default: GestureAction, val key: String) {
    TwoFingerTap(GestureAction.Undo, "two_finger_tap"),
    ThreeFingerTap(GestureAction.Redo, "three_finger_tap"),
}

/** The tap gesture for that many fingers, or null when there is none. */
fun fingerGesture(fingers: Int): NoteGesture? = when (fingers) {
    2 -> NoteGesture.TwoFingerTap
    3 -> NoteGesture.ThreeFingerTap
    else -> null
}

/** The action stored under [key], or the gesture's own default when it is missing or not one the app knows. */
internal fun gestureActionFor(gesture: NoteGesture, key: String?): GestureAction =
    GestureAction.entries.firstOrNull { it.key == key } ?: gesture.default

/** What each gesture does, in shared preferences. */
class GestureStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Map<NoteGesture, GestureAction> =
        NoteGesture.entries.associateWith { gestureActionFor(it, prefs.getString(it.key, null)) }

    fun save(gesture: NoteGesture, action: GestureAction) = prefs.edit().putString(gesture.key, action.key).apply()

    private companion object {
        const val PREFS = "grapho_gestures"
    }
}
