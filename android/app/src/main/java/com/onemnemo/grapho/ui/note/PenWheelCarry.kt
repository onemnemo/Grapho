package com.onemnemo.grapho.ui.note

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How much a carried circle grows when lifted. */
internal const val CARRY_SCALE = 0.04f

/** The circle drawn at the docked disc's size, so the two outlines match. */
internal const val DOCKED_SCALE = DISC_R / POPUP_R

/** How far in dp the finger moves into the page before a docked wheel comes off its edge as the circle. */
private const val PULL_OUT = 72f

private const val HINT_FADE_MS = 140

private val SETTLE: AnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
private val HINT_FADE: AnimationSpec<Float> = tween(HINT_FADE_MS)

internal fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * A 0 to 1 value read as state. It is set at once, which the wheel needs where a start must not
 * show last time's value for a frame, or sprung to a target.
 */
private class Ease(start: Float) {
    var value by mutableFloatStateOf(start)
        private set
    private var job: Job? = null

    fun set(v: Float) {
        job?.cancel()
        job = null
        value = v
    }

    fun to(scope: CoroutineScope, target: Float, spec: AnimationSpec<Float> = SETTLE) {
        job?.cancel()
        job = scope.launch { animateTo(target, spec) }
    }

    suspend fun animateTo(target: Float, spec: AnimationSpec<Float> = SETTLE) {
        animate(value, target, animationSpec = spec) { v, _ -> value = v }
        value = target
    }
}

private enum class Mode { Rest, Slide, Free, Land }

/**
 * Where the wheel is and what shape it is in, all in host px: the local px of the box the wheel is
 * drawn in. Every part reads [centre], [shape] and [lift] inside a graphics layer or a draw block,
 * so a carry only redraws. At rest the centre comes from the persisted placement, and a landing
 * ends on the very point the same placement gives, so nothing can jump when it hands over.
 *
 * A docked wheel slides along its edge with the finger, ignoring the way the finger strays from
 * it. Pulled [PULL_OUT] dp into the page it comes off as the circle, hanging from the finger at
 * once, with the offset it had there fading out. Let go, it glides to where it lands.
 */
@Stable
internal class WheelMotion(
    private val scope: CoroutineScope,
    start: WheelPlacement,
    private val onTick: () -> Unit,
    private val onPlace: (WheelPlacement) -> Unit,
) {
    // The persisted placement, mirrored here so a landing is in force the moment it is committed.
    var docked by mutableStateOf(start.docked)
        private set
    var edge by mutableStateOf(start.edge)
        private set
    private var along by mutableFloatStateOf(start.along)
    private var floatX by mutableFloatStateOf(start.floatX)
    private var floatY by mutableFloatStateOf(start.floatY)
    private var density by mutableFloatStateOf(1f)
    private var hostW by mutableFloatStateOf(0f)
    private var hostH by mutableFloatStateOf(0f)

    /** A finger is down on the wheel and moving it. */
    var held = false
        private set

    /** Carried by a finger, or gliding to where it lands. */
    var active by mutableStateOf(false)
        private set

    /** The edge the drop hint is on. It stays after the aim moves off, while the hint fades out. */
    var hintEdge by mutableStateOf<DockEdge?>(null)
        private set

    private var mode by mutableStateOf(Mode.Rest)
    private val morph = Ease(if (start.docked) 0f else 1f)
    private val liftAnim = Ease(0f)
    private val settle = Ease(1f)
    private val glide = Ease(0f)
    private val hint = Ease(0f)

    private var fingerX by mutableFloatStateOf(0f)
    private var fingerY by mutableFloatStateOf(0f)
    private var startX = 0f
    private var startY = 0f
    private var grabX = 0f
    private var grabY = 0f
    private var corrX = 0f
    private var corrY = 0f
    private var alongStart = 0f
    private var fromX = 0f
    private var fromY = 0f
    private var toX = 0f
    private var toY = 0f
    private var target: DockEdge? = null
    private var landJob: Job? = null

    /** How strong the drop hint is, 0 to 1. */
    val hintFade: Float get() = hint.value

    /** 0 for the docked half disc, 1 for the circle, between while one turns into the other. */
    val shape: Float get() = if (mode == Mode.Rest) (if (docked) 0f else 1f) else morph.value

    /** How far the circle is lifted off the page, 0 to 1. */
    val lift: Float get() = liftAnim.value

    /** The wheel's centre in host px. */
    val centre: Offset
        get() = when (mode) {
            Mode.Rest -> spot(docked, edge, along, floatX, floatY)
            Mode.Slide -> spot(true, edge, slideAlong(), floatX, floatY)
            Mode.Free -> {
                val k = 1f - settle.value
                Offset(fingerX - grabX + corrX * k, fingerY - grabY + corrY * k)
            }
            Mode.Land -> {
                val t = glide.value
                Offset(mix(fromX, toX, t), mix(fromY, toY, t))
            }
        }

    /** Where the wheel would dock on [on] if let go now, in host px. */
    fun hintCentre(on: DockEdge): Offset {
        val c = centre
        return spot(true, on, (if (on.vertical) c.y else c.x) / density, floatX, floatY)
    }

    /** Takes the density, the size of the host in px and the placement the screen holds. */
    fun sync(density: Float, hostW: Float, hostH: Float, p: WheelPlacement) {
        this.density = density
        this.hostW = hostW
        this.hostH = hostH
        docked = p.docked
        edge = p.edge
        along = p.along
        floatX = p.floatX
        floatY = p.floatY
    }

    /** Starts a carry with the finger at [finger] in host px. False when there is nothing to carry yet. */
    fun begin(finger: Offset): Boolean {
        if (held || hostW <= 0f || hostH <= 0f) return false
        // A landing in progress is taken over from where it is drawn.
        val c = centre
        val sliding = mode == Mode.Rest && docked
        landJob?.cancel()
        landJob = null
        target = null
        hint.set(0f)
        fingerX = finger.x
        fingerY = finger.y
        startX = finger.x
        startY = finger.y
        grabX = finger.x - c.x
        grabY = finger.y - c.y
        corrX = 0f
        corrY = 0f
        held = true
        active = true
        if (sliding) {
            alongStart = clampAlong(along, lengthOf(edge))
            morph.set(0f)
            liftAnim.set(0f)
            mode = Mode.Slide
        } else {
            if (mode == Mode.Rest) {
                morph.set(1f)
                liftAnim.set(0f)
            }
            settle.set(1f)
            mode = Mode.Free
            morph.to(scope, 1f)
            liftAnim.to(scope, 1f)
        }
        return true
    }

    /** The finger is now at [finger] in host px. */
    fun move(finger: Offset) {
        if (!held) return
        fingerX = finger.x
        fingerY = finger.y
        when (mode) {
            Mode.Slide -> {
                val into = when (edge) {
                    DockEdge.Left -> finger.x - startX
                    DockEdge.Right -> startX - finger.x
                    DockEdge.Top -> finger.y - startY
                    DockEdge.Bottom -> startY - finger.y
                } / density
                if (into >= PULL_OUT) pullOut()
            }
            Mode.Free -> aim(tick = true)
            else -> Unit
        }
    }

    /** The finger lifted, or the touch was taken away: the wheel lands. */
    fun end() {
        if (!held) return
        held = false
        when (mode) {
            Mode.Slide -> {
                // Already on its edge: keep the place it slid to.
                onCommit(WheelPlacement(true, edge, slideAlong(), floatX, floatY))
                mode = Mode.Rest
                active = false
            }
            Mode.Free -> land(centre)
            else -> Unit
        }
    }

    /** Drops any carry at once, for a wheel that was put away. */
    fun reset() {
        landJob?.cancel()
        landJob = null
        held = false
        target = null
        mode = Mode.Rest
        morph.set(if (docked) 0f else 1f)
        liftAnim.set(0f)
        hint.set(0f)
        active = false
    }

    private fun land(c: Offset) {
        val dockOn = target
        val cw = c.x / density
        val ch = c.y / density
        val toEdge: Boolean
        val landAt: Offset
        if (dockOn != null) {
            val spotAlong = clampAlong(if (dockOn.vertical) ch else cw, lengthOf(dockOn))
            onCommit(WheelPlacement(true, dockOn, spotAlong, floatX, floatY))
            landAt = spot(true, dockOn, spotAlong, floatX, floatY)
            toEdge = true
        } else {
            val fx = clampCircleX(cw, hostW / density)
            val fy = clampCircleY(ch, hostH / density)
            onCommit(WheelPlacement(false, edge, along, fx, fy))
            landAt = spot(false, edge, along, fx, fy)
            toEdge = false
        }
        target = null
        fromX = c.x
        fromY = c.y
        toX = landAt.x
        toY = landAt.y
        glide.set(0f)
        mode = Mode.Land
        morph.to(scope, if (toEdge) 0f else 1f)
        liftAnim.to(scope, 0f)
        hint.to(scope, 0f, HINT_FADE)
        landJob = scope.launch {
            glide.animateTo(1f)
            mode = Mode.Rest
            morph.set(if (docked) 0f else 1f)
            liftAnim.set(0f)
            active = false
        }
    }

    private fun pullOut() {
        // The circle takes the grab the finger had on the disc, at most half a radius from its centre.
        val c = centre
        var gx = fingerX - c.x
        var gy = fingerY - c.y
        val cap = 0.5f * POPUP_R * density
        val len = hypot(gx, gy)
        if (len > cap) {
            gx *= cap / len
            gy *= cap / len
        }
        grabX = gx
        grabY = gy
        corrX = c.x - (fingerX - gx)
        corrY = c.y - (fingerY - gy)
        settle.set(0f)
        settle.to(scope, 1f)
        morph.to(scope, 1f)
        liftAnim.to(scope, 1f)
        mode = Mode.Free
        onTick()
        aim(tick = false)
    }

    /** Finds the edge the wheel would dock on at its centre now, and shows or drops the hint when it changes. */
    private fun aim(tick: Boolean) {
        val c = centre
        val near = dockTarget(c.x / density, c.y / density, hostW / density, hostH / density)
        if (near == target) return
        target = near
        if (near != null) {
            hintEdge = near
            hint.set(0f)
            hint.to(scope, 1f, HINT_FADE)
        } else {
            hint.to(scope, 0f, HINT_FADE)
        }
        if (tick) onTick()
    }

    private fun onCommit(p: WheelPlacement) {
        docked = p.docked
        edge = p.edge
        along = p.along
        floatX = p.floatX
        floatY = p.floatY
        onPlace(p)
    }

    private fun lengthOf(on: DockEdge): Float = (if (on.vertical) hostH else hostW) / density

    private fun slideAlong(): Float {
        val moved = (if (edge.vertical) fingerY - startY else fingerX - startX) / density
        return clampAlong(alongStart + moved, lengthOf(edge))
    }

    /** The centre in host px of a wheel docked on [on] [at] dp along it, or of the circle at ([fx], [fy]) dp. */
    private fun spot(dockedOn: Boolean, on: DockEdge, at: Float, fx: Float, fy: Float): Offset {
        val w = hostW / density
        val h = hostH / density
        return if (dockedOn) {
            edgePoint(on, clampAlong(at, lengthOf(on)), w, h) * density
        } else {
            Offset(clampCircleX(fx, w), clampCircleY(fy, h)) * density
        }
    }
}
