package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.RectF
import android.view.MotionEvent
import com.onemnemo.grapho.engine.NativeNotebook
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** What the lasso needs from the canvas it lives in. */
internal interface LassoHost {
    val currentViewport: Viewport

    /** Counts edits, like the canvas' generation. */
    val editCount: Int
    val density: Float

    fun pageId(page: Int): String?

    /** The images the page shows, in drawing order. */
    fun images(pageId: String): List<SceneImage>
    fun redraw()

    /** The held strokes changed without the document changing: later renders must leave out the new set. */
    fun selectionShown()

    /** The document changed on [page]: read it again, show it in later renders and save it. */
    fun documentEdited(page: Int)
    fun menuChanged(menu: SelectionMenu?)
    fun pasteOffered(x: Float, y: Float)
}

/**
 * The lasso tool: tracing, the selection and dragging it, and the edits made to it. The selection holds strokes and
 * images, and a tap on an image makes one whatever the tool. Touch input arrives from the canvas on the UI thread;
 * [selection] and [trace] are what the render thread draws.
 */
internal class LassoController(private val notebook: Notebook, private val host: LassoHost) {
    @Volatile var selection: LassoSelection? = null
        private set

    @Volatile var trace: LassoTrace? = null
        private set

    // The image a picture job is working on. It cannot be selected, and so cannot be edited, until the job ends.
    @Volatile private var locked: StrokeId? = null
    private var touchDown = false
    private var menuShown = false
    private var menuHeld = false
    private val box = RectF()

    /** The trace is a rectangle dragged from corner to corner instead of a freehand loop. */
    var rectangle = false
    private var startLx = 0f
    private var startLy = 0f
    private val handle = FloatArray(2)

    // The drag in progress: which part was grabbed, where it started (view pixels), and for a corner the
    // corner and its opposite in page-local units.
    private var grab = NONE
    private var startX = 0f
    private var startY = 0f
    private var cornerX = 0f
    private var cornerY = 0f
    private var anchorX = 0f
    private var anchorY = 0f
    private var grabDx = 0f
    private var grabDy = 0f

    // For a turn: the middle of the box in view pixels and the angle the finger started at.
    private var pivotX = 0f
    private var pivotY = 0f
    private var startAngle = 0f

    /** Whether a touch at ([x], [y]) would grab the selection, by a corner or inside the box. */
    fun hits(x: Float, y: Float): Boolean {
        val sel = selection ?: return false
        return hitTest(sel, x, y) != NONE
    }

    fun hidden(): HiddenStrokes? = selection?.hidden

    /** Whether a picture job holds an image, which keeps history from changing under it. */
    val isLocked: Boolean get() = locked != null

    /** Keeps [id] from being selected while a job works on it, or lets go with null. Locking drops the selection. */
    fun lockImage(id: StrokeId?) {
        locked = id
        if (id != null) clear()
    }

    /** Where the image [id] on [page] sits in view pixels, written to [out]. False when the page has no such image. */
    fun imageViewRect(page: Int, id: StrokeId, out: RectF): Boolean {
        val pageId = host.pageId(page) ?: return false
        val image = host.images(pageId).firstOrNull { it.id == id } ?: return false
        val vp = host.currentViewport
        out.set(
            vp.viewX(page, image.bounds.left),
            vp.viewY(page, image.bounds.top),
            vp.viewX(page, image.bounds.right),
            vp.viewY(page, image.bounds.bottom),
        )
        return true
    }

    /** The part of its source the image [id] on [page] shows, as fractions, written to [out]. False if there is none. */
    fun imageCrop(page: Int, id: StrokeId, out: RectF): Boolean {
        val pageId = host.pageId(page) ?: return false
        val image = host.images(pageId).firstOrNull { it.id == id } ?: return false
        out.set(image.crop)
        return true
    }

    fun beginTrace(x: Float, y: Float, page: Int) {
        val vp = host.currentViewport
        val t = LassoTrace(page)
        startLx = vp.localX(page, x)
        startLy = vp.localY(page, y)
        t.add(startLx, startLy)
        trace = t
        host.redraw()
    }

    fun extendTrace(event: MotionEvent, pointer: Int) {
        val t = trace ?: return
        val vp = host.currentViewport
        if (rectangle) {
            val x = vp.localX(t.page, event.getX(pointer))
            val y = vp.localY(t.page, event.getY(pointer))
            t.setRectangle(startLx, startLy, x, y)
            host.redraw()
            return
        }
        for (h in 0 until event.historySize) {
            addPoint(t, vp, event.getHistoricalX(pointer, h), event.getHistoricalY(pointer, h))
        }
        addPoint(t, vp, event.getX(pointer), event.getY(pointer))
        host.redraw()
    }

    private fun addPoint(t: LassoTrace, vp: Viewport, x: Float, y: Float) {
        val lx = vp.localX(t.page, x)
        val ly = vp.localY(t.page, y)
        val p = t.points
        val n = t.size
        // Points closer than a pixel add nothing to the outline.
        if (n >= 2 && hypot(lx - p[n - 2], ly - p[n - 1]) * vp.scale < MIN_STEP_PX) return
        t.add(lx, ly)
    }

    /**
     * The pen lifted. A [tap] is a touch that never moved: it selects the image under it, or clears the selection,
     * or offers to paste.
     */
    fun endTrace(tap: Boolean, x: Float, y: Float) {
        val t = trace ?: return
        trace = null
        host.redraw()
        if (tap || t.size < MIN_POLYGON) {
            if (selectImageAt(t.page, x, y)) return
            if (selection != null) {
                clear()
            } else if (NativeNotebook.nativeHasClipboard(notebook.nativeHandle)) {
                host.pasteOffered(x, y)
            }
            return
        }
        val ids = NativeNotebook.nativeSelect(notebook.nativeHandle, t.page, t.points.copyOf(t.size))
        select(t.page, ids)
    }

    /**
     * A finger tap at ([x], [y]) in view pixels: selects the image under it, or clears the selection, or, with
     * nothing selected and something copied, offers to paste there.
     */
    fun tapAt(x: Float, y: Float) {
        val page = host.currentViewport.pageAt(x, y)
        if (page >= 0 && selectImageAt(page, x, y)) return
        val hadSelection = selection != null
        clear()
        if (page >= 0 && !hadSelection && NativeNotebook.nativeHasClipboard(notebook.nativeHandle)) {
            host.pasteOffered(x, y)
        }
    }

    private fun selectImageAt(page: Int, x: Float, y: Float): Boolean {
        val vp = host.currentViewport
        val id = NativeNotebook.nativeImageAt(notebook.nativeHandle, page, vp.localX(page, x), vp.localY(page, y))
        if (id.size < 2) return false
        select(page, id)
        return selection != null
    }

    fun beginDrag(x: Float, y: Float): Boolean {
        val sel = selection ?: return false
        val hit = hitTest(sel, x, y)
        if (hit == NONE) return false
        grab = hit
        startX = x
        startY = y
        if (hit == ROTATE) {
            sel.viewBox(host.currentViewport, LASSO_PAD_DP * host.density, box)
            pivotX = box.centerX()
            pivotY = box.centerY()
            startAngle = angleTo(x, y)
        } else if (hit != BOX) {
            val vp = host.currentViewport
            val b = sel.bounds
            val left = hit == 0 || hit == 3
            val top = hit < 2
            cornerX = if (left) b.left else b.right
            cornerY = if (top) b.top else b.bottom
            anchorX = if (left) b.right else b.left
            anchorY = if (top) b.bottom else b.top
            grabDx = vp.localX(sel.page, x) - cornerX
            grabDy = vp.localY(sel.page, y) - cornerY
        }
        selection = sel.with(Matrix(), true)
        host.redraw()
        return true
    }

    fun drag(x: Float, y: Float) {
        val sel = selection ?: return
        if (grab == NONE) return
        val vp = host.currentViewport
        val m = Matrix()
        if (grab == BOX) {
            m.setTranslate((x - startX) / vp.scale, (y - startY) / vp.scale)
        } else if (grab == ROTATE) {
            val turn = Rotation.snap(Rotation.between(startAngle, angleTo(x, y)))
            m.setRotate(turn, sel.bounds.centerX(), sel.bounds.centerY())
        } else {
            // Proportional: the corner follows the finger along the box's diagonal.
            val vx = cornerX - anchorX
            val vy = cornerY - anchorY
            val tx = vp.localX(sel.page, x) - grabDx - anchorX
            val ty = vp.localY(sel.page, y) - grabDy - anchorY
            val diagonal = vx * vx + vy * vy
            if (diagonal < 1e-6f) return
            val least = min(1f, MIN_SIZE_PT / min(abs(vx), abs(vy)).coerceAtLeast(0.01f))
            val k = ((tx * vx + ty * vy) / diagonal).coerceIn(least, MAX_SCALE)
            m.setScale(k, k, anchorX, anchorY)
        }
        selection = sel.with(m, true)
        host.redraw()
    }

    private fun angleTo(x: Float, y: Float): Float = Math.toDegrees(atan2(y - pivotY, x - pivotX).toDouble()).toFloat()

    /**
     * The finger lifted: a real move, scale or turn becomes one edit, and the items get new ids. A selection let go
     * over another page moves onto that page, so it is not left drawn past the edge of its own.
     */
    fun endDrag() {
        val sel = selection ?: return
        val grabbed = grab
        grab = NONE
        if (!sel.moved) {
            selection = sel.with(Matrix(), false)
            host.redraw()
            return
        }
        val v = FloatArray(9)
        sel.matrix.getValues(v)
        val matrix = floatArrayOf(v[0], v[1], v[2], v[3], v[4], v[5])
        val target = if (grabbed == BOX) dropPage(sel) else sel.page
        if (target != sel.page) {
            val vp = host.currentViewport
            // From the old page's units to the new one's: pages differ only by where they sit.
            matrix[2] += vp.localX(target, vp.viewX(sel.page, 0f))
            matrix[5] += vp.localY(target, vp.viewY(sel.page, 0f))
            val ids = NativeNotebook.nativeMoveToPage(notebook.nativeHandle, sel.page, sel.flat, target, matrix)
            if (ids.isNotEmpty()) {
                host.documentEdited(sel.page)
                adopt(target, ids)
                return
            }
            matrix[2] = v[2]
            matrix[5] = v[5]
        }
        val ids = NativeNotebook.nativeTransform(notebook.nativeHandle, sel.page, sel.flat, matrix)
        adopt(sel.page, ids)
    }

    /** The page under the middle of the moved selection, or its own page when the middle is on the desk. */
    private fun dropPage(sel: LassoSelection): Int {
        sel.viewBox(host.currentViewport, 0f, box)
        val page = host.currentViewport.pageAt(box.centerX(), box.centerY())
        return if (page < 0) sel.page else page
    }

    /** Abandons the trace or drag in progress; a half-made move is undone on screen. */
    fun cancel() {
        trace = null
        grab = NONE
        val sel = selection
        if (sel != null && (sel.moved || sel.dragging)) selection = sel.with(Matrix(), false)
        host.redraw()
    }

    fun clear() {
        trace = null
        grab = NONE
        if (selection == null) return
        selection = null
        host.selectionShown()
        syncMenu()
    }

    /** Holds strokes and images that are already in the renders, which then leave them out. */
    private fun select(page: Int, ids: LongArray) {
        val id = host.pageId(page)
        val bounds = if (id == null || ids.isEmpty()) null else inkBounds(page, ids)
        val held = locked
        if (id == null || bounds == null || (held != null && held in LassoSelection.idsOf(ids))) {
            clear()
            return
        }
        selection = LassoSelection(page, id, ids, bounds, host.editCount, fresh = false)
        host.selectionShown()
        syncMenu()
    }

    private fun inkBounds(page: Int, ids: LongArray): RectF? {
        val b = NativeNotebook.nativeSelectionBounds(notebook.nativeHandle, page, ids)
        return if (b.size < 4) null else RectF(b[0], b[1], b[2], b[3])
    }

    /** Holds what an edit made outside the lasso just created, e.g. an inserted or cropped image. */
    fun adoptEdit(page: Int, ids: LongArray) = adopt(page, ids)

    /** The one image that is selected, or null if the selection is anything else. */
    fun singleImage(): SceneImage? {
        val sel = selection ?: return null
        if (sel.ids.size != 1) return null
        return host.images(sel.pageId).firstOrNull { it.id in sel.ids }
    }

    /** Holds the strokes and images an edit just made, which have new ids that no render has drawn yet. */
    private fun adopt(page: Int, ids: LongArray) {
        val id = host.pageId(page)
        val bounds = if (id == null || ids.isEmpty()) null else inkBounds(page, ids)
        selection = if (id == null || bounds == null) {
            null
        } else {
            LassoSelection(page, id, ids, bounds, host.editCount + 1, fresh = true)
        }
        host.documentEdited(page)
        syncMenu()
    }

    fun recolor(argb: Int) {
        val sel = selection ?: return
        adopt(sel.page, NativeNotebook.nativeRecolor(notebook.nativeHandle, sel.page, sel.flat, argb))
    }

    fun duplicate() {
        val sel = selection ?: return
        val copies = NativeNotebook.nativeDuplicate(notebook.nativeHandle, sel.page, sel.flat, COPY_OFFSET, COPY_OFFSET)
        adopt(sel.page, copies)
    }

    fun delete() {
        val sel = selection ?: return
        NativeNotebook.nativeDelete(notebook.nativeHandle, sel.page, sel.flat)
        adopt(sel.page, LongArray(0))
    }

    /** Copies the selection to the clipboard and removes it, as one undo step for the removal. */
    fun cut() {
        val sel = selection ?: return
        NativeNotebook.nativeCopy(notebook.nativeHandle, sel.page, sel.flat)
        delete()
    }

    /** Moves the selection to the front or back of its layer, as one undo step. */
    fun reorder(front: Boolean) {
        val sel = selection ?: return
        if (NativeNotebook.nativeReorder(notebook.nativeHandle, sel.page, sel.flat, front)) adopt(sel.page, sel.flat)
    }

    /** Draws the selected strokes [width] points wide, as one undo step. */
    fun setWidth(width: Float) {
        val sel = selection ?: return
        adopt(sel.page, NativeNotebook.nativeSetWidth(notebook.nativeHandle, sel.page, sel.flat, width))
    }

    /** Mirrors the selection about its own middle. */
    fun flip(horizontal: Boolean) {
        val sel = selection ?: return
        val m = SelectionFlip.matrix(horizontal, sel.bounds.centerX(), sel.bounds.centerY())
        adopt(sel.page, NativeNotebook.nativeTransform(notebook.nativeHandle, sel.page, sel.flat, m))
    }

    /**
     * Moves what [sel] held to the page's active layer: copies it there in place and removes the originals. The
     * selection must be passed in because adding the layer it moves to clears the lasso.
     */
    fun moveToActiveLayer(sel: LassoSelection) {
        val copies = NativeNotebook.nativeDuplicate(notebook.nativeHandle, sel.page, sel.flat, 0f, 0f)
        if (copies.isEmpty()) return
        NativeNotebook.nativeDelete(notebook.nativeHandle, sel.page, sel.flat)
        adopt(sel.page, copies)
    }

    /** Swaps the image [old] for the new one [added], which is selected. */
    fun replaceWith(page: Int, old: LongArray, added: LongArray) {
        NativeNotebook.nativeDelete(notebook.nativeHandle, page, old)
        adopt(page, added)
    }

    fun copy() {
        val sel = selection ?: return
        NativeNotebook.nativeCopy(notebook.nativeHandle, sel.page, sel.flat)
    }

    /** Pastes the clipboard centred on a view point and holds the pasted strokes. */
    fun paste(x: Float, y: Float) {
        val vp = host.currentViewport
        val page = vp.pageAt(x, y)
        if (page < 0) return
        val ids = NativeNotebook.nativePaste(notebook.nativeHandle, page, vp.localX(page, x), vp.localY(page, y))
        if (ids.isNotEmpty()) adopt(page, ids)
    }

    /** The menu is out of the way while any touch is down, and shows again when the touch ends. */
    fun touching(on: Boolean) {
        touchDown = on
        syncMenu()
    }

    /** Keeps the menu hidden while something else, such as the crop editor, has the selection. */
    fun holdMenu(on: Boolean) {
        menuHeld = on
        syncMenu()
    }

    /** Tells the canvas where the menu belongs now, or that it should be hidden. */
    fun syncMenu() {
        val sel = selection
        if (sel == null || touchDown || sel.dragging || menuHeld) {
            if (menuShown) {
                menuShown = false
                host.menuChanged(null)
            }
            return
        }
        menuShown = true
        val images = host.images(sel.pageId).mapTo(HashSet()) { it.id }
        val imageCount = sel.ids.count { it in images }
        val bounds = sel.viewBox(host.currentViewport, LASSO_PAD_DP * host.density, RectF())
        val width = NativeNotebook.nativeCommonWidth(notebook.nativeHandle, sel.page, sel.flat)
        host.menuChanged(
            SelectionMenu(bounds, imageCount < sel.ids.size, sel.ids.size == 1 && imageCount == 1, width),
        )
    }

    private fun hitTest(sel: LassoSelection, x: Float, y: Float): Int {
        val d = host.density
        sel.viewBox(host.currentViewport, LASSO_PAD_DP * d, box)
        Rotation.handle(box, LASSO_ROTATE_DP * d, handle)
        if (hypot(x - handle[0], y - handle[1]) <= HANDLE_TOUCH_DP * d) return ROTATE
        var best = NONE
        var bestDistance = HANDLE_TOUCH_DP * d
        for (corner in 0 until 4) {
            val cx = if (corner == 0 || corner == 3) box.left else box.right
            val cy = if (corner < 2) box.top else box.bottom
            val distance = hypot(x - cx, y - cy)
            if (distance <= bestDistance) {
                best = corner
                bestDistance = distance
            }
        }
        if (best != NONE) return best
        box.inset(-BOX_SLOP_DP * d, -BOX_SLOP_DP * d)
        return if (box.contains(x, y)) BOX else NONE
    }

    private companion object {
        const val NONE = -1
        const val BOX = 4
        const val ROTATE = 5

        /** Floats in a trace below this make no polygon. */
        const val MIN_POLYGON = 6
        const val MIN_STEP_PX = 1f
        const val HANDLE_TOUCH_DP = 24f
        const val BOX_SLOP_DP = 4f

        /** The smallest the selection can be scaled to, in points, and the most it can grow. */
        const val MIN_SIZE_PT = 12f
        const val MAX_SCALE = 40f
        const val COPY_OFFSET = 12f
    }
}
