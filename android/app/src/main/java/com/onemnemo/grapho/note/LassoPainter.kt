package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Space between the ink and the selection box, in dp. */
internal const val LASSO_PAD_DP = 4f

/** Corner handle radius, in dp. */
internal const val LASSO_HANDLE_DP = 7f

/** How far the rotate handle sits above the selection box, in dp. */
internal const val LASSO_ROTATE_DP = 28f

/**
 * Draws everything the lasso adds to the page layer: the selected strokes and images lifted off the dry render, the
 * lasso being traced, and the selection box with its corner handles and the handle that turns it. Render thread only.
 */
internal class LassoPainter(private val density: Float, private val images: ImageBitmaps) {
    private val dashed = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        pathEffect = DashPathEffect(floatArrayOf(7f * density, 5f * density), 0f)
    }
    private val handleFill = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = WHITE
    }
    private val handleRing = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val glyph = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val imagePainter = ImagePainter()
    private val textPainter = TextPainter()
    private val path = Path()
    private val box = RectF()
    private val pageRect = RectF()
    private val arc = RectF()
    private val handle = FloatArray(2)
    private var paper = 0

    fun draw(
        canvas: Canvas,
        vp: Viewport,
        info: NotebookInfo,
        scenes: Map<String, PageScene>,
        snapshot: RenderedDry?,
        selection: LassoSelection?,
        trace: LassoTrace?,
    ) {
        val dark = isDarkPaper(info.background.color)
        paper = info.background.color
        dashed.color = SelectionColours.line(dark)
        handleRing.color = SelectionColours.accent(dark)
        glyph.color = handleRing.color
        drawStrokes(canvas, vp, scenes, snapshot, selection)
        if (trace != null) drawTrace(canvas, vp, trace)
        if (selection != null) drawBox(canvas, vp, selection)
    }

    /**
     * Strokes and images the current render leaves out are drawn here, so there is never a frame without them: the
     * selected ones (moved when a drag is on), and ones that were just let go until a render shows them again.
     */
    private fun drawStrokes(
        canvas: Canvas,
        vp: Viewport,
        scenes: Map<String, PageScene>,
        snapshot: RenderedDry?,
        selection: LassoSelection?,
    ) {
        val hidden = snapshot?.hidden
        if (selection != null) {
            val lifted = hidden?.takeIf { it.pageId == selection.pageId }?.ids
            val unseen = selection.fresh && snapshot != null && snapshot.generation < selection.generation
            drawFrom(canvas, vp, selection.page, scenes[selection.pageId], selection.matrix) { id ->
                id in selection.ids && (selection.moved || unseen || lifted?.contains(id) == true)
            }
        }
        if (hidden != null && hidden !== selection?.hidden) {
            val held = if (selection != null && selection.pageId == hidden.pageId) selection.ids else emptySet()
            drawFrom(canvas, vp, hidden.page, scenes[hidden.pageId], null) { it in hidden.ids && it !in held }
        }
    }

    private inline fun drawFrom(
        canvas: Canvas,
        vp: Viewport,
        page: Int,
        scene: PageScene?,
        matrix: Matrix?,
        wanted: (StrokeId) -> Boolean,
    ) {
        if (scene == null) return
        canvas.save()
        val s = vp.scale
        if (vp.infinite) {
            canvas.translate(vp.offsetX, vp.offsetY)
        } else {
            vp.pageRect(page, pageRect)
            // A selection being moved shows over the neighbouring pages instead of hiding under them.
            if (matrix == null) canvas.clipRect(pageRect)
            canvas.translate(pageRect.left, pageRect.top)
        }
        canvas.scale(s, s)
        if (matrix != null) canvas.concat(matrix)
        for (layer in scene.layers) {
            for (image in layer.images) {
                if (wanted(image.id)) imagePainter.draw(canvas, image, images, paper, layer.alpha)
            }
            for (box in layer.texts) {
                if (wanted(box.id)) textPainter.draw(canvas, box, paper, layer.alpha)
            }
        }
        for (stroke in scene.strokes) {
            if (wanted(stroke.id)) canvas.drawPath(stroke.path, stroke.paint)
        }
        canvas.restore()
    }

    private fun drawTrace(canvas: Canvas, vp: Viewport, trace: LassoTrace) {
        val p = trace.points
        val n = minOf(trace.size, p.size)
        if (n < 4) return
        path.reset()
        path.moveTo(vp.viewX(trace.page, p[0]), vp.viewY(trace.page, p[1]))
        var i = 2
        while (i + 1 < n) {
            path.lineTo(vp.viewX(trace.page, p[i]), vp.viewY(trace.page, p[i + 1]))
            i += 2
        }
        if (trace.closed) path.close()
        canvas.drawPath(path, dashed)
    }

    private fun drawBox(canvas: Canvas, vp: Viewport, selection: LassoSelection) {
        selection.viewBox(vp, LASSO_PAD_DP * density, box)
        canvas.save()
        // A turn in progress shows the box turning with its contents.
        if (selection.turned) canvas.rotate(selection.angle, box.centerX(), box.centerY())
        canvas.drawRect(box, dashed)
        val r = LASSO_HANDLE_DP * density
        for (corner in 0 until 4) {
            val x = if (corner == 0 || corner == 3) box.left else box.right
            val y = if (corner < 2) box.top else box.bottom
            canvas.drawCircle(x, y, r, handleFill)
            canvas.drawCircle(x, y, r, handleRing)
        }
        drawRotateHandle(canvas)
        canvas.restore()
    }

    /** A ring joined to the box, holding an open circular arrow. */
    private fun drawRotateHandle(canvas: Canvas) {
        Rotation.handle(box, LASSO_ROTATE_DP * density, handle)
        val r = LASSO_HANDLE_DP * density * 1.5f
        val edge = if (handle[1] < box.top) box.top else box.bottom
        canvas.drawLine(handle[0], edge, handle[0], handle[1], dashed)
        canvas.drawCircle(handle[0], handle[1], r, handleFill)
        canvas.drawCircle(handle[0], handle[1], r, handleRing)
        val a = r * 0.5f
        arc.set(handle[0] - a, handle[1] - a, handle[0] + a, handle[1] + a)
        canvas.drawArc(arc, ARC_START, ARC_SWEEP, false, glyph)
    }

    private companion object {
        const val WHITE = SelectionColours.WHITE
        const val ARC_START = -50f
        const val ARC_SWEEP = 280f
    }
}
