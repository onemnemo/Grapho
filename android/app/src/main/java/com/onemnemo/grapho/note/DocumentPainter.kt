package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.onemnemo.grapho.engine.PatternKind
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Patterns closer together than this (in pixels) are skipped; they would only be a grey wash. */
private const val MIN_SPACING_PX = 4f

/**
 * Draws the dry layer: desk, pages with their paper pattern, finished strokes
 * and the "Add page" target. Used only on the render thread, so it can keep
 * its paints and scratch rectangles.
 */
class DocumentPainter {
    private val fill = Paint()
    private val hole = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val edge = Paint()
    private val shadow = Paint()
    private val lines = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0f
    }
    private val dots = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val addFill = Paint().apply {
        isAntiAlias = true
    }
    private val addStroke = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val addPlus = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val imagePainter = ImagePainter()
    private val textPainter = TextPainter()
    private val pageRect = RectF()
    private val visible = RectF()
    private val viewRect = RectF()
    private var chromePaper: Int? = null

    private fun applyChrome(paper: Int) {
        if (chromePaper == paper) return
        chromePaper = paper
        edge.color = PaperChrome.edge(paper)
        shadow.color = PaperChrome.shadow(paper)
        lines.color = PaperChrome.pattern(paper)
        dots.color = PaperChrome.pattern(paper)
        addFill.color = PaperChrome.addFill(paper)
        addStroke.color = PaperChrome.addStroke(paper)
        addPlus.color = PaperChrome.addPlus(paper)
    }

    fun draw(
        canvas: Canvas,
        vp: Viewport,
        info: NotebookInfo,
        scenes: Map<String, PageScene>,
        hidden: HiddenStrokes? = null,
        pdf: PdfBackgrounds? = null,
        pdfBehind: Boolean = false,
        images: ImageBitmaps? = null,
    ) {
        val bg = info.background
        val s = vp.scale
        applyChrome(bg.color)
        viewRect.set(0f, 0f, vp.viewWidth, vp.viewHeight)
        if (vp.infinite) {
            canvas.drawColor(bg.color)
            visible.set(
                -vp.offsetX / s,
                -vp.offsetY / s,
                (vp.viewWidth - vp.offsetX) / s,
                (vp.viewHeight - vp.offsetY) / s,
            )
            canvas.save()
            canvas.translate(vp.offsetX, vp.offsetY)
            canvas.scale(s, s)
            drawPage(canvas, bg, true, info.pageIds.firstOrNull(), scenes, visible, s, hidden, images)
            canvas.restore()
            return
        }
        canvas.drawColor(PaperChrome.desk(bg.color))
        for (i in 0 until vp.pageCount) {
            vp.pageRect(i, pageRect)
            if (!RectF.intersects(pageRect, viewRect)) continue
            if (shadow.alpha != 0) {
                canvas.drawRect(
                    pageRect.left - 2f,
                    pageRect.top - 1f,
                    pageRect.right + 2f,
                    pageRect.bottom + 3f,
                    shadow,
                )
            }
            canvas.drawRect(pageRect.left - 1f, pageRect.top - 1f, pageRect.right + 1f, pageRect.bottom + 1f, edge)
            val ref = info.pdfRefs.getOrNull(i)
            if (ref != null && pdfBehind) {
                // Left clear: the screen draws PDF pages under this render, see [drawPdfPages].
                canvas.drawRect(pageRect, hole)
            } else {
                // A PDF page is white whatever the paper colour is.
                fill.color = if (ref != null) Color.WHITE else bg.color
                canvas.drawRect(pageRect, fill)
            }

            val pageWidth = vp.pages.width(i)
            val pageHeight = vp.pages.height(i)
            canvas.save()
            canvas.translate(pageRect.left, pageRect.top)
            canvas.scale(s, s)
            canvas.clipRect(0f, 0f, pageWidth, pageHeight)
            visible.set(
                -pageRect.left / s,
                -pageRect.top / s,
                (vp.viewWidth - pageRect.left) / s,
                (vp.viewHeight - pageRect.top) / s,
            )
            visible.intersect(0f, 0f, pageWidth, pageHeight)
            if (ref != null && !pdfBehind) pdf?.draw(canvas, ref, pageWidth, pageHeight)
            drawPage(canvas, bg, ref == null, info.pageIds.getOrNull(i), scenes, visible, s, hidden, images)
            canvas.restore()
        }
        if (vp.hasAddTarget()) drawAddTarget(canvas, vp)
    }

    /**
     * Draws the bare paper of every visible page at [vp]: shadow, edge and colour, white for a PDF page, and the
     * add target. Cheap enough for every frame, so a page the last render did not reach is never just desk.
     */
    fun drawBlankPages(canvas: Canvas, vp: Viewport, info: NotebookInfo) {
        if (vp.infinite) return
        val bg = info.background
        applyChrome(bg.color)
        viewRect.set(0f, 0f, vp.viewWidth, vp.viewHeight)
        for (i in vp.visiblePages()) {
            vp.pageRect(i, pageRect)
            if (shadow.alpha != 0) {
                canvas.drawRect(
                    pageRect.left - 2f,
                    pageRect.top - 1f,
                    pageRect.right + 2f,
                    pageRect.bottom + 3f,
                    shadow,
                )
            }
            canvas.drawRect(pageRect.left - 1f, pageRect.top - 1f, pageRect.right + 1f, pageRect.bottom + 1f, edge)
            fill.color = if (info.pdfRefs.getOrNull(i) != null) Color.WHITE else bg.color
            canvas.drawRect(pageRect, fill)
        }
        if (vp.hasAddTarget()) drawAddTarget(canvas, vp)
    }

    /**
     * Draws the visible PDF pages, white plus their best bitmap, at [vp]. The screen draws these every frame
     * under a render made with `pdfBehind`, so pages that pan or zoom into view show at once, without waiting
     * for the next render, and scaling the large page bitmaps stays on the GPU.
     */
    fun drawPdfPages(canvas: Canvas, vp: Viewport, info: NotebookInfo, pdf: PdfBackgrounds) {
        if (vp.infinite || !info.hasPdf) return
        viewRect.set(0f, 0f, vp.viewWidth, vp.viewHeight)
        fill.color = Color.WHITE
        for (i in 0 until vp.pageCount) {
            val ref = info.pdfRefs.getOrNull(i) ?: continue
            vp.pageRect(i, pageRect)
            if (!RectF.intersects(pageRect, viewRect)) continue
            canvas.drawRect(pageRect, fill)
            canvas.save()
            canvas.clipRect(pageRect)
            canvas.translate(pageRect.left, pageRect.top)
            canvas.scale(vp.scale, vp.scale)
            pdf.draw(canvas, ref, vp.pages.width(i), vp.pages.height(i))
            canvas.restore()
        }
    }

    /**
     * Draws what a page holds over [area], in page-local units, onto a canvas that is already scaled by [scale]
     * (pixels per unit): the paper pattern when [pattern] is set, then the images, text and ink of every visible layer,
     * the images from [images]. Shared by the screen and by export, so a page looks the same in both. The paper
     * colour and any PDF page behind the ink are the caller's to draw first.
     */
    fun drawPage(
        canvas: Canvas,
        bg: PageBackground,
        pattern: Boolean,
        pageId: String?,
        scenes: Map<String, PageScene>,
        area: RectF,
        scale: Float,
        hidden: HiddenStrokes? = null,
        images: ImageBitmaps? = null,
    ) {
        applyChrome(bg.color)
        if (pattern) drawPattern(canvas, bg, area, scale)
        drawStrokes(canvas, pageId, scenes, hidden, area, images, bg.color)
    }

    private fun drawStrokes(
        canvas: Canvas,
        pageId: String?,
        scenes: Map<String, PageScene>,
        hidden: HiddenStrokes?,
        area: RectF,
        images: ImageBitmaps?,
        paper: Int,
    ) {
        val scene = pageId?.let { scenes[it] } ?: return
        val skip = hidden?.takeIf { it.pageId == pageId }?.all
        for (layer in scene.layers) {
            if (layer.alpha <= 0) continue
            if (layer.alpha >= LAYER_OPAQUE) {
                drawLayer(canvas, layer, skip, area, images, paper)
            } else if (touches(layer, skip, area)) {
                // Drawn as one group so strokes that overlap inside a faded layer do not show through each other.
                val saved = canvas.saveLayerAlpha(area, layer.alpha)
                drawLayer(canvas, layer, skip, area, images, paper)
                canvas.restoreToCount(saved)
            }
        }
    }

    private fun drawLayer(
        canvas: Canvas,
        layer: SceneLayer,
        skip: Set<StrokeId>?,
        area: RectF,
        images: ImageBitmaps?,
        paper: Int,
    ) {
        for (image in layer.images) {
            if (skip != null && image.id in skip) continue
            if (RectF.intersects(image.bounds, area)) imagePainter.draw(canvas, image, images, paper)
        }
        for (box in layer.texts) {
            if (skip != null && box.id in skip) continue
            if (RectF.intersects(box.bounds, area)) textPainter.draw(canvas, box, paper)
        }
        for (stroke in layer.strokes) {
            if (skip != null && stroke.id in skip) continue
            if (RectF.intersects(stroke.bounds, area)) canvas.drawPath(stroke.path, stroke.paint)
        }
    }

    private fun touches(layer: SceneLayer, skip: Set<StrokeId>?, area: RectF): Boolean {
        for (image in layer.images) {
            if (skip != null && image.id in skip) continue
            if (RectF.intersects(image.bounds, area)) return true
        }
        for (box in layer.texts) {
            if (skip != null && box.id in skip) continue
            if (RectF.intersects(box.bounds, area)) return true
        }
        for (stroke in layer.strokes) {
            if (skip != null && stroke.id in skip) continue
            if (RectF.intersects(stroke.bounds, area)) return true
        }
        return false
    }

    /** Draws the paper pattern over [area], in page-local units. */
    private fun drawPattern(canvas: Canvas, bg: PageBackground, area: RectF, scale: Float) {
        if (bg.pattern == PatternKind.BLANK) return
        val spacing = bg.spacingMm * NotebookInfo.PT_PER_MM
        if (spacing * scale < MIN_SPACING_PX || area.isEmpty) return
        val firstCol = ceil(area.left / spacing).toInt()
        val lastCol = floor(area.right / spacing).toInt()
        val firstRow = ceil(area.top / spacing).toInt()
        val lastRow = floor(area.bottom / spacing).toInt()
        val cols = max(0, lastCol - firstCol + 1)
        val rows = max(0, lastRow - firstRow + 1)
        when (bg.pattern) {
            PatternKind.LINED -> drawRows(canvas, area, spacing, firstRow, rows)

            PatternKind.GRID -> {
                drawRows(canvas, area, spacing, firstRow, rows)
                val v = FloatArray(cols * 4)
                for (c in 0 until cols) {
                    val x = (firstCol + c) * spacing
                    v[4 * c] = x
                    v[4 * c + 1] = area.top
                    v[4 * c + 2] = x
                    v[4 * c + 3] = area.bottom
                }
                canvas.drawLines(v, lines)
            }

            PatternKind.DOTTED -> {
                val p = FloatArray(cols * rows * 2)
                var n = 0
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        p[n++] = (firstCol + c) * spacing
                        p[n++] = (firstRow + r) * spacing
                    }
                }
                dots.strokeWidth = max(1.2f, 1.8f / scale)
                canvas.drawPoints(p, dots)
            }
        }
    }

    private fun drawRows(canvas: Canvas, area: RectF, spacing: Float, firstRow: Int, rows: Int) {
        val v = FloatArray(rows * 4)
        for (r in 0 until rows) {
            val y = (firstRow + r) * spacing
            v[4 * r] = area.left
            v[4 * r + 1] = y
            v[4 * r + 2] = area.right
            v[4 * r + 3] = y
        }
        canvas.drawLines(v, lines)
    }

    private fun drawAddTarget(canvas: Canvas, vp: Viewport) {
        val r = vp.addTargetRect(pageRect)
        if (!RectF.intersects(r, viewRect)) return
        val radius = 12f * vp.scale
        canvas.drawRoundRect(r, radius, radius, addFill)
        canvas.drawRoundRect(r, radius, radius, addStroke)
        val arm = r.height() * 0.2f
        val cx = r.centerX()
        val cy = r.centerY()
        canvas.drawLine(cx - arm, cy, cx + arm, cy, addPlus)
        canvas.drawLine(cx, cy - arm, cx, cy + arm, addPlus)
    }
}
