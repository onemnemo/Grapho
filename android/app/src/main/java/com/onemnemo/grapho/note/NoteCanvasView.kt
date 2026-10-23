package com.onemnemo.grapho.note

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewConfiguration
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.input.motionprediction.MotionEventPredictor
import com.onemnemo.grapho.engine.NativeNotebook
import com.onemnemo.grapho.engine.Tool
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp
import kotlin.math.hypot

enum class ToolMode { PEN, HIGHLIGHTER, ERASER, FILL, LASSO, SHAPE, TEXT }

/**
 * [color] is ARGB (highlighter colours include alpha, e.g. 0x73F5D33F), [widthPt] in document points. For FILL, [color]
 * is the fill colour and [widthPt] is unused. The eraser settings ride along in every mode because the pen's side
 * button erases too: [eraserPt] is the eraser diameter in points, and [eraserArea] erases only the part under it.
 * [line] is the pen or highlighter's line style, and the Shapes tool draws with it too; every other tool is solid.
 * [shape] is what the Shapes tool draws, and [text] what the Text tool starts a box with.
 */
data class ToolState(
    val mode: ToolMode,
    val color: Int,
    val widthPt: Float,
    val eraserArea: Boolean = false,
    val eraserPt: Float = 16f,
    val line: LineStyle = LineStyle.SOLID,
    val shape: ShapeSettings = ShapeSettings(),
    val text: TextSettings = TextSettings(),
    /** The lasso picks a dragged rectangle instead of a freehand loop. */
    val lassoRect: Boolean = false,
)

/** What the current touch is doing. */
private enum class Mode {
    /** Nothing in progress. */
    NONE,

    /** The touch is ignored until it ends (palm, or pen down in the gap between pages). */
    IGNORE,

    /** One or more fingers panning and zooming. */
    NAVIGATE,

    /** A wet stroke is being drawn. */
    INK,

    /** The eraser is down. */
    ERASE,

    /** A lasso is being traced; lifting in place clears the selection or offers to paste. */
    LASSO,

    /** One or two fingers are moving or turning the ruler or protractor. */
    GUIDE,

    /** The selection's box, a corner handle or the rotate handle is being dragged. */
    LASSO_DRAG,

    /** A handle or the frame of the crop editor is being dragged. */
    CROP_DRAG,

    /** The bucket is down; it fills only if the touch ends without moving. */
    FILL,

    /** The Text tool is down; it opens a box only if the touch ends without moving. */
    TEXT,

    /** A pen or drawing finger landed on a button; it only counts if it stays put. */
    TAP,
}

/**
 * A stroke lifted after the newest finished render, drawn directly until a render includes it. [alpha] is the
 * opacity of the layer it went to.
 */
private class PendingStroke(val page: Int, val stroke: CachedStroke, val generation: Int, val alpha: Int)

/**
 * The page canvas: finished ink in the normal layer, the stroke being drawn
 * straight in the front buffer for low latency, and all the touch handling
 * that goes with it.
 *
 * The front buffer is on screen while it is drawn into, so each frame only
 * clears and redraws the small area that changed. Clearing the whole buffer
 * makes the stroke flicker.
 *
 * Finished pages are drawn into a bitmap on a background thread ([DryCache]).
 * The normal layer only blits that bitmap, so it stays cheap however full the
 * pages are and never holds up the wet stroke on the shared render thread.
 */
@SuppressLint("ViewConstructor")
class NoteCanvasView(context: Context, val notebook: Notebook) :
    SurfaceView(context),
    SurfaceHolder.Callback {

    var toolState: ToolState = ToolState(ToolMode.PEN, 0xFF1A1D26.toInt(), 1.6f)
        set(value) {
            // A selection survives a switch to the lasso, but not to a tool that would draw over it.
            if (field.mode != value.mode && value.mode != ToolMode.LASSO) lasso.clear()
            if (field.mode != value.mode) clearShapeOffer()
            // The box being typed into is saved when the person moves to another tool.
            if (field.mode == ToolMode.TEXT && value.mode != ToolMode.TEXT) textEditor.commit()
            field = value
            lasso.rectangle = value.lassoRect
        }

    /** What a stroke that looks like a shape turns into. See [ShapeDetection]. */
    var shapeDetection: ShapeDetection = ShapeDetection.SNAP_ON_HOLD
        set(value) {
            field = value
            if (!value.suggests) clearShapeOffer()
        }

    /** Called on the UI thread with the shape offered for the stroke just drawn, or null when the offer goes. */
    var onShapeSuggest: ((ShapeSuggestion?) -> Unit)? = null

    /** When false (default) only the stylus draws and fingers pan and zoom. When true one finger draws and two pan. */
    var fingerDraws: Boolean = false

    /**
     * Read mode: nothing draws, selects, guides or adds a page. Stylus and finger alike only pan and zoom, and
     * turning it on drops any selection.
     */
    var readOnly: Boolean = false
        set(value) {
            if (value && !field) {
                lasso.clear()
                clearShapeOffer()
                textEditor.commit()
            }
            field = value
        }

    /** Called on the UI thread when panning brings another page to the middle, with its zero-based index. */
    var onPageInView: ((Int) -> Unit)? = null

    /** Called once, on the main thread, after the first page's strokes have loaded and a redraw is queued. */
    var onFirstFrame: (() -> Unit)? = null

    private var reportedPage = -1

    /** Called on the UI thread after any edit (stroke added, erase, undo, redo, page added). */
    var onChanged: (() -> Unit)? = null

    /** Called on the UI thread when a bucket tap finds no enclosed area to fill. */
    var onFillFailed: (() -> Unit)? = null

    /**
     * Called on the UI thread at the start of every touch, e.g. to close popups. Erasing with the pen's side button
     * or its eraser end does not count, so the pen wheel stays as it was.
     */
    var onInteraction: (() -> Unit)? = null

    /**
     * Called on the UI thread when the pen or a finger starts or stops marking the page: a stroke, the eraser, a
     * lasso or a fill. Panning and zooming do not count.
     */
    var onWriting: ((Boolean) -> Unit)? = null

    /** Called on the UI thread while the pen hovers over the page, with where it is in window pixels. */
    var onPenHover: ((Float, Float) -> Unit)? = null
    private val inWindow = IntArray(2)

    private var writing = false

    /** Called on the UI thread when two or more fingers tap the page, with how many. */
    var onFingerTap: ((fingers: Int) -> Unit)? = null

    /** Called on the UI thread when the selection menu should move, show (bounds in view pixels) or hide (null). */
    var onSelectionChanged: ((SelectionMenu?) -> Unit)? = null

    /** Called on the UI thread when a text box opens for typing (the edit), changes its style, or closes (null). */
    var onTextEdit: ((TextEdit?) -> Unit)? = null

    /** Called on the UI thread when the crop card should move, show (picture bounds in view pixels) or hide (null). */
    var onCropChanged: ((CropMenu?) -> Unit)? = null

    /** Called on the UI thread when a tap on empty page could paste, with the tap in view pixels. */
    var onPasteOffer: ((x: Float, y: Float) -> Unit)? = null

    /** Called on the UI thread when a stroke, fill, erase or paste starts on a hidden or locked active layer. */
    var onLayerBlocked: ((LayerBlock) -> Unit)? = null

    /** Called on the UI thread when a layer cannot be added because the page is at its limit. */
    var onLayerLimit: (() -> Unit)? = null

    /** Called on the UI thread when a guide is closed from its own button, with which of the two are still on. */
    var onGuidesChanged: ((ruler: Boolean, protractor: Boolean) -> Unit)? = null

    /** Whether the ruler is on the page. It is a session tool: nothing about it is saved in the note. */
    var rulerShown: Boolean
        get() = guides.rulerOn
        set(value) = guides.setRuler(value)

    /** Whether the protractor is on the page. */
    var protractorShown: Boolean
        get() = guides.protractorOn
        set(value) = guides.setProtractor(value)

    // Written on the UI thread, read by the render thread. Each is replaced
    // as a whole, never changed in place.
    @Volatile private var info: NotebookInfo = notebook.info()

    @Volatile private var viewport = Viewport(trueSize = resources.displayMetrics.xdpi / 72f)
        set(value) {
            field = value
            layers.viewMoved(value)
            pdf.moved()
        }
    private val cache = PageCache(notebook)
    private val painter = DocumentPainter()
    private val imagesArrived = AtomicBoolean(false)
    private val images: ImageCache = ImageCache(notebook) {
        if (imagesArrived.compareAndSet(false, true)) {
            post {
                imagesArrived.set(false)
                onImagesReady()
            }
        }
    }
    private val pdf = PdfLayer(this, notebook, File(context.cacheDir, "pdf"), { viewport }, { info }, ::onPdfReady)
    private val dry = DryCache(images) { post { onDryPublished() } }
    private val previews = PagePreviews(images) { post { onPreviewPublished() } }
    private var imageEpoch = 0 // UI thread only; counts pictures that finished decoding
    private val covered = RectF() // render thread only
    private val geometry = GeometryLoader(cache, { post(it) }, ::onGeometryPublished)
    private val lassoPainter = LassoPainter(resources.displayMetrics.density, images) // render thread only
    private val cropPainter = CropPainter(resources.displayMetrics.density, images) // render thread only
    private val guidePainter = GuidePainter(resources.displayMetrics.density) // render thread only
    private val guideHost: GuideHost = object : GuideHost {
        override val currentViewport: Viewport get() = this@NoteCanvasView.viewport
        override val density: Float get() = resources.displayMetrics.density
        override val touchSlop: Float get() = this@NoteCanvasView.touchSlop.toFloat()
        override val paper: Int get() = info.background.color

        override fun redraw() = this@NoteCanvasView.redraw()

        override fun guidesChanged() {
            onGuidesChanged?.invoke(guides.rulerOn, guides.protractorOn)
        }
    }
    private val guides: GuideController = GuideController(guideHost)
    private val guidePoints = FloatArray(4)
    private val lassoHost: LassoHost = object : LassoHost {
        override val currentViewport: Viewport get() = this@NoteCanvasView.viewport
        override val editCount: Int get() = generation
        override val density: Float get() = resources.displayMetrics.density

        override fun pageId(page: Int): String? = info.pageIds.getOrNull(page)

        override fun images(pageId: String): List<SceneImage> = cache.scenes[pageId]?.images ?: emptyList()

        override fun redraw() = this@NoteCanvasView.redraw()

        override fun selectionShown() {
            // The crop editor cannot outlive the selection it was opened on.
            if (crop != null && lasso.selection == null) endCrop()
            requestDry()
            this@NoteCanvasView.redraw()
        }

        override fun documentEdited(page: Int) {
            refreshPage(page)
            contentChanged()
            notifyChanged()
        }

        override fun menuChanged(menu: SelectionMenu?) {
            onSelectionChanged?.invoke(menu)
        }

        override fun pasteOffered(x: Float, y: Float) {
            onPasteOffer?.invoke(x, y)
        }
    }
    private val lasso: LassoController = LassoController(notebook, lassoHost)
    private val textHost: TextHost = object : TextHost {
        override val currentViewport: Viewport get() = this@NoteCanvasView.viewport

        override fun pageId(page: Int): String? = info.pageIds.getOrNull(page)

        override fun texts(pageId: String): List<SceneText> = cache.scenes[pageId]?.texts ?: emptyList()

        override fun blocked(page: Int): Boolean = layerBlocked(page)

        override fun editChanged(edit: TextEdit?) {
            onTextEdit?.invoke(edit)
            requestDry()
            this@NoteCanvasView.redraw()
        }

        override fun documentEdited(page: Int) {
            refreshPage(page)
            contentChanged()
            notifyChanged()
        }
    }
    private val textEditor = TextEditor(notebook, textHost)
    private val cropHost: CropHost = object : CropHost {
        override val currentViewport: Viewport get() = this@NoteCanvasView.viewport
        override val density: Float get() = resources.displayMetrics.density

        override fun redraw() = this@NoteCanvasView.redraw()

        override fun menuChanged(menu: CropMenu?) {
            onCropChanged?.invoke(menu)
        }
    }

    // The crop editor, or null. Replaced as a whole; the render thread draws it.
    @Volatile private var crop: CropController? = null
    private val layerHost = object : LayerHost {
        override fun layersEdited(page: Int, reload: Boolean) {
            lasso.clear()
            if (reload) reloadLayers(page)
            notifyChanged()
        }

        override fun opacityChanged(page: Int, visibleIndex: Int, alpha: Int, commit: Boolean) {
            info.pageIds.getOrNull(page)?.let { cache.setLayerAlpha(it, visibleIndex, alpha) }
            contentChanged()
            if (commit) notifyChanged() else notebook.markChanged()
        }
    }

    /** The layers of the page in the middle of the view; the layer panel reads and edits them here. */
    val layers = LayerEditor(notebook, layerHost)

    // A background scene that arrives while the pen is inking waits until it lifts: showing it means a
    // render of the multi-buffered layer, which would hide the wet stroke in the front buffer.
    private var publishWaiting = false
    private val stallWatch = StallWatch()
    private val bitmapPaint = Paint().apply { isFilterBitmap = true }
    private val pageScratch = RectF() // render thread only

    // Counts edits. A render knows which edits it contains by its generation.
    private var generation = 0

    // Strokes lifted since the newest render, replaced as a whole like the scenes.
    @Volatile private var pending: List<PendingStroke> = emptyList()

    // While the view moves, the page is rendered again at most once per frame so newly revealed areas fill in
    // under the finger. Sharp PDF details still wait until the view has settled.
    private var movingDryQueued = false
    private val movingDry = Runnable {
        movingDryQueued = false
        requestDry(settled = false)
    }
    private val settle = Runnable { requestDry() }

    // An eraser can change the page on every input event; the page is read again once per frame, off the
    // UI thread, and shown when that finishes.
    private var erasedPage = -1
    private var erasedShowQueued = false
    private val showErased = Runnable {
        erasedShowQueued = false
        requestGeometry(erasedPage, false)
    }

    private var laidOut = false
    private val predictor = MotionEventPredictor.newInstance(this)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val edgeHold = EdgeHold(this, touchSlop.toFloat()) { handleTouch(it) }

    // A tap of several fingers wobbles a little more than one finger does.
    private val fingerTap = resources.displayMetrics.xdpi.let { dpi ->
        FingerTap(touchSlop * FINGER_TAP_SLOP, FINGER_MAX_MM * dpi / MM_PER_INCH, FINGER_SPREAD_MM * dpi / MM_PER_INCH)
    }
    private var fingerTapStart = viewport
    private var fingerTapBlocked = false

    // Touch state, UI thread only.
    private var mode = Mode.NONE
    private var activePointerId = -1
    private var activeIsStylus = false
    private var activeTool = toolState
    private var downTime = 0L
    private var startedAtMs = 0L
    private var wetPage = -1
    private var wet: WetStroke? = null

    // The eraser's circle as `[x, y, radius]` in view pixels, or null. Drawn with the multi-buffered layer, not
    // the front buffer: switching between the two on every frame while the eraser changes the page stalls the front
    // buffer.
    @Volatile private var eraserMark: FloatArray? = null

    // A snapped shape while the pen is still down, drawn with the multi-buffered layer for the
    // same reason as the eraser's circle: reshaping repaints the whole shape every move, and
    // clearing that much of the front buffer while it is on screen flickers.
    @Volatile private var liveShape: WetFrame? = null
    private val markPaint = Paint().apply {
        color = 0xFF50545E.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    // Set once the wet stroke has been snapped to a shape: the shape's points in view pixels.
    private var snapped: FloatArray? = null
    private var holdTried = false
    private var snappedLine: FloatArray? = null

    // Set while the Shapes tool is dragging: the shape follows the pen instead of the pen's own ink.
    private var shapeDrag: ShapeDrag? = null

    // The shape offered for the newest freehand stroke, for the person to accept.
    private val shapeOffer = ShapeOffer(SUGGEST_MIN_DP * resources.displayMetrics.density)

    // The newest stroke when it was snapped to a straight line, in page-local document units,
    // so a head drawn at its end next can become an arrow head.
    private var lastLine: FloatArray? = null
    private var lastLinePage = -1
    private val holdCheck = Runnable { checkHold() }
    private var erasedAny = false
    private var hasPrevErase = false
    private var prevEraseX = 0f
    private var prevEraseY = 0f
    private var tapCandidate = false
    private var tapX = 0f
    private var tapY = 0f
    private var tapTime = 0L
    private var lastStylusTime = Long.MIN_VALUE / 2
    private var lastMoveTime = 0L
    private var flingVx = 0f
    private var flingVy = 0f
    private var flingTime = 0L
    private var flinging = false
    private val gesture = FloatArray(3) // centroid x, centroid y, span
    private var lastCx = 0f
    private var lastCy = 0f
    private var lastSpan = 0f

    private val callback = object : CanvasFrontBufferedRenderer.Callback<WetFrame> {
        override fun onDrawFrontBufferedLayer(canvas: Canvas, bufferWidth: Int, bufferHeight: Int, param: WetFrame) {
            val (frame, dirty) = takePending() ?: return
            canvas.save()
            canvas.clipRect(dirty)
            canvas.drawColor(Color.TRANSPARENT, BlendMode.CLEAR)
            canvas.clipRect(frame.clip)
            frame.draw(canvas)
            canvas.restore()
        }

        override fun onDrawMultiBufferedLayer(
            canvas: Canvas,
            bufferWidth: Int,
            bufferHeight: Int,
            params: Collection<WetFrame>,
        ) {
            val snapshot = dry.current
            if (snapshot == null) {
                // First frame, before any background render has finished.
                painter.draw(canvas, viewport, info, cache.scenes, null, pdf.backgrounds, images = images)
            } else {
                drawSnapshot(canvas, snapshot)
            }
            liveShape?.let {
                canvas.save()
                canvas.clipRect(it.clip)
                it.draw(canvas)
                canvas.restore()
            }
            eraserMark?.let { canvas.drawCircle(it[0], it[1], it[2], markPaint) }
            val cropping = crop
            if (cropping != null) {
                cropPainter.draw(canvas, viewport, info, cropping)
            } else {
                lassoPainter.draw(canvas, viewport, info, cache.scenes, snapshot, lasso.selection, lasso.trace)
            }
            guidePainter.draw(canvas, viewport, info.background.color, guides.guides)
            multiInFlight = false
        }
    }

    private var renderer: CanvasFrontBufferedRenderer<WetFrame>? = null

    /** Blits the render mapped onto the current viewport, then the strokes it does not contain yet. */
    private fun drawSnapshot(canvas: Canvas, snapshot: RenderedDry) {
        val cur = viewport
        val old = snapshot.viewport
        val inf = info
        canvas.drawColor(if (cur.infinite) inf.background.color else PaperChrome.desk(inf.background.color))
        // Under the render, so wherever a zoom out or pan has moved past its edges there is paper and soft ink.
        painter.drawBlankPages(canvas, cur, inf)
        painter.drawPdfPages(canvas, cur, inf, pdf.backgrounds)
        val ratio = cur.scale / old.scale
        val left = cur.offsetX - old.offsetX * ratio
        val top = cur.offsetY - old.offsetY * ratio
        covered.set(left, top, left + old.viewWidth * ratio, top + old.viewHeight * ratio)
        previews.draw(canvas, cur, inf, covered, bitmapPaint)
        canvas.save()
        canvas.translate(cur.offsetX, cur.offsetY)
        canvas.scale(ratio, ratio)
        canvas.translate(-old.offsetX, -old.offsetY)
        canvas.drawBitmap(snapshot.bitmap, 0f, 0f, bitmapPaint)
        canvas.restore()
        val s = cur.scale
        for (p in pending) {
            if (p.generation <= snapshot.generation) continue
            canvas.save()
            if (cur.infinite) {
                canvas.translate(cur.offsetX, cur.offsetY)
                canvas.scale(s, s)
            } else {
                cur.pageRect(p.page, pageScratch)
                canvas.clipRect(pageScratch)
                canvas.translate(pageScratch.left, pageScratch.top)
                canvas.scale(s, s)
            }
            if (p.alpha < LAYER_OPAQUE) {
                val saved = canvas.saveLayerAlpha(p.stroke.bounds, p.alpha)
                canvas.drawPath(p.stroke.path, p.stroke.paint)
                canvas.restoreToCount(saved)
            } else {
                canvas.drawPath(p.stroke.path, p.stroke.paint)
            }
            canvas.restore()
        }
    }

    private fun requestDry(settled: Boolean = true) {
        if (settled) pdf.settled()
        val hidden = lasso.hidden() ?: textEditor.hidden()
        dry.request(viewport, info, cache.scenes, generation, hidden)
        previews.request(viewport, info, cache.scenes, hidden, imageEpoch)
    }

    // Like a background scene, a PDF page that arrives while the pen is inking waits until it lifts.
    private fun onPdfReady() {
        // PDF pages are drawn under the render on every frame, so a new bitmap only needs a frame.
        if (mode == Mode.INK) publishWaiting = true else redraw()
    }

    // Like a background scene, a picture that finishes decoding while the pen is inking waits until it lifts.
    private fun onImagesReady() {
        imageEpoch++
        if (mode == Mode.INK) publishWaiting = true else requestDry(settled = false)
    }

    // A preview only shows where the render does not reach, which while the view rests is nowhere.
    private fun onPreviewPublished() {
        if (mode != Mode.INK && dry.current?.viewport !== viewport) redraw()
    }

    private fun onDryPublished() {
        val g = dry.current?.generation ?: return
        val p = pending
        if (p.any { it.generation <= g }) pending = p.filter { it.generation > g }
        redraw()
    }

    /** The document changed: later renders must include it, and strokes the edit removed must go. */
    private fun contentChanged() {
        // Undo, erase or fill may have removed the line an arrow head would attach to.
        lastLine = null
        generation++
        val p = pending
        if (p.isNotEmpty()) {
            val scenes = cache.scenes
            val ids = info.pageIds
            pending = p.filter { ps ->
                val scene = ids.getOrNull(ps.page)?.let { scenes[it] }
                scene != null && scene.strokes.any { it === ps.stroke }
            }
        }
        requestDry()
        redraw()
    }

    private fun settleSoon() {
        removeCallbacks(settle)
        postDelayed(settle, SETTLE_MS)
    }

    private fun settleNow() {
        removeCallbacks(settle)
        requestDry()
    }

    // Input can arrive faster than frames render. Instead of queueing every
    // frame (which builds up lag), keep only the newest one and the union of
    // all areas changed since the last draw, with at most one render queued.
    private val lock = Any()
    private var latest: WetFrame? = null
    private val pendingDirty = RectF()
    private var renderQueued = false
    private var queuedAt = 0L
    private var lostRenders = 0 // UI thread only

    private fun takePending(): Pair<WetFrame, Rect>? = synchronized(lock) {
        renderQueued = false
        val frame = latest ?: return null
        // Whole pixels only. A clip with fractional edges is antialiased, so
        // the clear would only half-erase the edge pixels and the redraw
        // would only half-cover them, leaving faint light lines across the
        // stroke wherever an old dirty area ended.
        val dirty = Rect().also { pendingDirty.roundOut(it) }
        pendingDirty.setEmpty()
        frame to dirty
    }

    // Called before every commit or cancel. Those drop a render that is still
    // queued, so its callback never runs to clear [renderQueued]; left set, it
    // would stop every later stroke from rendering until the pen lifts.
    private fun resetPending() = synchronized(lock) {
        latest = null
        pendingDirty.setEmpty()
        renderQueued = false
    }

    init {
        viewport = viewport.withStructure(info)
        cache.setPaper(info.background.color)
        loadPagesInBackground()
        holder.addCallback(this)
    }

    // Pages load one after the other on a background thread, with quick geometry, so opening a big note does not
    // hold the screen. Each page shows as soon as it is ready and its exact outlines follow through [geometry].
    private var openLoader: ExecutorService? = null

    private fun loadPagesInBackground() {
        val ids = info.pageIds
        if (ids.isEmpty()) {
            post { announceFirstFrame() }
            return
        }
        val loader = Executors.newSingleThreadExecutor()
        openLoader = loader
        ids.forEachIndexed { index, id ->
            try {
                loader.execute {
                    val load = try {
                        cache.compute(index, null, 0)
                    } catch (e: RuntimeException) {
                        Log.e("GraphoOpen", "page $index failed to load", e)
                        null
                    }
                    post {
                        if (openLoader !== loader) return@post
                        if (load != null && cache.publish(id, null, load.scene)) {
                            geometry.settle(id, index)
                            contentChanged()
                        }
                        if (index == 0) announceFirstFrame()
                    }
                }
            } catch (_: RejectedExecutionException) {
                // The view went away in between.
            }
        }
    }

    private fun announceFirstFrame() {
        onFirstFrame?.let { done ->
            onFirstFrame = null
            done()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resetPending()
        renderer = CanvasFrontBufferedRenderer(this, callback).also {
            it.renderMultiBufferedLayer(emptyList())
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer?.renderMultiBufferedLayer(emptyList())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderer?.release(true)
        renderer = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        val resized = viewport.resized(w.toFloat(), h.toFloat())
        // Fit once; after that keep whatever zoom the user chose.
        viewport = if (laidOut) resized else resized.fitted()
        laidOut = true
        requestDry()
        lasso.syncMenu()
        crop?.syncMenu()
        redraw()
    }

    override fun onDetachedFromWindow() {
        edgeHold.clear()
        flinging = false
        removeCallbacks(flingStep)
        cancelDrawing()
        images.shutdown()
        removeCallbacks(settle)
        removeCallbacks(movingDry)
        movingDryQueued = false
        removeCallbacks(redrawFrame)
        openLoader?.shutdownNow()
        openLoader = null
        dry.shutdown()
        previews.shutdown()
        pdf.shutdown()
        geometry.shutdown()
        stallWatch.stop()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        stallWatch.start()
        info.pageIds.forEachIndexed { index, id -> geometry.settle(id, index) }
        pdf.settled()
    }

    // The multi-buffered layer draws into a pool of three buffers that come back only once the screen has shown
    // them. Asking for renders faster than that empties the pool and its thread then waits for a buffer for good.
    // So the layer is drawn at most once per frame, and only after the last render has finished, however many
    // changes ask for it.
    private var redrawQueued = false

    @Volatile private var multiInFlight = false
    private var multiRequestedAt = 0L
    private val redrawFrame = Runnable {
        redrawQueued = false
        val r = renderer ?: return@Runnable
        val now = SystemClock.uptimeMillis()
        if (multiInFlight && now - multiRequestedAt < STUCK_MS) {
            redraw()
            return@Runnable
        }
        multiInFlight = true
        multiRequestedAt = now
        r.renderMultiBufferedLayer(emptyList())
    }

    private fun redraw() {
        if (redrawQueued) return
        redrawQueued = true
        postOnAnimation(redrawFrame)
    }

    private fun renderWet(frame: WetFrame) {
        val queue = synchronized(lock) {
            latest = frame
            pendingDirty.union(frame.dirty)
            // A render that hasn't run for a while was dropped somewhere; ask again.
            val now = SystemClock.uptimeMillis()
            val lost = renderQueued && now - queuedAt > STUCK_MS
            when {
                lost -> lostRenders++
                !renderQueued -> lostRenders = 0
            }
            (!renderQueued || lost).also {
                if (it) queuedAt = now
                renderQueued = true
            }
        }
        if (lostRenders >= MAX_LOST_RENDERS) {
            // The renderer has stopped drawing the front buffer. Start a new one rather than leave the page frozen.
            Log.w(EraseLog.TAG, "front buffer stopped drawing, restarting the renderer")
            restartRenderer()
            return
        }
        if (queue) renderer?.renderFrontBufferedLayer(frame)
    }

    private fun restartRenderer() {
        resetPending()
        lostRenders = 0
        renderer?.release(true)
        renderer = CanvasFrontBufferedRenderer(this, callback).also {
            it.renderMultiBufferedLayer(emptyList())
        }
    }

    fun undo() {
        if (lasso.isLocked) return
        lasso.clear()
        clearShapeOffer()
        afterHistory(NativeNotebook.nativeUndo(notebook.nativeHandle))
    }

    fun redo() {
        if (lasso.isLocked) return
        lasso.clear()
        clearShapeOffer()
        afterHistory(NativeNotebook.nativeRedo(notebook.nativeHandle))
    }

    fun recolorSelection(argb: Int) = lasso.recolor(argb)

    fun copySelection() = lasso.copy()

    /** Duplicates onto the active layer, which must take drawing, so a hidden or locked one is explained instead. */
    fun duplicateSelection() {
        val page = lasso.selection?.page ?: return
        if (layerBlocked(page)) return
        lasso.duplicate()
    }

    fun cutSelection() = lasso.cut()

    fun flipSelection(horizontal: Boolean) = lasso.flip(horizontal)

    fun orderSelection(front: Boolean) = lasso.reorder(front)

    fun setSelectionWidth(width: Float) = lasso.setWidth(width)

    /** The selection drawn alone on a transparent background, or null if nothing is selected or it will not draw. */
    fun renderSelection(): Bitmap? {
        val sel = lasso.selection ?: return null
        val scene = cache.scenes[sel.pageId] ?: return null
        return SelectionRender.render(scene, sel.ids, sel.bounds, images, info.background.color)
    }

    /** Puts the selection on the layer at [index] of its page, which must take ink. Returns false if it cannot. */
    fun moveSelectionToLayer(index: Int): Boolean {
        val sel = lasso.selection ?: return false
        if (layers.page != sel.page || !layers.setActive(index)) return false
        lasso.moveToActiveLayer(sel)
        return true
    }

    /**
     * Puts the selection on a new layer named [name], above the active one. Returns false, after telling the person
     * why, when the page has no room for another layer. Copies and removes, so Undo takes it back in steps.
     */
    fun moveSelectionToNewLayer(name: String): Boolean {
        val sel = lasso.selection ?: return false
        if (layers.page != sel.page) return false
        if (!layers.add(name)) {
            onLayerLimit?.invoke()
            return false
        }
        lasso.moveToActiveLayer(sel)
        return true
    }

    /** The selected image's page, id and frame in page-local units, or null unless exactly one image is selected. */
    fun selectedImageFrame(): SelectedFrame? {
        val sel = lasso.selection ?: return null
        val image = lasso.singleImage() ?: return null
        return SelectedFrame(sel.page, image.id, RectF(image.bounds))
    }

    /** The picture [old] was swapped for the new [added] one on [page]: remove it and select the new one. */
    fun imageReplaced(page: Int, old: LongArray, added: LongArray) = lasso.replaceWith(page, old, added)

    /** Whether the active layer of the page in view takes a new picture; if not, the person is told why. */
    fun activeLayerAccepts(): Boolean = !layerBlocked(viewport.centrePage())

    fun deleteSelection() = lasso.delete()

    /** Remembers what the field over the open text box holds. */
    fun textTyped(text: String) = textEditor.typed(text)

    /** Ends the text edit in progress, if any, writing the box to the note. */
    fun commitText() = textEditor.commit()

    /** Draws and saves the open text box with [style] from now on. */
    fun setTextStyle(style: TextSettings) = textEditor.restyle(style)

    /** Moves the open text box. See [TextEditor.move]. */
    fun moveText(dx: Float, dy: Float) = textEditor.move(dx, dy)

    /** Drags a side of the open text box. See [TextEditor.stretch]. */
    fun stretchText(fromLeft: Boolean, delta: Float) = textEditor.stretch(fromLeft, delta)

    /** The paper colour of the note, which text colours are adapted to. */
    val paperColor: Int get() = info.background.color

    /** Where the open text box is in view pixels. See [TextEditor.placement]. False when none is open. */
    fun textPlacement(out: FloatArray): Boolean = textEditor.placement(out)

    fun clearSelection() = lasso.clear()

    /** Opens the crop editor on the selected image. Does nothing unless exactly one image is selected. */
    fun beginCrop() {
        val sel = lasso.selection ?: return
        val image = lasso.singleImage() ?: return
        crop = CropController(sel.page, image, cropHost).also {
            lasso.holdMenu(true)
            it.syncMenu()
        }
        redraw()
    }

    /** Closes the crop editor. With [apply] the frame becomes the image's crop, as one undo step. */
    fun finishCrop(apply: Boolean) {
        val editor = crop ?: return
        val id = editor.image.id
        val r = editor.rect
        val ids = if (apply && editor.changed()) {
            NativeNotebook.nativeCropImage(
                notebook.nativeHandle,
                editor.page,
                longArrayOf(id.hi, id.lo),
                floatArrayOf(r.left, r.top, r.right, r.bottom),
            )
        } else {
            LongArray(0)
        }
        endCrop()
        if (ids.size >= 2) lasso.adoptEdit(editor.page, ids)
    }

    /** Puts the crop frame back around the whole picture. */
    fun resetCrop() {
        crop?.reset()
    }

    private fun endCrop() {
        crop = null
        onCropChanged?.invoke(null)
        lasso.holdMenu(false)
        redraw()
    }

    /**
     * Where a picture of [pixelsWide] by [pixelsHigh] goes: centred in the visible part of the page in view, covering
     * at most about 60% of it. Returns null, after telling the person why, when that page's active layer is hidden or
     * locked.
     */
    fun imagePlacement(pixelsWide: Int, pixelsHigh: Int): ImagePlacement? {
        val vp = viewport
        val page = vp.centrePage()
        if (layerBlocked(page)) return null
        val area =
            RectF(
                vp.localX(page, 0f),
                vp.localY(page, 0f),
                vp.localX(page, vp.viewWidth),
                vp.localY(page, vp.viewHeight),
            )
        val size = if (vp.infinite) null else info.pageSizes.getOrNull(page)
        if (size != null && !area.intersect(0f, 0f, size.width, size.height)) area.set(0f, 0f, size.width, size.height)
        return ImageSizing.place(page, area.left, area.top, area.width(), area.height(), pixelsWide, pixelsHigh)
    }

    /** Selects an image that was just embedded on [page] and makes sure the note is saved. */
    fun imageInserted(page: Int, ids: LongArray) = lasso.adoptEdit(page, ids)

    /** The one image that is selected, or null when the selection is anything else. */
    fun selectedImage(): SelectedImage? {
        val sel = lasso.selection ?: return null
        val image = lasso.singleImage() ?: return null
        return SelectedImage(sel.page, image.id, image.source)
    }

    /** Counts edits to the note. Two reads that match mean nothing was changed in between. */
    val editGeneration: Int get() = generation

    /** Holds [id] out of reach while a picture job works on it: it cannot be selected, and undo and redo wait. */
    fun lockImage(id: StrokeId?) = lasso.lockImage(id)

    /** Where the image [id] on [page] is in view pixels, written to [out]. False when it isn't on the page. */
    fun imageViewRect(page: Int, id: StrokeId, out: RectF): Boolean = lasso.imageViewRect(page, id, out)

    /** The part of its source the image [id] on [page] shows, as fractions, written to [out]. False if none. */
    fun imageCrop(page: Int, id: StrokeId, out: RectF): Boolean = lasso.imageCrop(page, id, out)

    /** Selects what an edit made outside the lasso just created on [page], e.g. the strokes traced from a picture. */
    fun selectEdited(page: Int, ids: LongArray) = lasso.adoptEdit(page, ids)

    /** Pastes the clipboard centred on a point in view pixels and selects what was pasted. */
    fun paste(x: Float, y: Float) {
        val page = viewport.pageAt(x, y)
        if (page >= 0 && layerBlocked(page)) return
        lasso.paste(x, y)
    }

    fun addPage() {
        lasso.clear()
        val index = NativeNotebook.nativeAddPage(notebook.nativeHandle)
        if (index < 0) return
        info = notebook.info()
        viewport = viewport.withStructure(info).revealed(index)
        info.pageIds.getOrNull(index)?.let { cache.refresh(it, index) }
        contentChanged()
        notifyChanged()
    }

    /** The page nearest the middle of the view, where pages are inserted after. */
    fun currentPage(): Int = viewport.centrePage()

    /** Inserts a blank page after the page in view and scrolls to it. Returns false on an infinite canvas. */
    fun insertBlankPage(): Boolean {
        if (viewport.infinite) return false
        lasso.clear()
        val index = notebook.insertPage(currentPage() + 1)
        if (index < 0) return false
        structureChanged(index)
        return true
    }

    /**
     * The pages were added, removed or reordered: reads the structure again, drops what was loaded for pages
     * by position, and scrolls to [reveal] if it is a page. Called after inserting pages.
     */
    fun structureChanged(reveal: Int) {
        lasso.clear()
        // A refresh still running or queued reads its page by the position it had before, which has shifted.
        geometry.shutdown()
        info = notebook.info()
        viewport = viewport.withStructure(info)
        cache.sync(info.pageIds)
        info.pageIds.forEachIndexed { index, id -> geometry.settle(id, index) }
        if (reveal in info.pageIds.indices) viewport = viewport.revealed(reveal)
        layers.refresh()
        contentChanged()
        notifyChanged()
    }

    /** Re-reads structure and redraws, e.g. after the background or title changed. */
    fun reload() {
        lasso.clear()
        info = notebook.info()
        viewport = viewport.withStructure(info)
        cache.setPaper(info.background.color)
        cache.sync(info.pageIds)
        layers.refresh()
        contentChanged()
    }

    private fun afterHistory(page: Int) {
        val before = info
        val after = notebook.info()
        // Undoing or redoing an inserted page changes the structure whatever page the core reports.
        val restructured = after.pageIds != before.pageIds ||
            after.pageSizes != before.pageSizes ||
            after.pdfRefs != before.pdfRefs
        if (page < 0 && !restructured) return
        info = after
        viewport = viewport.withStructure(info)
        val id = info.pageIds.getOrNull(page)
        if (restructured || id == null) {
            // Refreshes by page position that are still running would read the wrong page.
            geometry.shutdown()
            cache.sync(info.pageIds)
            info.pageIds.forEachIndexed { index, pageId -> geometry.settle(pageId, index) }
        } else {
            cache.refresh(id, page)
            geometry.settle(id, page)
        }
        // Show where the change happened if it is off screen.
        if (id != null && !viewport.infinite && !viewport.isPageVisible(page)) {
            viewport = viewport.revealed(page)
        }
        layers.refresh()
        contentChanged()
        notifyChanged()
    }

    private fun refreshPage(page: Int) {
        info.pageIds.getOrNull(page)?.let {
            cache.refresh(it, page)
            geometry.settle(it, page)
        }
    }

    private fun requestGeometry(page: Int, upgrade: Boolean) {
        info.pageIds.getOrNull(page)?.let { geometry.request(it, page, upgrade) }
    }

    /** Shows a layer's edit: the page is read again, in the background when strokes have to be built. */
    private fun reloadLayers(page: Int) {
        val id = info.pageIds.getOrNull(page) ?: return
        if (!cache.relayer(id, page)) geometry.request(id, page, false)
        contentChanged()
    }

    /** Warns the person and returns true when [page]'s active layer is hidden or locked, so nothing can be drawn. */
    private fun layerBlocked(page: Int): Boolean {
        val block = layers.block(page)
        if (block == LayerBlock.NONE) return false
        onLayerBlocked?.invoke(block)
        return true
    }

    private fun notifyChanged() {
        notebook.markChanged()
        onChanged?.invoke()
    }

    private fun isStylus(type: Int) = type == MotionEvent.TOOL_TYPE_STYLUS || type == MotionEvent.TOOL_TYPE_ERASER

    private fun noteStylus(event: MotionEvent) {
        for (i in 0 until event.pointerCount) {
            if (isStylus(event.getToolType(i))) lastStylusTime = event.eventTime
        }
    }

    // The pen hovering means a hand is likely about to rest on the screen.
    override fun onHoverEvent(event: MotionEvent): Boolean {
        noteStylus(event)
        val action = event.actionMasked
        val moving = action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE
        if (moving && isStylus(event.getToolType(0))) {
            getLocationInWindow(inWindow)
            onPenHover?.invoke(inWindow[0] + event.x, inWindow[1] + event.y)
        }
        return super.onHoverEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        noteStylus(event)
        return super.onGenericMotionEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        noteStylus(event)
        if (!edgeHold.filter(event)) handleTouch(event)
        return true
    }

    private fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event)
            MotionEvent.ACTION_MOVE -> onMove(event)
            MotionEvent.ACTION_POINTER_UP -> onPointerUp(event)
            MotionEvent.ACTION_UP -> onUp(event)
            MotionEvent.ACTION_CANCEL -> onCancel()
        }
        trackFingerTap(event)
        reportWriting()
    }

    private fun reportWriting() {
        val now = mode == Mode.INK || mode == Mode.ERASE || mode == Mode.LASSO || mode == Mode.FILL
        if (now != writing) {
            writing = now
            onWriting?.invoke(now)
        }
    }

    // Runs after the touch was handled, so a tap pans nothing in the end and leaves the ruler and crop alone.
    private fun trackFingerTap(event: MotionEvent) {
        val i = event.actionIndex
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    fingerTapStart = viewport
                    fingerTapBlocked = readOnly || crop != null || mode == Mode.GUIDE || mode == Mode.TAP
                }
                // A hand that lands while the pen hovers, or right after it lifts, is most likely resting to write.
                val pen = isStylus(event.getToolType(i)) || event.eventTime - lastStylusTime < PALM_REJECT_MS
                fingerTap.down(
                    event.getPointerId(i),
                    event.getX(i),
                    event.getY(i),
                    event.eventTime,
                    pen,
                    event.getTouchMajor(i),
                )
            }

            MotionEvent.ACTION_MOVE -> for (p in 0 until event.pointerCount) {
                fingerTap.move(event.getPointerId(p), event.getX(p), event.getY(p), event.getTouchMajor(p))
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                // The system's own palm rejection lifts a palm with this flag instead of a cancel.
                val canceled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    event.flags and MotionEvent.FLAG_CANCELED != 0
                val fingers = fingerTap.up(event.eventTime, canceled)
                if (fingers > 0 && !fingerTapBlocked) fingerTapped(fingers)
            }

            MotionEvent.ACTION_CANCEL -> fingerTap.cancel()
        }
    }

    private fun fingerTapped(fingers: Int) {
        // The fingers may have nudged the view while they were down.
        if (viewport != fingerTapStart) {
            viewport = fingerTapStart
            reportPageInView()
            redraw()
            settleNow()
        }
        onFingerTap?.invoke(fingers)
    }

    /** The pen is down with its side button held, or with its eraser end. */
    private fun erasesByPen(event: MotionEvent): Boolean = isStylus(event.getToolType(0)) &&
        (
            event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER ||
                event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0
            )

    private fun onDown(event: MotionEvent) {
        stopFling()
        clearShapeOffer()
        if (!erasesByPen(event)) onInteraction?.invoke()
        lasso.touching(true)
        crop?.touching(true)
        mode = Mode.NONE
        tapCandidate = false
        val stylus = isStylus(event.getToolType(0))
        // A palm usually lands right after the pen, so fingers that follow it are ignored.
        if (!stylus && event.eventTime - lastStylusTime < PALM_REJECT_MS) {
            mode = Mode.IGNORE
            return
        }
        tapX = event.x
        tapY = event.y
        tapTime = event.eventTime
        val vp = viewport
        crop?.let {
            onCropDown(event, it, stylus)
            return
        }
        if (!readOnly && !stylus && !fingerDraws && guides.begin(event.x, event.y)) {
            activePointerId = event.getPointerId(0)
            activeIsStylus = false
            mode = Mode.GUIDE
            return
        }
        if (!readOnly && (stylus || fingerDraws)) {
            // Deliver moves as they arrive instead of batching them per frame.
            requestUnbufferedDispatch(event)
            if (vp.hitsAddTarget(event.x, event.y)) {
                mode = Mode.TAP
                tapCandidate = true
                return
            }
            val plain = event.getToolType(0) != MotionEvent.TOOL_TYPE_ERASER &&
                event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY == 0
            if (plain && lasso.beginDrag(event.x, event.y)) {
                // The box and its handles may stick out past the page's edge.
                activePointerId = event.getPointerId(0)
                activeIsStylus = stylus
                mode = Mode.LASSO_DRAG
                tapCandidate = true
                return
            }
            // A selection made by tapping an image does not outlive the next stroke.
            if (plain && toolState.mode != ToolMode.LASSO) lasso.clear()
            val page = vp.pageAt(event.x, event.y)
            if (page < 0) {
                mode = Mode.IGNORE
                return
            }
            beginDrawing(event, page, stylus)
        } else if (!readOnly && lasso.beginDrag(event.x, event.y)) {
            // A finger on the selection moves it even when fingers do not draw.
            activePointerId = event.getPointerId(0)
            activeIsStylus = false
            mode = Mode.LASSO_DRAG
        } else {
            mode = Mode.NAVIGATE
            resetVelocity(event)
            // A tap that stays put adds a page on the target, or selects the image under it.
            tapCandidate = true
            measure(event, -1)
            lastCx = gesture[0]
            lastCy = gesture[1]
            lastSpan = gesture[2]
        }
    }

    // The crop editor takes the touch: a handle or the frame drags, a finger elsewhere pans and zooms, a pen is idle.
    private fun onCropDown(event: MotionEvent, editor: CropController, stylus: Boolean) {
        if (editor.beginDrag(event.x, event.y)) {
            activePointerId = event.getPointerId(0)
            activeIsStylus = stylus
            mode = Mode.CROP_DRAG
        } else if (stylus) {
            mode = Mode.IGNORE
        } else {
            startNavigating(event)
        }
    }

    private fun beginDrawing(event: MotionEvent, page: Int, stylus: Boolean) {
        activePointerId = event.getPointerId(0)
        activeIsStylus = stylus
        downTime = event.eventTime
        startedAtMs = System.currentTimeMillis()
        wetPage = page
        activeTool = toolState
        // The side button only ever erases, from the first touch, so a quick tap erases too.
        if (erasesByPen(event) || activeTool.mode == ToolMode.ERASER) {
            beginErase(event, 0)
        } else if (activeTool.mode == ToolMode.FILL) {
            mode = if (layerBlocked(page)) Mode.IGNORE else Mode.FILL
            tapCandidate = mode == Mode.FILL
        } else if (activeTool.mode == ToolMode.TEXT) {
            mode = Mode.TEXT
            tapCandidate = true
        } else if (activeTool.mode == ToolMode.LASSO) {
            tapCandidate = true
            if (lasso.beginDrag(event.x, event.y)) {
                mode = Mode.LASSO_DRAG
            } else if (layerBlocked(page)) {
                mode = Mode.IGNORE
            } else {
                mode = Mode.LASSO
                lasso.beginTrace(event.x, event.y, page)
            }
        } else if (layerBlocked(page)) {
            mode = Mode.IGNORE
        } else {
            mode = Mode.INK
            snapped = null
            holdTried = false
            val scale = viewport.scale
            val code = if (activeTool.mode == ToolMode.HIGHLIGHTER) Tool.HIGHLIGHTER else Tool.PEN
            val stroke = WetStroke(
                activeTool.widthPt * scale,
                code,
                displayColor(activeTool.color, info.background.color),
                activeTool.line,
            )
            wet = stroke
            if (activeTool.mode == ToolMode.SHAPE) {
                // The shape is drawn from the drag, not from the pen's path, so the pen's own ink never shows.
                shapeDrag = ShapeDrag(activeTool.shape.spec(scale, activeTool.widthPt), event.x, event.y)
                stroke.push(pack(event, 0))
            } else {
                shapeDrag = null
                guides.beginStroke(stroke, event.x, event.y)
                ink(event, 0)
            }
        }
    }

    /** Moves the end of the shape being dragged to the pen, and shows the shape that results. */
    private fun dragShape(event: MotionEvent, pointer: Int) {
        val stroke = wet ?: return
        val drag = shapeDrag ?: return
        stroke.push(pack(event, pointer))
        val shape = drag.shapeTo(
            stroke,
            event.getX(pointer),
            event.getY(pointer),
            viewport.pageRect(wetPage, RectF()),
        )
        snapped = shape?.points
        snappedLine = shape?.line
        showLiveShape(shape?.frame)
    }

    private fun beginErase(event: MotionEvent, pointer: Int) {
        if (layerBlocked(wetPage)) {
            mode = Mode.IGNORE
            return
        }
        mode = Mode.ERASE
        erasedAny = false
        hasPrevErase = false
        NativeNotebook.nativeBeginErase(notebook.nativeHandle, wetPage, activeTool.eraserArea)
        erase(event, pointer)
    }

    private fun onPointerDown(event: MotionEvent) {
        tapCandidate = false
        when (mode) {
            Mode.INK, Mode.ERASE, Mode.FILL, Mode.TEXT, Mode.LASSO, Mode.LASSO_DRAG, Mode.CROP_DRAG -> {
                // A pen keeps drawing when a hand touches down; a finger turns into a pan/zoom.
                if (activeIsStylus) return
                cancelDrawing()
                startNavigating(event)
            }

            Mode.NAVIGATE -> startNavigating(event)

            Mode.GUIDE -> guides.rebase(guidePoints, touchPoints(event, -1))

            else -> Unit
        }
    }

    private fun startNavigating(event: MotionEvent) {
        mode = Mode.NAVIGATE
        resetVelocity(event)
        measure(event, -1)
        lastCx = gesture[0]
        lastCy = gesture[1]
        lastSpan = gesture[2]
    }

    private fun onPointerUp(event: MotionEvent) {
        if (mode == Mode.GUIDE) {
            guides.rebase(guidePoints, touchPoints(event, event.actionIndex))
            return
        }
        if (mode != Mode.NAVIGATE) return
        // The remaining fingers become the new reference, so the view doesn't jump.
        resetVelocity(event)
        measure(event, event.actionIndex)
        lastCx = gesture[0]
        lastCy = gesture[1]
        lastSpan = gesture[2]
    }

    private fun onMove(event: MotionEvent) {
        if (tapCandidate && hypot(event.x - tapX, event.y - tapY) > touchSlop) tapCandidate = false
        when (mode) {
            Mode.INK -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) {
                    when {
                        shapeDrag != null -> dragShape(event, idx)
                        snapped == null -> ink(event, idx)
                        else -> reshape(event.getX(idx), event.getY(idx))
                    }
                }
            }

            Mode.ERASE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) erase(event, idx)
            }

            Mode.LASSO -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) lasso.extendTrace(event, idx)
            }

            Mode.LASSO_DRAG -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) lasso.drag(event.getX(idx), event.getY(idx))
            }

            Mode.CROP_DRAG -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx >= 0) crop?.drag(event.getX(idx), event.getY(idx))
            }

            Mode.GUIDE -> guides.update(guidePoints, touchPoints(event, -1))

            Mode.NAVIGATE -> navigate(event)

            else -> Unit
        }
    }

    private fun onUp(event: MotionEvent) {
        when (mode) {
            Mode.INK -> finishInk(event)

            Mode.ERASE -> finishErase(event)

            Mode.FILL -> if (tapCandidate) fillAt(tapX, tapY, wetPage)

            Mode.TEXT -> if (tapCandidate) textEditor.tap(tapX, tapY, wetPage, activeTool.text)

            Mode.LASSO -> lasso.endTrace(tapCandidate, tapX, tapY)

            Mode.LASSO_DRAG -> lasso.endDrag()

            Mode.CROP_DRAG -> crop?.endDrag()

            Mode.GUIDE -> guides.end()

            Mode.TAP, Mode.NAVIGATE -> {
                if (tapCandidate && event.eventTime - tapTime < TAP_MS) tapped()
                if (mode == Mode.NAVIGATE) {
                    settleNow()
                    if (!tapCandidate) startFling(event)
                }
            }

            else -> Unit
        }
        mode = Mode.NONE
        tapCandidate = false
        showWaitingPublish()
        lasso.touching(false)
        crop?.touching(false)
    }

    private fun onCancel() {
        if (mode == Mode.NAVIGATE) settleNow()
        cancelDrawing()
        mode = Mode.NONE
        tapCandidate = false
        showWaitingPublish()
        lasso.touching(false)
        crop?.touching(false)
    }

    // A touch that stayed put: the add page target adds a page, a page selects the image under it or clears.
    private fun tapped() {
        if (readOnly) return
        if (viewport.hitsAddTarget(tapX, tapY)) {
            addPage()
        } else if (mode == Mode.NAVIGATE && crop == null && toolState.mode == ToolMode.TEXT) {
            val page = viewport.pageAt(tapX, tapY)
            if (page >= 0) textEditor.tap(tapX, tapY, page, toolState.text)
        } else if (mode == Mode.NAVIGATE && crop == null) {
            lasso.tapAt(tapX, tapY)
        }
    }

    private fun onGeometryPublished() {
        if (mode == Mode.INK) publishWaiting = true else contentChanged()
    }

    private fun showWaitingPublish() {
        if (!publishWaiting) return
        publishWaiting = false
        contentChanged()
    }

    /** Fills [guidePoints] with the first two fingers, leaving out the pen and index [skip], and returns how many. */
    private fun touchPoints(event: MotionEvent, skip: Int): Int {
        var n = 0
        for (i in 0 until event.pointerCount) {
            if (i == skip || isStylus(event.getToolType(i)) || n == 2) continue
            guidePoints[n * 2] = event.getX(i)
            guidePoints[n * 2 + 1] = event.getY(i)
            n++
        }
        return n
    }

    /** Fills [gesture] with the centroid and mean spread of the pointers, leaving out [skip]. */
    private fun measure(event: MotionEvent, skip: Int) {
        var cx = 0f
        var cy = 0f
        var n = 0
        for (i in 0 until event.pointerCount) {
            if (i == skip) continue
            cx += event.getX(i)
            cy += event.getY(i)
            n++
        }
        if (n == 0) return
        cx /= n
        cy /= n
        var span = 0f
        for (i in 0 until event.pointerCount) {
            if (i == skip) continue
            span += hypot(event.getX(i) - cx, event.getY(i) - cy)
        }
        gesture[0] = cx
        gesture[1] = cy
        gesture[2] = span / n
    }

    private fun reportPageInView() {
        val page = viewport.centrePage()
        if (page == reportedPage) return
        reportedPage = page
        onPageInView?.invoke(page)
    }

    // A finger flick keeps the view gliding only when zoomed far out, where the person is browsing. Closer in the
    // view stops where the fingers let go, so writing and editing stay exact.
    private fun momentumOn(): Boolean = viewport.zoom <= FLING_ZOOM

    private fun trackVelocity(event: MotionEvent, dx: Float, dy: Float) {
        val dt = (event.eventTime - lastMoveTime).coerceAtLeast(1L) / 1000f
        lastMoveTime = event.eventTime
        flingVx = flingVx * 0.4f + dx / dt * 0.6f
        flingVy = flingVy * 0.4f + dy / dt * 0.6f
    }

    private fun resetVelocity(event: MotionEvent) {
        flingVx = 0f
        flingVy = 0f
        lastMoveTime = event.eventTime
    }

    private fun startFling(event: MotionEvent) {
        if (event.pointerCount != 1 || event.eventTime - lastMoveTime > FLING_STALE_MS) return
        if (!momentumOn()) return
        val speed = hypot(flingVx, flingVy)
        if (speed < ViewConfiguration.get(context).scaledMinimumFlingVelocity) return
        val cap = ViewConfiguration.get(context).scaledMaximumFlingVelocity.toFloat()
        val k = if (speed > cap) cap / speed else 1f
        flingVx *= k
        flingVy *= k
        flingTime = System.nanoTime()
        flinging = true
        postOnAnimation(flingStep)
    }

    private fun stopFling() {
        if (!flinging) return
        flinging = false
        removeCallbacks(flingStep)
        settleNow()
    }

    private val flingStep = object : Runnable {
        override fun run() {
            if (!flinging) return
            val now = System.nanoTime()
            val dt = ((now - flingTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
            flingTime = now
            val before = viewport
            val after = before.panned(flingVx * dt, flingVy * dt)
            val decay = exp(-FLING_FRICTION * dt)
            flingVx *= decay
            flingVy *= decay
            // An axis that hit the edge of the document stops there.
            if (after.offsetX == before.offsetX) flingVx = 0f
            if (after.offsetY == before.offsetY) flingVy = 0f
            viewport = after
            reportPageInView()
            redraw()
            requestDry(settled = false)
            if (hypot(flingVx, flingVy) < FLING_STOP_SPEED) {
                flinging = false
                settleNow()
            } else {
                postOnAnimation(this)
            }
        }
    }

    private fun navigate(event: MotionEvent) {
        measure(event, -1)
        val cx = gesture[0]
        val cy = gesture[1]
        val span = gesture[2]
        trackVelocity(event, cx - lastCx, cy - lastCy)
        var vp = viewport.panned(cx - lastCx, cy - lastCy)
        if (event.pointerCount >= 2 && lastSpan > MIN_SPAN && span > MIN_SPAN) {
            vp = vp.zoomedAt(vp.zoom * span / lastSpan, cx, cy)
        }
        viewport = vp
        reportPageInView()
        lastCx = cx
        lastCy = cy
        lastSpan = span
        redraw()
        if (!movingDryQueued) {
            movingDryQueued = true
            postOnAnimation(movingDry)
        }
        settleSoon()
    }

    /** Packs the pointer's samples, including batched history, as `[x, y, pressure, t]`. */
    private fun pack(event: MotionEvent, pointer: Int): FloatArray {
        val n = event.historySize + 1
        val out = FloatArray(n * SAMPLE_STRIDE)
        for (h in 0 until event.historySize) {
            val o = h * SAMPLE_STRIDE
            out[o] = event.getHistoricalX(pointer, h)
            out[o + 1] = event.getHistoricalY(pointer, h)
            out[o + 2] = event.getHistoricalPressure(pointer, h)
            out[o + 3] = (event.getHistoricalEventTime(h) - downTime).toFloat()
        }
        val o = event.historySize * SAMPLE_STRIDE
        out[o] = event.getX(pointer)
        out[o + 1] = event.getY(pointer)
        out[o + 2] = event.getPressure(pointer)
        out[o + 3] = (event.eventTime - downTime).toFloat()
        return out
    }

    private fun ink(event: MotionEvent, pointer: Int) {
        val stroke = wet ?: return
        stroke.push(pack(event, pointer))
        predictor.record(event)
        val predicted = predictor.predict()?.let { p ->
            try {
                pack(p, 0)
            } finally {
                p.recycle()
            }
        }
        // Wet ink never leaves the page it started on.
        val clip = viewport.pageRect(wetPage, RectF())
        renderWet(guides.annotate(stroke.frame(predicted, clip), event.getX(pointer), event.getY(pointer)))
        scheduleHoldCheck(stroke, (event.eventTime - downTime).toFloat())
    }

    // Rescheduled on every move so it fires HOLD_MS after the pen stopped,
    // even when a pen held still sends no more events.
    private fun scheduleHoldCheck(stroke: WetStroke, nowMs: Float) {
        removeCallbacks(holdCheck)
        if (!shapeDetection.snapsOnHold) return
        val still = stroke.stillMs(nowMs)
        if (still < HOLD_MS) holdTried = false
        if (holdTried) return
        postDelayed(holdCheck, (HOLD_MS - still).toLong().coerceAtLeast(0L))
    }

    /** Replaces the wet stroke with the shape it looks like once the pen has been held still long enough. */
    private fun checkHold() {
        val stroke = wet ?: return
        if (mode != Mode.INK || snapped != null || holdTried) return
        val still = stroke.stillMs((SystemClock.uptimeMillis() - downTime).toFloat())
        if (still < HOLD_MS) {
            postDelayed(holdCheck, (HOLD_MS - still).toLong() + 1)
            return
        }
        holdTried = true
        val vp = viewport
        val shaft = lastLine?.takeIf { lastLinePage == wetPage }?.let { l ->
            floatArrayOf(
                vp.viewX(wetPage, l[0]),
                vp.viewY(wetPage, l[1]),
                vp.viewX(wetPage, l[2]),
                vp.viewY(wetPage, l[3]),
            )
        }
        val shape = stroke.snap(vp.pageRect(wetPage, RectF()), shaft) ?: return
        snapped = shape.points
        snappedLine = shape.line
        // The freehand stroke leaves the front buffer; the shape takes over in the page layer.
        resetPending()
        renderer?.cancel()
        showLiveShape(shape.frame)
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Keeps working on a snapped shape while the pen stays down. */
    private fun reshape(x: Float, y: Float) {
        val stroke = wet ?: return
        val shape = stroke.reshape(x, y, viewport.pageRect(wetPage, RectF())) ?: return
        snapped = shape.points
        snappedLine = shape.line
        showLiveShape(shape.frame)
    }

    // Goes through the coalesced page redraw, so a long drag asks for at most one page frame at
    // a time and never starves the page layer's buffers.
    private fun showLiveShape(frame: WetFrame?) {
        liveShape = frame
        redraw()
    }

    private fun finishInk(event: MotionEvent) {
        val stroke = wet ?: return
        val idx = event.findPointerIndex(activePointerId)
        val dragging = shapeDrag != null
        if (dragging && idx >= 0) dragShape(event, idx)
        wet = null
        shapeDrag = null
        guides.endStroke()
        removeCallbacks(holdCheck)
        val shape = snapped
        val line = snappedLine
        snapped = null
        snappedLine = null
        lastLine = null
        if (dragging && shape == null) {
            // A tap, or a drag that never drew anything, leaves nothing behind.
            stroke.cancel()
            resetPending()
            renderer?.cancel()
            if (liveShape != null) showLiveShape(null)
            return
        }
        if (shape == null && idx >= 0) stroke.push(pack(event, idx))
        val finished = stroke.finish()
        val points = shape ?: finished
        val vp = viewport
        val page = wetPage
        val local = toLocal(page, points)
        val tool = activeTool
        val code = if (tool.mode == ToolMode.HIGHLIGHTER) Tool.HIGHLIGHTER else Tool.PEN
        val added = local.isNotEmpty() && NativeNotebook.nativeAddStroke(
            notebook.nativeHandle,
            page,
            code,
            tool.color,
            tool.widthPt,
            tool.line.code,
            startedAtMs,
            local,
        )
        if (!added) {
            resetPending()
            renderer?.cancel()
            if (shape != null) showLiveShape(null)
            return
        }
        if (line != null) {
            lastLine =
                floatArrayOf(
                    vp.localX(page, line[0]),
                    vp.localY(page, line[1]),
                    vp.localX(page, line[2]),
                    vp.localY(page, line[3]),
                )
            lastLinePage = page
        }
        val pageId = info.pageIds.getOrNull(page)
        val before = pageId?.let { cache.scenes[it] }
        refreshPage(page)
        generation++
        // Keep the new stroke on screen until a background render contains it.
        val lifted = pageId?.let { cache.scenes[it]?.addedSince(before) }
        if (lifted != null) pending = pending + PendingStroke(page, lifted.first, generation, lifted.second)
        requestDry()
        resetPending()
        // The committed frame draws the new stroke from the pending list in place of the shape.
        liveShape = null
        multiInFlight = true
        multiRequestedAt = SystemClock.uptimeMillis()
        renderer?.commit()
        notifyChanged()
        if (shape == null && shapeDetection.suggests && !guides.rulerOn && !guides.protractorOn) {
            offerShape(page, finished, tool)
        }
    }

    /** The engine wants page-local document units; pressure and time carry over. */
    private fun toLocal(page: Int, points: FloatArray): FloatArray {
        val vp = viewport
        val local = FloatArray(points.size)
        var i = 0
        while (i + SAMPLE_STRIDE <= points.size) {
            local[i] = vp.localX(page, points[i])
            local[i + 1] = vp.localY(page, points[i + 1])
            local[i + 2] = points[i + 2]
            local[i + 3] = points[i + 3]
            i += SAMPLE_STRIDE
        }
        return local
    }

    private fun offerShape(page: Int, drawn: FloatArray, tool: ToolState) {
        val suggestion = shapeOffer.consider(page, drawn, tool, startedAtMs, { toLocal(page, it) })
        onShapeSuggest?.invoke(suggestion)
    }

    private fun clearShapeOffer() {
        if (shapeOffer.clear()) onShapeSuggest?.invoke(null)
    }

    /** Dismisses the offered shape and keeps the stroke as it was drawn. */
    fun dismissShapeSuggestion() = clearShapeOffer()

    /**
     * Swaps the stroke just drawn for the shape that was offered, as one step of history: the freehand stroke is
     * undone and the shape is added in its place. Nothing happens if the offer has gone.
     */
    fun acceptShapeSuggestion() {
        val offered = shapeOffer.offered ?: return
        clearShapeOffer()
        if (lasso.isLocked) return
        val handle = notebook.nativeHandle
        val undone = NativeNotebook.nativeUndo(handle)
        if (undone != offered.page) {
            // Something else was undone, so put it back and leave the stroke alone.
            if (undone >= 0) NativeNotebook.nativeRedo(handle)
            return
        }
        val tool = offered.tool
        val code = if (tool.mode == ToolMode.HIGHLIGHTER) Tool.HIGHLIGHTER else Tool.PEN
        val added = NativeNotebook.nativeAddStroke(
            handle,
            offered.page,
            code,
            tool.color,
            tool.widthPt,
            tool.line.code,
            offered.startedAtMs,
            offered.local,
        )
        // A failed add leaves the redo step in place, so the stroke comes back as it was.
        if (!added) NativeNotebook.nativeRedo(handle)
        afterHistory(offered.page)
    }

    /** Fills the enclosed area under a tap (view pixels in, page-local document units out). */
    private fun fillAt(x: Float, y: Float, page: Int) {
        val vp = viewport
        val ok = NativeNotebook.nativeFill(
            notebook.nativeHandle,
            page,
            vp.localX(page, x),
            vp.localY(page, y),
            activeTool.color,
            vp.scale,
            vp.localX(page, 0f),
            vp.localY(page, 0f),
            vp.localX(page, width.toFloat()),
            vp.localY(page, height.toFloat()),
        )
        if (!ok) {
            onFillFailed?.invoke()
            return
        }
        refreshPage(page)
        contentChanged()
        notifyChanged()
    }

    private fun erase(event: MotionEvent, pointer: Int) {
        val t0 = EraseLog.nowNs()
        val vp = viewport
        val page = wetPage
        val radius = activeTool.eraserPt / 2f
        val removed = erasePath(event, pointer, vp, page, radius)
        if (removed > 0) {
            erasedAny = true
            erasedPage = page
            if (!erasedShowQueued) {
                erasedShowQueued = true
                postOnAnimation(showErased)
            }
        }
        moveMark(event.getX(pointer), event.getY(pointer), radius * vp.scale)
        EraseLog.slow("erase input", EraseLog.elapsedMs(t0), EraseLog.INPUT_MS)
    }

    /**
     * Erases along everything the pointer did since the last event, as one polyline from the previous point
     * (view pixels in, document units out); returns strokes removed.
     */
    private fun erasePath(event: MotionEvent, pointer: Int, vp: Viewport, page: Int, radius: Float): Int {
        val samples = event.historySize + 1
        val points = FloatArray((samples + 1) * 2)
        var n = 0
        if (hasPrevErase) {
            points[n++] = prevEraseX
            points[n++] = prevEraseY
        }
        for (h in 0 until event.historySize) {
            points[n++] = vp.localX(page, event.getHistoricalX(pointer, h))
            points[n++] = vp.localY(page, event.getHistoricalY(pointer, h))
        }
        points[n++] = vp.localX(page, event.getX(pointer))
        points[n++] = vp.localY(page, event.getY(pointer))
        if (!hasPrevErase) {
            // The first sample is a dab: a segment of no length.
            System.arraycopy(points, 0, points, 2, n)
            n += 2
        }
        prevEraseX = points[n - 2]
        prevEraseY = points[n - 1]
        hasPrevErase = true
        return NativeNotebook.nativeErasePath(notebook.nativeHandle, points.copyOf(n), radius)
    }

    /** Moves the eraser's circle; the page shows it with its next frame. */
    private fun moveMark(x: Float, y: Float, radius: Float) {
        eraserMark = floatArrayOf(x, y, radius)
        redraw()
    }

    private fun finishErase(event: MotionEvent) {
        val idx = event.findPointerIndex(activePointerId)
        if (idx >= 0) erase(event, idx)
        endErase()
    }

    private fun endErase() {
        if (erasedShowQueued) {
            removeCallbacks(showErased)
            erasedShowQueued = false
        }
        NativeNotebook.nativeEndErase(notebook.nativeHandle)
        if (erasedAny) {
            val left = StrokeLayout.count(NativeNotebook.nativeLayerStrokeIds(notebook.nativeHandle, erasedPage))
            Log.i(EraseLog.TAG, "erase ended, page $erasedPage has $left visible strokes")
            requestGeometry(erasedPage, true)
        }
        eraserMark = null
        redraw()
        if (erasedAny) notifyChanged()
        erasedAny = false
    }

    /** Abandons whatever is being drawn or erased. Edits already made by the eraser stay. */
    private fun cancelDrawing() {
        guides.cancel()
        guides.endStroke()
        wet?.cancel()
        wet = null
        shapeDrag = null
        snapped = null
        snappedLine = null
        removeCallbacks(holdCheck)
        if (liveShape != null) showLiveShape(null)
        if (mode == Mode.ERASE) {
            endErase()
        } else if (mode == Mode.LASSO || mode == Mode.LASSO_DRAG) {
            lasso.cancel()
        } else if (mode == Mode.CROP_DRAG) {
            crop?.endDrag()
        } else {
            resetPending()
            renderer?.cancel()
        }
    }

    private companion object {
        const val SAMPLE_STRIDE = 4

        /** How long the pen must rest at the end of a stroke before it snaps to a shape. */
        const val HOLD_MS = 350f

        /** A stroke smaller than this across is writing, not a shape, and is never offered as one. */
        const val SUGGEST_MIN_DP = 72f

        /** A queued wet render older than this is assumed lost. */
        const val STUCK_MS = 100L

        /** Lost front buffer renders in a row before the renderer is started again. */
        const val MAX_LOST_RENDERS = 5

        /** Fingers this soon after the pen are treated as a resting palm. */
        const val PALM_REJECT_MS = 400L
        const val TAP_MS = 400L
        const val MIN_SPAN = 8f
        const val FINGER_TAP_SLOP = 1.5f

        /** A fingertip touches less than this wide; a palm or the side of a hand more. */
        const val FINGER_MAX_MM = 16f

        /** Fingers of one hand tapping together land within this of each other. */
        const val FINGER_SPREAD_MM = 60f
        const val MM_PER_INCH = 25.4f

        /** How long pan and zoom must rest before the dry layer is re-rendered. */
        const val SETTLE_MS = 120L

        /** At this zoom or further out a flick glides on (a page about half the view wide); closer in it stops. */
        const val FLING_ZOOM = 0.55f

        /** A finger that rested this long before lifting does not flick. */
        const val FLING_STALE_MS = 80L

        /** Per second, how fast the glide loses speed. */
        const val FLING_FRICTION = 3.2f
        const val FLING_STOP_SPEED = 40f
    }
}
