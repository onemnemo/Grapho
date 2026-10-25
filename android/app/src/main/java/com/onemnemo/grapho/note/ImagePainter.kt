package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.roundToInt

/** Draws images onto a page canvas. Keeps its paints, so one instance belongs to one drawing thread. */
internal class ImagePainter {
    private val picture = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
    }
    private val placeholder = Paint().apply { style = Paint.Style.FILL }
    private val src = Rect()
    private val unit = RectF(0f, 0f, 1f, 1f)

    /**
     * Draws the shown part of [image] at [alpha] (0 to 255). A picture that is not decoded yet is a plain
     * placeholder in its place, in a tone that suits [paper].
     */
    fun draw(canvas: Canvas, image: SceneImage, bitmaps: ImageBitmaps?, paper: Int, alpha: Int = LAYER_OPAQUE) {
        val bitmap = bitmaps?.bitmap(image.source)
        canvas.save()
        canvas.concat(image.matrix)
        if (bitmap == null) {
            fillPlaceholder(canvas, paper, alpha)
        } else {
            val crop = image.crop
            src.set(
                (crop.left * bitmap.width).roundToInt(),
                (crop.top * bitmap.height).roundToInt(),
                (crop.right * bitmap.width).roundToInt(),
                (crop.bottom * bitmap.height).roundToInt(),
            )
            if (src.width() < 1) src.right = src.left + 1
            if (src.height() < 1) src.bottom = src.top + 1
            picture.alpha = alpha
            canvas.drawBitmap(bitmap, src, unit, picture)
        }
        canvas.restore()
    }

    /** Draws the whole of [image]'s picture into the unit square of the canvas, as the crop editor shows it. */
    fun drawWhole(canvas: Canvas, image: SceneImage, bitmaps: ImageBitmaps?, paper: Int) {
        val bitmap: Bitmap? = bitmaps?.bitmap(image.source)
        if (bitmap == null) {
            fillPlaceholder(canvas, paper, LAYER_OPAQUE)
        } else {
            picture.alpha = LAYER_OPAQUE
            canvas.drawBitmap(bitmap, null, unit, picture)
        }
    }

    private fun fillPlaceholder(canvas: Canvas, paper: Int, alpha: Int) {
        placeholder.color = PaperChrome.imageFill(paper)
        placeholder.alpha = alpha
        canvas.drawRect(unit, placeholder)
    }
}
