package com.onemnemo.grapho.ui.settings

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.onemnemo.grapho.note.ShapeDetection
import com.onemnemo.grapho.ui.note.AutoHide
import com.onemnemo.grapho.ui.note.BarPosition
import com.onemnemo.grapho.ui.note.DockEdge
import com.onemnemo.grapho.ui.note.NoteBarStore
import com.onemnemo.grapho.ui.note.NoteBarStyle
import com.onemnemo.grapho.ui.note.NoteBarTool
import com.onemnemo.grapho.ui.note.WheelPlacementStore
import com.onemnemo.grapho.ui.theme.Appearance
import com.onemnemo.grapho.ui.theme.AppearanceStore

/**
 * The app wide choices that Settings changes while the app runs. Each one is state, so whatever reads it
 * redraws at once, and each is written to its own store as it changes.
 */
@Stable
class AppSettings(context: Context) {
    private val appearanceStore = AppearanceStore(context)
    private val noteBarStore = NoteBarStore(context)
    private val inputStore = InputStore(context)
    private val gestureStore = GestureStore(context)
    private val wheelStore = WheelPlacementStore(context)

    var appearance by mutableStateOf(appearanceStore.load())
        private set

    var noteBar by mutableStateOf(noteBarStore.load())
        private set

    /** The tools on the note bar, in order. */
    var barTools by mutableStateOf(noteBarStore.loadTools())
        private set

    /** Whether the note bar shows the note's title. */
    var showTitle by mutableStateOf(noteBarStore.loadShowTitle())
        private set

    /** The edge of the window the note bar sits against. */
    var barPosition by mutableStateOf(noteBarStore.loadPosition())
        private set

    /** When the note bar steps away from the page. */
    var autoHide by mutableStateOf(noteBarStore.loadAutoHide())
        private set

    /** Whether a new note starts with finger drawing on. A note can still switch it from its menu. */
    var fingerDraws by mutableStateOf(inputStore.loadFingerDraws())
        private set

    /** What a stroke that looks like a shape turns into: offered, snapped on hold, or left alone. */
    var shapeDetection by mutableStateOf(inputStore.loadShapeDetection())
        private set

    /** What each finger gesture does on the note screen. */
    var gestures by mutableStateOf(gestureStore.load())
        private set

    /** The action [g] has now, its default when none is set. */
    fun gestureAction(g: NoteGesture): GestureAction = gestures[g] ?: g.default

    /** The edge the pen wheel is docked on now. */
    fun wheelEdge(): DockEdge = wheelStore.load().edge

    /** Docks the pen wheel on [edge]; an open note moves it at once. */
    fun changeWheelEdge(edge: DockEdge) = wheelStore.dockOn(edge)

    fun changeAppearance(next: Appearance) {
        appearance = next
        appearanceStore.save(next)
    }

    fun changeNoteBar(style: NoteBarStyle) {
        noteBar = style
        noteBarStore.save(style)
    }

    fun changeBarTools(tools: List<NoteBarTool>) {
        barTools = tools
        noteBarStore.saveTools(tools)
    }

    fun changeShowTitle(show: Boolean) {
        showTitle = show
        noteBarStore.saveShowTitle(show)
    }

    fun changeBarPosition(position: BarPosition) {
        barPosition = position
        noteBarStore.savePosition(position)
    }

    fun changeAutoHide(next: AutoHide) {
        autoHide = next
        noteBarStore.saveAutoHide(next)
    }

    fun changeFingerDraws(on: Boolean) {
        fingerDraws = on
        inputStore.saveFingerDraws(on)
    }

    fun changeShapeDetection(mode: ShapeDetection) {
        shapeDetection = mode
        inputStore.saveShapeDetection(mode)
    }

    fun changeGesture(g: NoteGesture, action: GestureAction) {
        gestures = gestures + (g to action)
        gestureStore.save(g, action)
    }
}
