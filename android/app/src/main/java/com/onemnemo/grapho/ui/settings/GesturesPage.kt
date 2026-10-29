package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl

@StringRes
private fun gestureName(gesture: NoteGesture): Int = when (gesture) {
    NoteGesture.TwoFingerTap -> R.string.gesture_two_finger_tap
    NoteGesture.ThreeFingerTap -> R.string.gesture_three_finger_tap
}

@StringRes
private fun actionName(action: GestureAction): Int = when (action) {
    GestureAction.Undo -> R.string.note_undo
    GestureAction.Redo -> R.string.note_redo
    GestureAction.Off -> R.string.gesture_off
}

/** What a tap with fingers does on the note screen. Each choice applies at once. */
@Composable
internal fun GesturesPage(settings: AppSettings) {
    SettingsPage {
        SettingsGroup(R.string.settings_finger_gestures) {
            for (gesture in NoteGesture.entries) {
                FieldRow(
                    label = stringResource(gestureName(gesture)),
                    // One hint is enough for the group.
                    hint = stringResource(R.string.gesture_finger_hint).takeIf { gesture.ordinal == 0 },
                    below = {
                        SegmentedControl(
                            options = GestureAction.entries.map { it to stringResource(actionName(it)) },
                            selected = settings.gestureAction(gesture),
                            onSelect = { settings.changeGesture(gesture, it) },
                            size = SegmentSize.Small,
                        )
                    },
                )
            }
        }
    }
}
