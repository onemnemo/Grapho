package com.onemnemo.grapho.ui.note

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.DecodedPixels
import com.onemnemo.grapho.note.DiagramPixels
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.SelectedImage
import com.onemnemo.grapho.note.StrokeId
import com.onemnemo.grapho.ui.components.Notices
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a picture job does to the picture. */
enum class PictureJobKind { RemoveBackground, ConvertToInk }

/** A job that is running on [id], the picture on [page], and the [label] the pill shows for it. */
class PictureJob(val kind: PictureJobKind, @StringRes val label: Int, val page: Int, val id: StrokeId)

/**
 * Cleans up the selected picture: strips its background, or turns the drawing in it into ink. [job] is the one
 * running, or null when idle. The heavy work runs off the UI thread and can be cancelled; the canvas is only touched
 * again once the engine has finished, and the picture is held out of reach while it works.
 */
@Stable
class DiagramCleaner {
    var job by mutableStateOf<PictureJob?>(null)
        internal set

    internal var compare by mutableStateOf<CompareOffer?>(null)

    /** How the background is cut. The panel under the picture edits it; [applied] is what the cut-out on show used. */
    internal var options by mutableStateOf(CutoutOptions())
        private set
    internal var applied = CutoutOptions()

    /** The hand brushes for touching the cut-out up. Their strokes are written to the picture by [finishBrush]. */
    internal val brush = CutoutBrush()

    /** True while the original is shown in place of the cut-out. */
    internal var peeking by mutableStateOf(false)
        private set

    internal var launchRemoveBackground: () -> Unit = {}
    internal var launchReapply: () -> Unit = {}
    internal var launchBrush: (List<BrushStroke>) -> Unit = {}
    internal var launchToInk: () -> Unit = {}
    internal var stop: () -> Unit = {}
    internal var showOriginal: () -> Unit = {}
    internal var showResult: () -> Unit = {}

    fun removeBackground() {
        if (job == null) {
            options = CutoutOptions()
            launchRemoveBackground()
        }
    }

    internal fun adjust(next: CutoutOptions) {
        options = next
    }

    /** Picks the tool. Going back to Auto writes what was painted to the picture. */
    internal fun chooseTool(tool: CutoutTool) {
        if (tool == brush.tool) return
        if (tool == CutoutTool.Auto) finishBrush() else brush.tool = tool
    }

    /** Leaves the brushes and writes what was painted to the picture as one undo step. */
    internal fun finishBrush() {
        val strokes = brush.finish()
        if (strokes.isNotEmpty() && job == null) launchBrush(strokes)
    }

    /** Cuts the original again with the current [options], if they differ from the cut-out on show. */
    internal fun reapply() {
        if (job == null && compare != null && !peeking && options != applied) launchReapply()
    }

    fun convertToInk() {
        if (job == null) launchToInk()
    }

    /** Stops the running job. Whatever it had already done to the note is taken back. */
    fun cancel() = stop()

    /** Shows the original while [on], and the cut-out again after. */
    internal fun peek(on: Boolean) {
        if (on == peeking) return
        if (on) showOriginal() else showResult()
    }

    internal fun setPeeking(on: Boolean) {
        peeking = on
    }
}

/** [view] is read when the work is done, since the canvas may have been made again by then. */
@Composable
fun rememberDiagramCleaner(
    session: NoteSession,
    view: () -> NoteCanvasView?,
    notices: Notices,
    onEdited: () -> Unit,
): DiagramCleaner {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cleaner = remember { DiagramCleaner() }
    val running = remember { arrayOfNulls<Job>(1) }

    fun run(
        kind: PictureJobKind,
        @StringRes message: Int,
        @StringRes failure: Int,
        edit: (SelectedImage, Notebook, DecodedPixels) -> LongArray,
    ) {
        val canvas = view() ?: return
        val image = canvas.selectedImage() ?: return
        cleaner.compare = null
        cleaner.job = PictureJob(kind, message, image.page, image.id)
        canvas.lockImage(image.id)
        running[0] = scope.launch {
            // The engine call can't be interrupted, so a cancel that lands during it is undone afterwards.
            var applied = false
            try {
                val bytes = session.withNotebook { it.imageBytes(image.source) }
                    ?: throw IOException("The picture is no longer in the note.")
                ensureActive()
                val pixels = withContext(Dispatchers.Default) { DiagramPixels.decode(bytes) }
                ensureActive()
                val ids = session.withNotebook { notebook ->
                    edit(image, notebook, pixels).also { applied = it.isNotEmpty() }
                }
                ensureActive()
                view()?.lockImage(null)
                if (ids.isEmpty()) {
                    notices.show(context.getString(failure), isError = true)
                } else {
                    view()?.let { current ->
                        current.selectEdited(image.page, ids)
                        if (kind == PictureJobKind.RemoveBackground) {
                            cleaner.compare = CompareOffer(
                                image.page,
                                ids,
                                current.editGeneration,
                                image.idPair,
                                image.source,
                                current.selectedImage()?.source.orEmpty(),
                            )
                        }
                    }
                    onEdited()
                }
            } catch (e: CancellationException) {
                view()?.lockImage(null)
                if (applied) {
                    withContext(NonCancellable) {
                        view()?.undo()
                        onEdited()
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "couldn't clean up image ${image.source}", e)
                notices.show(context.getString(R.string.diagram_failed), isError = true)
            } finally {
                view()?.lockImage(null)
                cleaner.job = null
                running[0] = null
            }
        }
    }

    cleaner.launchRemoveBackground = {
        val options = cleaner.options
        cleaner.applied = options
        run(
            PictureJobKind.RemoveBackground,
            R.string.diagram_removing_background,
            R.string.diagram_failed,
        ) { image, notebook, pixels ->
            notebook.removeImageBackground(
                image.page,
                image.idPair,
                pixels,
                options.strength,
                options.outline.code,
            )
        }
    }
    cleaner.launchReapply = {
        val offer = cleaner.compare
        val canvas = view()
        if (offer != null && canvas != null) {
            if (offer.stillCurrent(canvas.editGeneration)) {
                // Back to the original, which the cut starts from again.
                canvas.undo()
                canvas.selectEdited(offer.page, offer.original)
                onEdited()
                if (canvas.selectedImage() != null) cleaner.launchRemoveBackground() else cleaner.compare = null
            } else {
                cleaner.compare = null
            }
        }
    }
    cleaner.launchBrush = { strokes ->
        val offer = cleaner.compare
        val canvas = view()
        if (offer != null && canvas != null && !cleaner.peeking && offer.cutSource.isNotEmpty()) {
            if (offer.stillCurrent(canvas.editGeneration)) {
                // The painted cut-out takes the place of the first cut, so Compare still goes straight to the original.
                canvas.undo()
                canvas.selectEdited(offer.page, offer.original)
                onEdited()
                if (canvas.selectedImage() != null) {
                    run(
                        PictureJobKind.RemoveBackground,
                        R.string.picture_brushing,
                        R.string.diagram_failed,
                    ) { image, notebook, original ->
                        val bytes = notebook.imageBytes(offer.cutSource)
                            ?: throw IOException("The picture is no longer in the note.")
                        val cut = DiagramPixels.decode(bytes)
                        if (cut.width != original.width || cut.height != original.height) {
                            throw IOException("The cut-out is not the size of the picture.")
                        }
                        BrushMath.apply(strokes, cut.argb, original.argb, cut.width, cut.height)
                        notebook.replaceImagePixels(image.page, image.idPair, cut)
                    }
                } else {
                    cleaner.compare = null
                }
            } else {
                cleaner.compare = null
            }
        }
    }
    cleaner.launchToInk = {
        run(
            PictureJobKind.ConvertToInk,
            R.string.diagram_converting,
            R.string.diagram_nothing_found,
        ) { image, notebook, pixels ->
            notebook.imageToInk(image.page, image.idPair, pixels, System.currentTimeMillis())
        }
    }
    cleaner.stop = { running[0]?.cancel() }
    cleaner.showOriginal = {
        val offer = cleaner.compare
        val canvas = view()
        if (offer != null && canvas != null) {
            if (offer.stillCurrent(canvas.editGeneration)) {
                canvas.undo()
                cleaner.setPeeking(true)
                onEdited()
            } else {
                cleaner.compare = null
            }
        }
    }
    cleaner.showResult = {
        val offer = cleaner.compare
        val canvas = view()
        cleaner.setPeeking(false)
        if (offer != null && canvas != null) {
            canvas.redo()
            canvas.selectEdited(offer.page, offer.ids)
            offer.generation = canvas.editGeneration
            onEdited()
        }
    }
    return cleaner
}

private const val TAG = "DiagramCleaner"
