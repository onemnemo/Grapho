package com.onemnemo.grapho.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturesTest {
    @Test
    fun aStoredKeyGivesItsAction() {
        for (action in GestureAction.entries) {
            assertEquals(action, gestureActionFor(NoteGesture.TwoFingerTap, action.key))
        }
    }

    @Test
    fun aMissingOrUnknownKeyGivesTheDefault() {
        assertEquals(GestureAction.Undo, gestureActionFor(NoteGesture.TwoFingerTap, null))
        assertEquals(GestureAction.Redo, gestureActionFor(NoteGesture.ThreeFingerTap, "scribble"))
    }

    @Test
    fun keysAreUniqueSoOneGestureNeverSharesAPreference() {
        assertEquals(NoteGesture.entries.size, NoteGesture.entries.map { it.key }.toSet().size)
        assertEquals(GestureAction.entries.size, GestureAction.entries.map { it.key }.toSet().size)
        assertTrue(NoteGesture.entries.all { it.key.isNotBlank() })
    }
}
