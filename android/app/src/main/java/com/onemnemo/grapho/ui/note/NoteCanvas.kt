package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.viewinterop.AndroidView
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LayerBlock
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.PageLayers
import com.onemnemo.grapho.ui.components.Notices

/**
 * The page, as the canvas view, with everything it reports wired into [ui]. [onFingerTap] hears taps of two or
 * more fingers. The view fills its parent, whose pixels are the cards' and the wheel's pixels.
 */
@Composable
internal fun NoteCanvas(
    notebook: Notebook,
    session: NoteSession,
    notices: Notices,
    toolbar: ToolbarState,
    ui: NoteUiState,
    onFingerTap: (fingers: Int) -> Unit,
) {
    val fingerTap by rememberUpdatedState(onFingerTap)
    val focusManager = LocalFocusManager.current
    DisposableEffect(Unit) {
        onDispose {
            ui.view[0]?.run {
                commitText()
                onTextEdit = null
                onChanged = null
                onFillFailed = null
                onInteraction = null
                onWriting = null
                onPenHover = null
                this.onFingerTap = null
                onSelectionChanged = null
                onCropChanged = null
                onPasteOffer = null
                onLayerBlocked = null
                onLayerLimit = null
                onGuidesChanged = null
                onShapeSuggest = null
                onPageInView = null
                onFirstFrame = null
                layers.onChanged = null
            }
            ui.view[0] = null
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            NoteCanvasView(ctx, notebook).also { view ->
                // Fills change the document like any other edit, so undo and redo refresh here too.
                view.onChanged = { session.onEdited() }
                view.onFillFailed = { notices.show(ctx.getString(R.string.note_fill_not_closed)) }
                // Touching the page puts the wheel away and ends a rename in the bar.
                view.onInteraction = {
                    // Typing into a text box goes on while the page is panned.
                    if (ui.textEdit == null) focusManager.clearFocus()
                    ui.pasteAt = null
                    if (ui.wheelOpen && !toolbar.wheelPinned && !toolbar.wheelCarrying) ui.wheelOpen = false
                }
                view.onWriting = { ui.barVisibility.penDown = it }
                view.onPenHover = { x, y -> ui.penHover.at(x, y) }
                view.onFingerTap = { fingerTap(it) }
                view.onSelectionChanged = { ui.selectionMenu = it }
                view.onCropChanged = { ui.cropMenu = it }
                view.onTextEdit = { ui.textEdit = it }
                view.onPasteOffer = { x, y -> ui.pasteAt = Offset(x, y) }
                view.onLayerBlocked = { block ->
                    val message = if (block == LayerBlock.HIDDEN) {
                        R.string.layer_hidden_notice
                    } else {
                        R.string.layer_locked_notice
                    }
                    notices.show(ctx.getString(message))
                }
                view.onLayerLimit = {
                    notices.show(ctx.getString(R.string.layer_limit_notice, PageLayers.MAX_LAYERS))
                }
                view.onGuidesChanged = { ruler, protractor ->
                    ui.rulerOn = ruler
                    ui.protractorOn = protractor
                }
                view.onPageInView = { ui.readPage = it }
                view.onShapeSuggest = { ui.shapeSuggestion = it }
                view.onFirstFrame = { session.onCanvasShown() }
                view.layers.onChanged = { ui.pageLayers = view.layers.current() }
                ui.view[0] = view
            }
        },
        update = { view ->
            view.toolState = toolbar.toolState
            view.fingerDraws = ui.fingerDraws
            view.shapeDetection = ui.shapeDetection
            view.readOnly = ui.readMode
            view.rulerShown = ui.rulerOn && !ui.readMode
            view.protractorShown = ui.protractorOn && !ui.readMode
        },
    )
}
