package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Draws the lasso's selection alone, on a transparent background, for copying or exporting as a picture. */
internal object SelectionRender {
    /** Room around the selection's ink, in page-local units. */
    private const val PAD = 6f

    /** Pixels per page-local unit when the selection is small, and the most pixels on the long side. */
    private const val WANTED_SCALE = 2f
    private const val MAX_SIDE = 4096

    /**
     * The strokes, images and text of [ids] from [scene], in their layers' order, inside [bounds] (page-local units)
     * grown by a little padding. [paper] only tones the stand-in for a picture that is not decoded yet. Null when the
     * area is empty or a bitmap of that size cannot be made.
     */
    fun render(scene: PageScene, ids: Set<StrokeId>, bounds: RectF, images: ImageBitmaps?, paper: Int): Bitmap? {
        val area = RectF(bounds).apply { inset(-PAD, -PAD) }
        if (area.isEmpty) return null
        val scale = min(WANTED_SCALE, MAX_SIDE / max(area.width(), area.height()))
        val width = ceil(area.width() * scale).toInt().coerceAtLeast(1)
        val height = ceil(area.height() * scale).toInt().coerceAtLeast(1)
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            return null
        }
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        canvas.translate(-area.left, -area.top)
        val imagePainter = ImagePainter()
        val textPainter = TextPainter()
        for (layer in scene.layers) {
            if (layer.alpha <= 0) continue
            // A faded layer is drawn as one group, like on the page, so overlapping strokes do not show through.
            val saved = if (layer.alpha < LAYER_OPAQUE) canvas.saveLayerAlpha(area, layer.alpha) else -1
            for (image in layer.images) {
                if (image.id in ids) imagePainter.draw(canvas, image, images, paper)
            }
            for (box in layer.texts) {
                if (box.id in ids) textPainter.draw(canvas, box, paper)
            }
            for (stroke in layer.strokes) {
                if (stroke.id in ids) canvas.drawPath(stroke.path, stroke.paint)
            }
            if (saved >= 0) canvas.restoreToCount(saved)
        }
        return bitmap
    }
}
