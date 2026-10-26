package com.onemnemo.grapho.note

import android.graphics.RectF
import com.onemnemo.grapho.engine.NativeGuide
import kotlin.math.hypot
import kotlin.math.roundToInt

/** What [GuideController] needs from the canvas. */
internal interface GuideHost {
    val currentViewport: Viewport
    val density: Float
    val touchSlop: Float

    /** The paper colour, for choosing legible marks. */
    val paper: Int

    fun redraw()

    /** A guide was closed from its own button. */
    fun guidesChanged()
}

/**
 * The ruler and protractor: which are on, where they sit, and the fingers that move them. A session tool, so none of
 * it is saved. Positions are in content points, which is what keeps the ticks on the paper's millimetres as the view
 * pans and zooms. UI thread, except that [guides] is read by the render thread.
 */
internal class GuideController(
    private val host: GuideHost,
    private val snapAngle: (Float) -> Float = { NativeGuide.nativeSnapAngle(it) },
    private val rulerLabel: (Float) -> Int = { NativeGuide.nativeRulerDegrees(it).roundToInt() },
) {
    @Volatile var guides = GuideSet()
        private set

    private val readout by lazy { GuideReadout(host.density) }
    private val scratch = FloatArray(2)
    private val moved = FloatArray(3)

    // Drag state: the guide under the fingers, where the fingers were last seen, and whether this touch can still
    // count as a tap on the close button.
    private var target: GuideKind? = null
    private val last = FloatArray(4)
    private var lastCount = 0
    private var downX = 0f
    private var downY = 0f
    private var closing = false

    // The protractor a stroke started at the centre of, as one packed guide, and the label box last drawn. The
    // drawing parts are made on first use so the logic above can run without them.
    private var reading: FloatArray? = null
    private val box by lazy { RectF() }
    private val prevBox by lazy { RectF() }

    val rulerOn: Boolean get() = guides.ruler != null
    val protractorOn: Boolean get() = guides.protractor != null

    fun setRuler(on: Boolean) = switch(GuideKind.RULER, on)

    fun setProtractor(on: Boolean) = switch(GuideKind.PROTRACTOR, on)

    // A guide that comes on is centred in the view, the ruler a little above the protractor.
    private fun switch(kind: GuideKind, on: Boolean) {
        if (on == (guides.place(kind) != null)) return
        if (!on) {
            guides = guides.with(kind, null)
        } else {
            val vp = host.currentViewport
            val s = vp.scale
            val dy = if (kind == GuideKind.RULER) -0.12f else 0.12f
            val x = (vp.viewWidth / 2 - vp.offsetX) / s
            val y = (vp.viewHeight * (0.5f + dy) - vp.offsetY) / s
            guides = guides.with(kind, GuidePlace(x, y, 0f, 0f, rulerLabel(0f)))
        }
        host.redraw()
    }

    /** True when a finger landing at ([x], [y]) is on a guide, which then follows it until [end]. */
    fun begin(x: Float, y: Float): Boolean {
        val vp = host.currentViewport
        val margin = MARGIN_DP * host.density
        for (kind in GuideKind.entries) {
            val p = guides.place(kind) ?: continue
            if (GuideMath.hits(kind, p, vp.scale, vp.offsetX, vp.offsetY, x, y, margin, scratch)) {
                target = kind
                closing = GuideMath.hitsClose(
                    kind, p, vp.scale, vp.offsetX, vp.offsetY, x, y, CLOSE_DP * host.density, scratch,
                )
                downX = x
                downY = y
                last[0] = x
                last[1] = y
                lastCount = 1
                return true
            }
        }
        return false
    }

    /**
     * The fingers' positions as `[x0, y0, x1, y1]`, [count] of them from 1 to 2. When the number of fingers has
     * changed, the new positions only become the reference, so the guide does not jump.
     */
    fun update(pointers: FloatArray, count: Int) {
        val kind = target ?: return
        val p = guides.place(kind) ?: return
        if (count != lastCount) {
            rebase(pointers, count)
            return
        }
        if (count > 1 || hypot(pointers[0] - downX, pointers[1] - downY) > host.touchSlop) closing = false
        val vp = host.currentViewport
        GuideMath.move(p, vp.scale, vp.offsetX, vp.offsetY, last, pointers, count, moved)
        val snapped = snapAngle(moved[2])
        guides = guides.with(kind, GuidePlace(moved[0], moved[1], moved[2], snapped, rulerLabel(snapped)))
        remember(pointers, count)
        host.redraw()
    }

    /** Takes [pointers] as the new reference, after a finger landed or lifted. */
    fun rebase(pointers: FloatArray, count: Int) {
        if (target == null || count == 0) return
        remember(pointers, count)
    }

    private fun remember(pointers: FloatArray, count: Int) {
        pointers.copyInto(last, 0, 0, count * 2)
        lastCount = count
    }

    /** Ends the touch. A tap that stayed on the close button closes that guide. */
    fun end() {
        val kind = target ?: return
        target = null
        if (!closing) return
        closing = false
        guides = guides.with(kind, null)
        host.redraw()
        host.guidesChanged()
    }

    fun cancel() {
        target = null
        closing = false
    }

    /**
     * Starts a pen stroke at ([x], [y]): hands the stroke the guides, so it follows an edge the pen landed beside, and
     * remembers a protractor whose centre mark it landed on, for [annotate].
     */
    fun beginStroke(stroke: WetStroke, x: Float, y: Float) {
        reading = null
        prevBox.setEmpty()
        if (guides.isEmpty) return
        val vp = host.currentViewport
        val packed = GuideMath.pack(guides, vp.scale, vp.offsetX, vp.offsetY)
        val reach = REACH_DP * host.density
        stroke.guide(packed, x, y, reach)
        val i = NativeGuide.nativeCentreNear(packed, x, y, reach)
        if (i >= 0) reading = packed.copyOfRange(i * GuideMath.STRIDE, (i + 1) * GuideMath.STRIDE)
    }

    /** Ends the stroke's use of the guides. */
    fun endStroke() {
        reading = null
        prevBox.setEmpty()
    }

    /**
     * [frame] with the angle between the protractor's base and the pen at ([x], [y]) drawn beside the pen, for a stroke
     * that started on the protractor's centre mark. Any other stroke gets [frame] back.
     */
    fun annotate(frame: WetFrame, x: Float, y: Float): WetFrame {
        val guide = reading ?: return frame
        val degrees = NativeGuide.nativeReading(guide, x, y)
        if (degrees < 0f) return frame
        readout.box(x, y, box)
        val dirty = RectF(frame.dirty).apply {
            union(box)
            union(prevBox)
        }
        prevBox.set(box)
        val whole = degrees.roundToInt()
        val text = "$whole°  ${180 - whole}°"
        val dark = isDarkPaper(host.paper)
        val at = RectF(box)
        return WetFrame(dirty, frame.clip) { canvas ->
            frame.draw(canvas)
            readout.draw(canvas, text, at, dark)
        }
    }

    companion object {
        /** How near an edge the pen can land and still be guided along it. */
        const val REACH_DP = 14f

        /** A finger this close to a guide's body still moves it. */
        const val MARGIN_DP = 6f
        const val CLOSE_DP = 22f
    }
}
