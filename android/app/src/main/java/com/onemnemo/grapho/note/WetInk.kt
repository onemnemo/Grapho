package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.onemnemo.grapho.engine.NativeInk

/**
 * One frame of wet drawing: what to paint, the area that changed since the
 * previous frame, and the area it may paint into. Immutable, so the render
 * thread can draw it while input keeps arriving.
 */
class WetFrame(val dirty: RectF, val clip: RectF, private val drawer: (Canvas) -> Unit) {
    fun draw(canvas: Canvas) = drawer(canvas)
}

private fun Canvas.drawMesh(mesh: FloatArray, count: Int, paint: Paint) {
    if (count == 0) return
    drawVertices(
        Canvas.VertexMode.TRIANGLES, count, mesh, 0,
        null, 0, null, 0, null, 0, 0, paint,
    )
}

/**
 * Append-only float buffer. Snapshots keep the array and length they saw;
 * later appends only write past that length (or into a new array), so a
 * snapshot never changes under the render thread.
 */
private class GrowableFloats(initial: Int) {
    var data = FloatArray(initial)
        private set
    var size = 0
        private set

    fun append(src: FloatArray, from: Int, count: Int) {
        if (size + count > data.size) {
            data = data.copyOf(maxOf(data.size * 2, size + count))
        }
        src.copyInto(data, size, from, from + count)
        size += count
    }
}

/**
 * A wet stroke snapped to a shape: its points in view pixels, packed like [WetStroke.finish],
 * and the frame that shows it. [line] is `[x0, y0, x1, y1]` when the shape is a straight line.
 */
class SnappedShape(val points: FloatArray, val frame: WetFrame, val line: FloatArray?)

/**
 * The stroke being drawn while the pen is down, modelled and tessellated by
 * grapho-core. Works in view pixels. Only used on the UI thread.
 */
class WetStroke(widthPx: Float, tool: Int, argb: Int, line: LineStyle = LineStyle.SOLID) {
    private val handle = NativeInk.nativeNew(widthPx, STABILIZER, tool, line.code)
    private val committed = GrowableFloats(8192)

    // Translucent colours are drawn opaque into a layer that is then faded,
    // so overlapping triangles don't darken each other.
    private val alpha = argb ushr 24
    private val paint = Paint().apply {
        color = argb or 0xFF000000.toInt()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun push(samples: FloatArray) = NativeInk.nativePush(handle, samples)

    /** Draws along the nearest edge of the packed [guides] to ([x], [y]) within [reach], if any is that close. */
    fun guide(guides: FloatArray, x: Float, y: Float, reach: Float): Boolean =
        NativeInk.nativeGuide(handle, guides, x, y, reach)

    /** A snapshot of the stroke so far plus [predicted] samples, painted inside [clip]. */
    fun frame(predicted: FloatArray?, clip: RectF): WetFrame {
        val f = NativeInk.nativeFrame(handle, predicted)
        val dirty = RectF(f[0], f[1], f[2], f[3])
        // Edge pixels of antialiased triangles fall just outside the exact bounds.
        if (!dirty.isEmpty) dirty.inset(-DIRTY_PAD, -DIRTY_PAD)
        val n = f[4].toInt()
        committed.append(f, 5, n)
        val tail = f.copyOfRange(5 + n, f.size)
        val mesh = committed.data
        val count = committed.size
        val a = alpha
        val p = paint
        return WetFrame(dirty, clip) { c ->
            val layer = if (a < 255) c.saveLayerAlpha(null, a) else -1
            c.drawMesh(mesh, count, p)
            c.drawMesh(tail, tail.size, p)
            if (layer >= 0) c.restoreToCount(layer)
        }
    }

    /** How long the pen has stayed still, at [nowMs] on the samples' clock. */
    fun stillMs(nowMs: Float): Float = NativeInk.nativeStillMs(handle, nowMs)

    /**
     * The stroke redrawn as the shape it looks like, replacing everything inside [clip], or null
     * if it matches none. [shaft] is a line drawn just before, `[x0, y0, x1, y1]` in view pixels,
     * that this stroke may be the arrow head of.
     */
    fun snap(clip: RectF, shaft: FloatArray?): SnappedShape? = snapped(NativeInk.nativeSnap(handle, shaft), clip)

    /** After [snap], the shape reshaped for the pen at ([x], [y]), or null if nothing has snapped. */
    fun reshape(x: Float, y: Float, clip: RectF): SnappedShape? = snapped(NativeInk.nativeReshape(handle, x, y), clip)

    /**
     * The shape the Shapes tool draws for a drag between two points in view pixels, replacing everything inside
     * [clip], or null when the drag draws nothing. [spec] is from [ShapeSettings.spec].
     */
    fun drawShape(spec: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, clip: RectF): SnappedShape? =
        snapped(NativeInk.nativeDrawShape(handle, spec, x0, y0, x1, y1), clip)

    private fun snapped(f: FloatArray, clip: RectF): SnappedShape? {
        if (f.isEmpty()) return null
        val line = if (f[0] == 1f) f.copyOfRange(1, 5) else null
        val n = f[5].toInt()
        val points = f.copyOfRange(6, 6 + n)
        val mesh = f.copyOfRange(6 + n, f.size)
        val a = alpha
        val p = paint
        val frame = WetFrame(RectF(clip), clip) { c ->
            val layer = if (a < 255) c.saveLayerAlpha(null, a) else -1
            c.drawMesh(mesh, mesh.size, p)
            if (layer >= 0) c.restoreToCount(layer)
        }
        return SnappedShape(points, frame, line)
    }

    /** Ends the stroke and returns its stabilised points in view pixels. */
    fun finish(): FloatArray = NativeInk.nativeFinish(handle)

    /** Ends the stroke and throws it away. */
    fun cancel() {
        NativeInk.nativeFinish(handle)
    }

    private companion object {
        const val STABILIZER = 0.25f
        const val DIRTY_PAD = 2f
    }
}
