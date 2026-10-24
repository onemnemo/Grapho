package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope

/** The gap kept clear of swatches either side of the fixed buttons on the full circle, in degrees. */
private const val GAP = 16f

/**
 * The size dots sit on the rim between these angles and the opacity band takes the rest, which leaves
 * the gap between them for the grown plate's readout.
 */
internal const val SIZE_FROM = -86f
internal const val SIZE_TO = -18f
private const val OPACITY_FROM = 20f
private const val OPACITY_TO = 84f

private enum class Zone { Spin, Tools, Size, Opacity, EraserSize, Option, Stem }

/** Degrees between swatches: fixed on the docked rim, an even share of the circle on the full one. */
internal fun stepOf(full: Boolean, count: Int): Float = if (full && count > 0) 360f / count else RIM_STEP

internal fun stepOf(geo: Geo, count: Int): Float = stepOf(geo.full, count)

/** How visible a swatch at [angle] is: faded out toward the ends of the docked arc and into the gaps of the full circle. */
internal fun swatchFade(geo: Geo, angle: Float, step: Float): Float =
    if (geo.full) {
        val d = minOf(abs(normDeg(angle + 90f)), abs(normDeg(angle - 90f)))
        ((d - GAP) / step).coerceIn(0f, 1f)
    } else {
        ((4f * RIM_STEP - abs(angle)) / RIM_STEP).coerceIn(0f, 1f)
    }

/** Radius in dp of the dot standing for a size number from 1 to 100. */
internal fun dotRadius(number: Float): Float = 2f + 7f * ((number - 1f) / 99f).coerceIn(0f, 1f)

internal fun sizeAngle(number: Float): Float = SIZE_FROM + (number - 1f) / 99f * (SIZE_TO - SIZE_FROM)

private fun numberAt(degrees: Float): Float = 1f + ((degrees - SIZE_FROM) / (SIZE_TO - SIZE_FROM)).coerceIn(0f, 1f) * 99f

/** Opacity from 100 percent at the end by the size dots down to 10 percent at the far end, as an alpha. */
private fun alphaAt(degrees: Float): Int {
    val t = ((degrees - OPACITY_FROM) / (OPACITY_TO - OPACITY_FROM)).coerceIn(0f, 1f)
    return ((100f - 90f * t) * 2.55f).roundToInt().coerceIn(ALPHA_MIN, ALPHA_MAX)
}

private fun angleOfAlpha(alpha: Int): Float {
    val t = ((alpha / 2.55f - 10f) / 90f).coerceIn(0f, 1f)
    return OPACITY_FROM + (1f - t) * (OPACITY_TO - OPACITY_FROM)
}

internal fun applyAlpha(state: ToolbarState, alpha: Int) {
    if (state.mode == ToolMode.ERASER) state.mode = state.drawMode
    state.alpha = alpha
}

/** The band along the lower arc of the rim: a checkerboard fading into the colour, with a ring on the current opacity. */
internal fun DrawScope.drawOpacityBand(c: Offset, rimR: Float, u: Float, color: Color, alpha: Int, edge: DockEdge) {
    // Drawn in the left-docked layout and turned onto the real edge.
    withTransform({
        rotate(edge.face, c)
        scale(1f, edge.mirror, c)
    }) { drawOpacityBandLeft(c, rimR, u, color, alpha) }
}

private fun DrawScope.drawOpacityBandLeft(c: Offset, rimR: Float, u: Float, color: Color, alpha: Int) {
    val outer = (rimR + 13f) * u
    val innerR = (rimR - 13f) * u
    val sweep = OPACITY_TO - OPACITY_FROM
    // Round ends: a half circle across the band's width at each end.
    val cap = 13f * u
    val end = polar(rimR, OPACITY_TO)
    val start = polar(rimR, OPACITY_FROM)
    val band = Path().apply {
        arcTo(Rect(c, outer), OPACITY_FROM, sweep, true)
        arcTo(Rect(Offset(c.x + end.x * u, c.y + end.y * u), cap), OPACITY_TO, 180f, false)
        arcTo(Rect(c, innerR), OPACITY_TO, -sweep, false)
        arcTo(Rect(Offset(c.x + start.x * u, c.y + start.y * u), cap), OPACITY_FROM + 180f, 180f, false)
        close()
    }
    clipPath(band) {
        val cover = Offset(c.x - outer, c.y - outer)
        val coverSize = Size(2f * outer, 2f * outer)
        drawRect(GraphoWheel.checkerDark, cover, coverSize)
        val sq = 8f * u
        var row = 0
        var y = c.y
        while (y < c.y + outer) {
            var col = 0
            var x = c.x - outer
            while (x < c.x + outer) {
                if ((row + col) % 2 == 0) drawRect(GraphoWheel.checkerLight, Offset(x, y), Size(sq, sq))
                x += sq
                col++
            }
            y += sq
            row++
        }
        drawRect(
            Brush.sweepGradient(
                OPACITY_FROM / 360f to color.copy(alpha = 1f),
                OPACITY_TO / 360f to color.copy(alpha = 0.1f),
                center = c,
            ),
            cover,
            coverSize,
        )
    }
    drawPath(band, GraphoWheel.bandEdge, style = Stroke(width = 1.5f * u))
    val p = polar(rimR, angleOfAlpha(alpha))
    drawCircle(GraphoWheel.selectedIndicator, 11f * u, Offset(c.x + p.x * u, c.y + p.y * u), style = Stroke(width = 2.5f * u))
}

/**
 * Touches on the wheel. A touch that stays within the touch slop is a tap and selects what is under it, or
 * closes the open tool's controls when it is on their plate; a drag never selects. A drag on the rim in
 * colour mode, or on the tool ring, turns that ring with the finger's angle and lets it glide on release. In
 * size mode a drag on the rim sets the size or the opacity continuously. Nothing happens while [active] says
 * the shape is not the one at rest.
 */
internal suspend fun PointerInputScope.rimGestures(
    geo: Geo,
    state: ToolbarState,
    rim: SpinState,
    tools: SpinState,
    toolCount: Int,
    scope: CoroutineScope,
    haptic: HapticFeedback,
    sizeMode: () -> Boolean,
    active: () -> Boolean,
    stemAt: () -> Float,
    onStem: () -> Unit,
    onTool: (Int) -> Unit,
) {
    val centreX = geo.cx * density
    val centreY = geo.cy * density
    fun angleOf(p: Offset): Float = geo.edge.fromScreen(atan2(p.y - centreY, p.x - centreX) * 180f / PI.toFloat())

    awaitEachGesture {
        val down = awaitFirstDown()
        // The wheel's other shape is under this one while it moves, and leaves the touch alone.
        if (!active()) return@awaitEachGesture
        val dx = (down.position.x - centreX) / density
        val dy = (down.position.y - centreY) / density
        val r = hypot(dx, dy)
        if (r > geo.r) return@awaitEachGesture
        val deg = angleOf(down.position)
        val editing = sizeMode() || state.mode == ToolMode.ERASER
        val zone = when {
            // While the open tool's controls are up its grown plate is all that answers on the tool band.
            editing && isOnStem(geo, stemAt(), dx, dy) -> Zone.Stem
            r < geo.inner -> if (!editing && r > geo.core && (geo.full || abs(deg) <= TOOL_REACH)) Zone.Tools else return@awaitEachGesture
            state.mode == ToolMode.ERASER ->
                if (deg in (ERASER_FROM - 10f)..(ERASER_TO + 10f)) Zone.EraserSize else return@awaitEachGesture
            // The options of Shapes, Text and the lasso are buttons of their own; the rim between them is still.
            sizeMode() && hasToolOptions(state.mode) ->
                if (dotRow(state) != null && deg in (SIZE_FROM - 10f)..(SIZE_TO + 10f)) Zone.Option else return@awaitEachGesture
            !sizeMode() -> Zone.Spin
            deg in (OPACITY_FROM - 6f)..(OPACITY_TO + 10f) -> Zone.Opacity
            deg in (SIZE_FROM - 10f)..(SIZE_TO + 10f) -> Zone.Size
            else -> return@awaitEachGesture
        }
        val spin = when (zone) {
            Zone.Spin -> rim
            Zone.Tools -> tools
            else -> null
        }
        // A touch that stops a glide only stops it.
        val wasMoving = spin?.moving == true
        spin?.grab()

        val slop = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
        if (slop == null) {
            spin?.release(scope, 0f)
            // A touch that was taken over, as when the wheel starts to move, selects nothing.
            if (isReleased(down.id) && !wasMoving && currentEvent.changes.none { it.isConsumed }) {
                when (zone) {
                    Zone.Spin -> swatchAt(geo, state, rim, dx, dy)?.let {
                        haptic.tick()
                        applyColor(state, state.rim[it])
                    }
                    Zone.Tools -> toolAt(geo, tools, toolCount, dx, dy)?.let {
                        haptic.tick()
                        onTool(it)
                    }
                    Zone.Size -> dotAt(deg)?.let {
                        haptic.tick()
                        applySize(state, widthOfNumber(state, it))
                    }
                    Zone.Opacity -> applyAlpha(state, alphaAt(deg))
                    Zone.EraserSize -> eraserStepAt(deg)?.let {
                        haptic.tick()
                        state.eraserPt = it
                    }
                    Zone.Option -> dotRow(state)?.let {
                        haptic.tick()
                        it.set(it.values[dotIndexAt(it, deg)])
                    }
                    Zone.Stem -> {
                        haptic.tick()
                        onStem()
                    }
                }
            }
            return@awaitEachGesture
        }

        if (spin != null) {
            spinWithFinger(spin, scope, down, slop, ::angleOf)
            return@awaitEachGesture
        }
        when (zone) {
            Zone.Size -> {
                applySize(state, widthOfNumber(state, numberAt(angleOf(slop.position))))
                drag(down.id) { change ->
                    change.consume()
                    applySize(state, widthOfNumber(state, numberAt(angleOf(change.position))))
                }
            }
            Zone.Opacity -> {
                applyAlpha(state, alphaAt(angleOf(slop.position)))
                drag(down.id) { change ->
                    change.consume()
                    applyAlpha(state, alphaAt(angleOf(change.position)))
                }
            }
            Zone.Option -> {
                fun scrub(at: Offset) {
                    val row = dotRow(state) ?: return
                    row.set(row.values[dotIndexAt(row, angleOf(at))])
                }
                scrub(slop.position)
                drag(down.id) { change ->
                    change.consume()
                    scrub(change.position)
                }
            }
            Zone.EraserSize -> {
                state.eraserPt = eraserPtAt(angleOf(slop.position))
                drag(down.id) { change ->
                    change.consume()
                    state.eraserPt = eraserPtAt(angleOf(change.position))
                }
            }
            else -> Unit
        }
    }
}

/**
 * Turns [spin] with the finger's angle from [down], starting with the move that
 * passed the touch slop, and lets it glide on release. Shared by the colour
 * ring and the tool ring.
 */
private suspend fun AwaitPointerEventScope.spinWithFinger(
    spin: SpinState,
    scope: CoroutineScope,
    down: PointerInputChange,
    slop: PointerInputChange,
    angleOf: (Offset) -> Float,
) {
    val tracker = VelocityTracker()
    var last = angleOf(down.position)
    fun turn(change: PointerInputChange) {
        val a = angleOf(change.position)
        spin.dragBy(normDeg(a - last))
        last = a
        tracker.addPosition(change.uptimeMillis, Offset(spin.offset, 0f))
    }
    turn(slop)
    drag(down.id) { change ->
        change.consume()
        turn(change)
    }
    val upTime = currentEvent.changes.firstOrNull { it.id == down.id }?.uptimeMillis ?: slop.uptimeMillis
    tracker.addPosition(upTime, Offset(spin.offset, 0f))
    spin.release(scope, tracker.calculateVelocity().x)
}

/** The index of the visible tool within reach of ([dx], [dy]) dp from the wheel's centre, or null. */
private fun toolAt(geo: Geo, tools: SpinState, count: Int, dx: Float, dy: Float): Int? {
    var best: Int? = null
    var bestDistance = 26f
    for (i in 0 until count) {
        val a = toolAngle(geo.full, i, tools.offset, count)
        if (toolFade(geo.full, a) < 0.4f) continue
        val p = polar(geo.toolR, geo.edge.toScreen(a))
        val d = hypot(dx - p.x, dy - p.y)
        if (d <= bestDistance) {
            best = i
            bestDistance = d
        }
    }
    return best
}

/** The index of the visible swatch within reach of ([dx], [dy]) dp from the wheel's centre, or null. */
private fun swatchAt(geo: Geo, state: ToolbarState, rim: SpinState, dx: Float, dy: Float): Int? {
    val slots = state.rim
    val n = slots.size
    val step = stepOf(geo, n)
    var best: Int? = null
    var bestDistance = 22f
    for (i in 0 until n) {
        // The strips between sets are not swatches.
        if (slots[i] == RIM_STRIP) continue
        val a = wrapDeg(i * step + rim.offset, n * step)
        if (swatchFade(geo, a, step) < 0.4f) continue
        val p = polar(geo.rimR, geo.edge.toScreen(a))
        val d = hypot(dx - p.x, dy - p.y)
        if (d <= bestDistance) {
            best = i
            bestDistance = d
        }
    }
    return best
}

/** The number of the size dot closest to [degrees] on the rim, or null when none is near. */
private fun dotAt(degrees: Float): Float? {
    var best: Float? = null
    var bestDistance = 7f
    for (n in DOT_NUMBERS) {
        val d = abs(sizeAngle(n) - degrees)
        if (d <= bestDistance) {
            best = n
            bestDistance = d
        }
    }
    return best
}
