package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.StrokeId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** How long the strength slider and outline choice must rest before the picture is cut again. */
private const val CUTOUT_PAUSE_MS = 350L

/**
 * The bars that ride on the page: the selection bar, the crop bar and the paste offer, and a picture's working
 * state or its Compare chip. They are placed in the canvas's own pixels and stay inside [insets], which keeps them
 * clear of a floating note bar.
 */
@Composable
internal fun NoteCardLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    diagramCleaner: DiagramCleaner,
    imageReplacer: ImageReplacer,
    selectionImager: SelectionImager,
    insets: EdgeInsets,
) {
    val context = LocalContext.current
    ui.selectionMenu?.let { menu ->
        SelectionMenuCard(
            bounds = menu.bounds,
            insets = insets,
            canColour = menu.canColour,
            canCrop = menu.canCrop,
            palette = toolbar.palette,
            recent = toolbar.recent,
            customStart = toolbar.pen.color,
            onColour = { ui.view[0]?.recolorSelection(it) },
            onCustomColour = {
                toolbar.addRecent(it)
                ui.view[0]?.recolorSelection(it)
            },
            onCrop = { ui.view[0]?.beginCrop() },
            onRemoveBackground = diagramCleaner::removeBackground,
            onConvertToInk = diagramCleaner::convertToInk,
            onCopy = { ui.view[0]?.copySelection() },
            onDuplicate = { ui.view[0]?.duplicateSelection() },
            layers = ui.pageLayers ?: ui.view[0]?.layers?.current(),
            thickness = menu.width,
            onThickness = { ui.view[0]?.setSelectionWidth(it) },
            more = SelectionMoreActions(
                onCut = { ui.view[0]?.cutSelection() },
                onReplaceImage = imageReplacer::replace,
                onBringToFront = { ui.view[0]?.orderSelection(true) },
                onSendToBack = { ui.view[0]?.orderSelection(false) },
                onMoveToLayer = { ui.view[0]?.moveSelectionToLayer(it) },
                onMoveToNewLayer = {
                    val name = ui.pageLayers?.nextName { context.getString(R.string.layer_default_name, it) }
                    if (name != null) ui.view[0]?.moveSelectionToNewLayer(name)
                },
                onFlipHorizontally = { ui.view[0]?.flipSelection(true) },
                onFlipVertically = { ui.view[0]?.flipSelection(false) },
                onCopyAsImage = selectionImager::copy,
                onExportSelection = selectionImager::export,
            ),
            onDelete = { ui.view[0]?.deleteSelection() },
        )
    }
    ui.cropMenu?.let { menu ->
        CropCard(
            bounds = menu.bounds,
            insets = insets,
            onReset = { ui.view[0]?.resetCrop() },
            onCancel = { ui.view[0]?.finishCrop(false) },
            onDone = { ui.view[0]?.finishCrop(true) },
        )
    }
    ui.pasteAt?.let { at ->
        PasteCard(
            x = at.x,
            y = at.y,
            insets = insets,
            onPaste = {
                ui.pasteAt = null
                ui.view[0]?.paste(at.x, at.y)
            },
        )
    }
    ui.shapeSuggestion?.let { suggestion ->
        ShapeSuggestCard(
            suggestion = suggestion,
            insets = insets,
            onAccept = { ui.view[0]?.acceptShapeSuggestion() },
            onDismiss = { ui.view[0]?.dismissShapeSuggestion() },
        )
    }
    TextEditLayer(ui, toolbar, insets)
    diagramCleaner.job?.let { job ->
        val rect = rememberImageRect({ ui.view[0] }, job.page, job.id)
        PictureWorking(job, rect, insets, onCancel = diagramCleaner::cancel)
    }
    val offer = diagramCleaner.compare
    if (offer != null && diagramCleaner.job == null && offer.ids.size >= 2) {
        val image = StrokeId(offer.ids[0], offer.ids[1])
        val showing = diagramCleaner.peeking ||
            (ui.selectionMenu != null && ui.view[0]?.selectedImage()?.id == image)
        if (showing) {
            val rect = rememberImageRect({ ui.view[0] }, offer.page, image, keepLast = true)
            val crop = remember(image) {
                RectF(0f, 0f, 1f, 1f).also { ui.view[0]?.imageCrop(offer.page, image, it) }
            }
            // Leaving the picture writes what the brushes painted.
            DisposableEffect(image) { onDispose { diagramCleaner.finishBrush() } }
            PictureCompare(
                rect,
                insets,
                diagramCleaner.options,
                onOptions = diagramCleaner::adjust,
                brush = diagramCleaner.brush,
                crop = crop,
                onTool = diagramCleaner::chooseTool,
                onDone = diagramCleaner::finishBrush,
                onHold = diagramCleaner::peek,
            )
        }
    }
    // A pause in the changes cuts the picture again, once any cut that is running has finished.
    LaunchedEffect(diagramCleaner.options) {
        delay(CUTOUT_PAUSE_MS)
        snapshotFlow { diagramCleaner.job }.first { it == null }
        diagramCleaner.reapply()
    }
}
