package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.onemnemo.grapho.note.CropMenu
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.PageLayers
import com.onemnemo.grapho.note.SelectionMenu
import com.onemnemo.grapho.note.ShapeDetection
import com.onemnemo.grapho.note.ShapeSuggestion
import com.onemnemo.grapho.note.TextEdit

/** What is open or switched on over one note, shared by the screen, the canvas and the overlays. */
@Stable
internal class NoteUiState {
    var fingerDraws by mutableStateOf(false)

    /** What a stroke that looks like a shape turns into, from Settings. */
    var shapeDetection by mutableStateOf(ShapeDetection.SNAP_ON_HOLD)

    /** The shape offered for the stroke just drawn, if any. */
    var shapeSuggestion by mutableStateOf<ShapeSuggestion?>(null)
    var rulerOn by mutableStateOf(false)
    var protractorOn by mutableStateOf(false)
    var wheelOpen by mutableStateOf(false)
    var wheelCustomise by mutableStateOf<CustomiseRequest?>(null)
    var showPaperColor by mutableStateOf(false)
    var selectionMenu by mutableStateOf<SelectionMenu?>(null)
    var cropMenu by mutableStateOf<CropMenu?>(null)
    var pasteAt by mutableStateOf<Offset?>(null)
    var layersOpen by mutableStateOf(false)
    var pageLayers by mutableStateOf<PageLayers?>(null)
    var insertOpen by mutableStateOf(false)

    /** Where the bar's Layers button is in the window (More on a side bar), so the panel opens from it. */
    var layersAnchor by mutableStateOf<Rect?>(null)

    /** Where the bar's Shapes button is in the window, so the options card opens from it. Null when the bar has none. */
    var shapesAnchor by mutableStateOf<Rect?>(null)

    /** The Shapes tool's options card is open. */
    var shapeOptionsOpen by mutableStateOf(false)

    /** Where the bar's Text button is in the window, so the options card opens from it. Null when the bar has none. */
    var textAnchor by mutableStateOf<Rect?>(null)

    /** The Text tool's options card is open. */
    var textOptionsOpen by mutableStateOf(false)

    /** The text box being typed into, or null. The canvas keeps it up to date. */
    var textEdit by mutableStateOf<TextEdit?>(null)
    var imageToolOpen by mutableStateOf(false)
    var fullScreen by mutableStateOf(false)

    /** Whether the bar is away, and why: the pen is down, it was called back, or a menu on it is open. */
    val barVisibility = BarVisibility()

    /** Calls a hidden bar back when the pen hovers near its edge. */
    val penHover = PenHover()

    /** Read mode: the page only scrolls and zooms. Not saved, each time the note opens it starts in edit mode. */
    var readMode by mutableStateOf(false)

    /** The zero-based page in view, kept up to date while reading. */
    var readPage by mutableStateOf(0)

    /** Not state: the view is only needed to call into it. */
    val view = arrayOfNulls<NoteCanvasView>(1)

    /** True while something is open that Back closes before it leaves the note. */
    val dismissable: Boolean get() = !readMode && (cardOpen || cropMenu != null || selectionMenu != null)

    private val cardOpen: Boolean get() = layersOpen || shapeOptionsOpen || textOptionsOpen || textEdit != null

    /** Switches to read mode, putting away everything that belongs to drawing: the wheel, panels, menus and selection. */
    fun enterRead() {
        view[0]?.run {
            commitText()
            finishCrop(false)
            clearSelection()
            readPage = currentPage()
        }
        wheelOpen = false
        layersOpen = false
        insertOpen = false
        shapeOptionsOpen = false
        textOptionsOpen = false
        imageToolOpen = false
        selectionMenu = null
        cropMenu = null
        pasteAt = null
        shapeSuggestion = null
        readMode = true
    }

    /** Back to editing. */
    fun exitRead() {
        readMode = false
    }

    /**
     * Closes the topmost of the options cards, then the layers panel, then the text box being typed into, then crop,
     * then the selection, the order Back follows.
     */
    fun dismissTop() {
        when {
            shapeOptionsOpen -> shapeOptionsOpen = false
            textOptionsOpen -> textOptionsOpen = false
            layersOpen -> layersOpen = false
            textEdit != null -> view[0]?.commitText()
            cropMenu != null -> view[0]?.finishCrop(false)
            else -> view[0]?.clearSelection()
        }
    }
}
