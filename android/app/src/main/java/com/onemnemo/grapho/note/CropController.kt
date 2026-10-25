package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.hypot

/** What the crop editor needs from the canvas it lives in. */
internal interface CropHost {
    val currentViewport: Viewport
    val density: Float

    fun redraw()
    fun menuChanged(menu: CropMenu?)
}

/** Where the crop card belongs, in view pixels: the box of the whole picture. */
class CropMenu(val bounds: RectF)

/**
 * Crops one image. The whole picture stays where it is on the page, and the frame is dragged by its corners and
 * edges, or moved whole, in the picture's own (possibly turned) frame. Touch input arrives from the canvas on the UI
 * thread; [rect] is what the render thread draws.
 *
 * Positions inside the picture are fractions of its width and height, the same as [SceneImage.crop].
 */
internal class CropController(val page: Int, val image: SceneImage, private val host: CropHost) {
    /** Maps picture fractions onto the page, in page-local units. Fixed while cropping. */
    val pictureToPage = Matrix()
    private val pageToPicture = Matrix()

    /** The part of the picture kept, as fractions: left, top, right, bottom. */
    val rect = RectF(image.crop)

    private val start = RectF()
    private var grab = NONE
    private var grabU = 0f
    private var grabV = 0f
    private var touchDown = false
    private var menuShown = false
    private val point = FloatArray(2)

    init {
        val c = image.crop
        val width = (c.right - c.left).coerceAtLeast(TINY)
        val height = (c.bottom - c.top).coerceAtLeast(TINY)
        // Picture fractions to the unit square of the part that is shown now, then onto the page.
        pictureToPage.setTranslate(-c.left, -c.top)
        pictureToPage.postScale(1f / width, 1f / height)
        pictureToPage.postConcat(image.matrix)
        pictureToPage.invert(pageToPicture)
    }

    /** Whether the frame differs from the crop the image had. */
    fun changed(): Boolean {
        val c = image.crop
        return abs(rect.left - c.left) > EPSILON || abs(rect.top - c.top) > EPSILON ||
            abs(rect.right - c.right) > EPSILON || abs(rect.bottom - c.bottom) > EPSILON
    }

    /** Shows the whole picture again. */
    fun reset() {
        rect.set(0f, 0f, 1f, 1f)
        host.redraw()
    }

    /** Puts the view position of picture point ([u], [v]) in [out] as x, y. */
    fun viewPoint(vp: Viewport, u: Float, v: Float, out: FloatArray) {
        out[0] = u
        out[1] = v
        pictureToPage.mapPoints(out)
        out[0] = vp.viewX(page, out[0])
        out[1] = vp.viewY(page, out[1])
    }

    /** Puts the picture position under view point ([x], [y]) in [out] as u, v. */
    private fun picturePoint(vp: Viewport, x: Float, y: Float, out: FloatArray) {
        out[0] = vp.localX(page, x)
        out[1] = vp.localY(page, y)
        pageToPicture.mapPoints(out)
    }

    /**
     * Puts handle [i] in picture fractions in [out]: corners 0 to 3 clockwise from the top left, then the top, right,
     * bottom and left edges.
     */
    fun handle(i: Int, out: FloatArray) {
        val midX = (rect.left + rect.right) / 2f
        val midY = (rect.top + rect.bottom) / 2f
        when (i) {
            0 -> set(out, rect.left, rect.top)
            1 -> set(out, rect.right, rect.top)
            2 -> set(out, rect.right, rect.bottom)
            3 -> set(out, rect.left, rect.bottom)
            4 -> set(out, midX, rect.top)
            5 -> set(out, rect.right, midY)
            6 -> set(out, midX, rect.bottom)
            else -> set(out, rect.left, midY)
        }
    }

    private fun set(out: FloatArray, u: Float, v: Float) {
        out[0] = u
        out[1] = v
    }

    /** Grabs the handle or the frame under ([x], [y]) in view pixels. Returns false if the touch is on neither. */
    fun beginDrag(x: Float, y: Float): Boolean {
        val vp = host.currentViewport
        var best = NONE
        var bestDistance = TOUCH_DP * host.density
        for (i in 0 until HANDLES) {
            handle(i, point)
            viewPoint(vp, point[0], point[1], point)
            val distance = hypot(x - point[0], y - point[1])
            if (distance <= bestDistance) {
                best = i
                bestDistance = distance
            }
        }
        picturePoint(vp, x, y, point)
        if (best == NONE) {
            if (!rect.contains(point[0], point[1])) return false
            best = MOVE
            start.set(rect)
            grabU = point[0]
            grabV = point[1]
        } else {
            val pu = point[0]
            val pv = point[1]
            handle(best, point)
            grabU = pu - point[0]
            grabV = pv - point[1]
        }
        grab = best
        return true
    }

    fun drag(x: Float, y: Float) {
        if (grab == NONE) return
        picturePoint(host.currentViewport, x, y, point)
        if (grab == MOVE) {
            val dx = (point[0] - grabU).coerceIn(-start.left, 1f - start.right)
            val dy = (point[1] - grabV).coerceIn(-start.top, 1f - start.bottom)
            rect.set(start.left + dx, start.top + dy, start.right + dx, start.bottom + dy)
        } else {
            val u = point[0] - grabU
            val v = point[1] - grabV
            val sides = SIDES[grab]
            // A frame that is already narrower than the minimum can still be dragged wider.
            if (sides and LEFT != 0) rect.left = u.coerceIn(0f, maxOf(0f, rect.right - MIN_SPAN))
            if (sides and RIGHT != 0) rect.right = u.coerceIn(minOf(1f, rect.left + MIN_SPAN), 1f)
            if (sides and TOP != 0) rect.top = v.coerceIn(0f, maxOf(0f, rect.bottom - MIN_SPAN))
            if (sides and BOTTOM != 0) rect.bottom = v.coerceIn(minOf(1f, rect.top + MIN_SPAN), 1f)
        }
        host.redraw()
    }

    fun endDrag() {
        grab = NONE
    }

    /** The card is out of the way while any touch is down, and shows again when the touch ends. */
    fun touching(on: Boolean) {
        touchDown = on
        syncMenu()
    }

    /** Tells the canvas where the card belongs now, or that it should be hidden. */
    fun syncMenu() {
        if (touchDown) {
            if (menuShown) {
                menuShown = false
                host.menuChanged(null)
            }
            return
        }
        menuShown = true
        val vp = host.currentViewport
        val box = RectF()
        for (corner in 0 until 4) {
            viewPoint(vp, if (corner == 0 || corner == 3) 0f else 1f, if (corner < 2) 0f else 1f, point)
            if (corner == 0) box.set(point[0], point[1], point[0], point[1]) else box.union(point[0], point[1])
        }
        host.menuChanged(CropMenu(box))
    }

    companion object {
        const val HANDLES = 8
        private const val NONE = -1
        private const val MOVE = 8
        private const val LEFT = 1
        private const val TOP = 2
        private const val RIGHT = 4
        private const val BOTTOM = 8
        private val SIDES = intArrayOf(
            LEFT or TOP,
            RIGHT or TOP,
            RIGHT or BOTTOM,
            LEFT or BOTTOM,
            TOP,
            RIGHT,
            BOTTOM,
            LEFT,
        )

        /** The smallest the frame can be dragged to, as a fraction of the picture each way. */
        private const val MIN_SPAN = 0.05f
        private const val EPSILON = 0.0005f
        private const val TINY = 0.001f
        private const val TOUCH_DP = 24f
    }
}
