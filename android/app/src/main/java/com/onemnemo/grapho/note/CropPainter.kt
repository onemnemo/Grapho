package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Draws the crop editor on the page layer: the whole picture dimmed where it lies, the kept part bright, and the
 * frame with its handles. Render thread only.
 */
internal class CropPainter(private val density: Float, private val images: ImageBitmaps) {
    private val dim = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(DIM_ALPHA, 0, 0, 0)
    }
    private val frame = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val handleFill = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = SelectionColours.WHITE
    }
    private val handleRing = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val imagePainter = ImagePainter()
    private val path = Path()
    private val pageRect = RectF()
    private val point = FloatArray(2)

    fun draw(canvas: Canvas, vp: Viewport, info: NotebookInfo, crop: CropController) {
        val paper = info.background.color
        val dark = isDarkPaper(paper)
        frame.color = SelectionColours.accent(dark)
        handleRing.color = frame.color
        drawPicture(canvas, vp, paper, crop)
        drawFrame(canvas, vp, crop)
    }

    private fun drawPicture(canvas: Canvas, vp: Viewport, paper: Int, crop: CropController) {
        canvas.save()
        if (vp.infinite) {
            canvas.translate(vp.offsetX, vp.offsetY)
        } else {
            vp.pageRect(crop.page, pageRect)
            canvas.clipRect(pageRect)
            canvas.translate(pageRect.left, pageRect.top)
        }
        canvas.scale(vp.scale, vp.scale)
        // In the picture's own frame the whole picture is the unit square.
        canvas.concat(crop.pictureToPage)
        imagePainter.drawWhole(canvas, crop.image, images, paper)
        canvas.drawRect(0f, 0f, 1f, 1f, dim)
        canvas.save()
        canvas.clipRect(crop.rect)
        imagePainter.drawWhole(canvas, crop.image, images, paper)
        canvas.restore()
        canvas.restore()
    }

    private fun drawFrame(canvas: Canvas, vp: Viewport, crop: CropController) {
        path.reset()
        for (corner in 0 until 4) {
            crop.handle(corner, point)
            crop.viewPoint(vp, point[0], point[1], point)
            if (corner == 0) path.moveTo(point[0], point[1]) else path.lineTo(point[0], point[1])
        }
        path.close()
        canvas.drawPath(path, frame)
        for (i in 0 until CropController.HANDLES) {
            crop.handle(i, point)
            crop.viewPoint(vp, point[0], point[1], point)
            val r = (if (i < 4) CORNER_DP else EDGE_DP) * density
            canvas.drawCircle(point[0], point[1], r, handleFill)
            canvas.drawCircle(point[0], point[1], r, handleRing)
        }
    }

    private companion object {
        const val DIM_ALPHA = 150
        const val CORNER_DP = 7f
        const val EDGE_DP = 5f
    }
}
