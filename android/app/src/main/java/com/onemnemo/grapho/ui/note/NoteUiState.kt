package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
    var pageLayers by mutableStateOf<PageLayers?>(null)

    /**
     * The one pane of the bar that is open, or null. Only one is ever open, so opening one closes the last, and a
     * button that opened a pane closes it again on the next tap.
     */
    var pane by mutableStateOf<BarPane?>(null)

    /** Where the bar is in the window, so a touch on it is left to its buttons. */
    var barBounds by mutableStateOf<Rect?>(null)

    /** Where an options card open over the page is in the window, so a touch inside it does not close it. */
    var cardBounds by mutableStateOf<Rect?>(null)

    var layersOpen: Boolean
        get() = pane == BarPane.Layers
        set(value) = setPane(BarPane.Layers, value)
    var insertOpen: Boolean
        get() = pane == BarPane.Insert
        set(value) = setPane(BarPane.Insert, value)
    var moreOpen: Boolean
        get() = pane == BarPane.More
        set(value) = setPane(BarPane.More, value)

    /** Where the bar's Layers button is in the window (More on a side bar), so the panel opens from it. */
    var layersAnchor by mutableStateOf<Rect?>(null)

    /** Where the bar's Shapes button is in the window, so the options card opens from it. Null when the bar has none. */
    var shapesAnchor by mutableStateOf<Rect?>(null)

    /** The Shapes tool's options card is open. */
    var shapeOptionsOpen: Boolean
        get() = pane == BarPane.Shapes
        set(value) = setPane(BarPane.Shapes, value)

    /** Where the bar's Text button is in the window, so the options card opens from it. Null when the bar has none. */
    var textAnchor by mutableStateOf<Rect?>(null)

    /**
     * Where the bar's Pen, Highlighter, Eraser and Lasso buttons are in the window, so their options cards open from
     * them. A tool the bar does not show has no entry.
     */
    val toolAnchors = mutableStateMapOf<NoteBarTool, Rect>()

    /** The Text tool's options card is open. */
    var textOptionsOpen: Boolean
        get() = pane == BarPane.Text
        set(value) = setPane(BarPane.Text, value)

    /** The text box being typed into, or null. The canvas keeps it up to date. */
    var textEdit by mutableStateOf<TextEdit?>(null)
    var imageToolOpen: Boolean
        get() = pane == BarPane.Image
        set(value) = setPane(BarPane.Image, value)
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

    private val cardOpen: Boolean get() = pane != null || textEdit != null

    /** Switches to read mode, putting away everything that belongs to drawing: the wheel, panels, menus and selection. */
    fun enterRead() {
        view[0]?.run {
            commitText()
            finishCrop(false)
            clearSelection()
            readPage = currentPage()
        }
        wheelOpen = false
        pane = null
        selectionMenu = null
        cropMenu = null
        pasteAt = null
        shapeSuggestion = null
        readMode = true
    }

    /** Keeps where [tool]'s button is, writing only when it moved so the map does not churn every layout. */
    fun placeTool(tool: NoteBarTool, bounds: Rect) {
        if (toolAnchors[tool] != bounds) toolAnchors[tool] = bounds
    }

    /** Opens [which] when [open], closing whatever pane was open; closes it when it is the one open. */
    fun setPane(which: BarPane, open: Boolean) {
        if (open) {
            pane = which
        } else if (pane == which) {
            pane = null
        }
    }

    /** Opens [which], or closes it when it is already open. */
    fun togglePane(which: BarPane) = setPane(which, pane != which)

    /**
     * A touch landed outside the bar and outside the open card: every pane goes away but the layers panel, which
     * stays beside the page while you work on its layers and closes from its own button.
     */
    fun closePanes() {
        if (pane != null && pane != BarPane.Layers) pane = null
    }

    /** Back to editing. */
    fun exitRead() {
        readMode = false
    }

    /**
     * Closes the open pane of the bar, then the text box being typed into, then crop, then the selection, the order
     * Back follows.
     */
    fun dismissTop() {
        when {
            pane != null -> pane = null
            textEdit != null -> view[0]?.commitText()
            cropMenu != null -> view[0]?.finishCrop(false)
            else -> view[0]?.clearSelection()
        }
    }
}

/**
 * The panes that open from the note bar. Menus and option cards share one slot, so there is never more than one,
 * and every one of them closes the same way: its own button again, a touch anywhere off the bar and off the pane,
 * or Back. Layers is the one exception to the touch: it stays open while you draw. Pen is the options card of both
 * the pen and the highlighter.
 */
internal enum class BarPane { Image, Insert, More, Layers, Shapes, Text, Pen, Eraser, Lasso }
