package com.onemnemo.grapho.ui.note

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A value that turns like a dial, in degrees: dragged 1:1, then released with
 * the finger's speed so it glides and settles on a multiple of [step]. Between
 * [min] and [max], which are multiples of [step] too.
 */
internal class SpinState(
    var step: Float,
    private val min: Float = Float.NEGATIVE_INFINITY,
    private val max: Float = Float.POSITIVE_INFINITY,
    start: Float = 0f,
) {
    var offset by mutableFloatStateOf(start)
        private set
    private var job: Job? = null
    private var slot = (start / step).roundToInt()

    /** Called each time the dial passes a slot, by the finger or by a glide. */
    var onDetent: (() -> Unit)? = null

    private fun turnTo(value: Float) {
        offset = value
        val now = (value / step).roundToInt()
        if (now != slot) {
            slot = now
            onDetent?.invoke()
        }
    }

    /** True while a glide is running. */
    val moving: Boolean get() = job?.isActive == true

    /** Stops a glide where it is. */
    fun grab() {
        job?.cancel()
        job = null
    }

    fun dragBy(delta: Float) {
        turnTo((offset + delta).coerceIn(min, max))
    }

    fun snapTo(value: Float) {
        grab()
        offset = value.coerceIn(min, max)
        slot = (offset / step).roundToInt()
    }

    /** Lets go at [velocity] (degrees per second): glide on, then settle on a slot. */
    fun release(scope: CoroutineScope, velocity: Float) {
        val from = offset
        var target = ((from + velocity * GLIDE_S) / step).roundToInt() * step
        // A flick never ends up behind where it started.
        if (velocity > MIN_FLICK && target < from) target = kotlin.math.ceil(from / step) * step
        if (velocity < -MIN_FLICK && target > from) target = kotlin.math.floor(from / step) * step
        glide(scope, from, target.coerceIn(min, max), velocity)
    }

    /** Glides from where it is to [target] degrees, as a release would settle. */
    fun glideTo(scope: CoroutineScope, target: Float) {
        glide(scope, offset, target.coerceIn(min, max), 0f)
    }

    private fun glide(scope: CoroutineScope, from: Float, target: Float, velocity: Float) {
        job?.cancel()
        job = scope.launch {
            Animatable(from).animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessLow),
                initialVelocity = velocity,
            ) {
                turnTo(this.value.coerceIn(min, max))
            }
        }
    }

    private companion object {
        /** How far a release carries, in seconds of the release speed. */
        const val GLIDE_S = 0.3f
        const val MIN_FLICK = 60f
    }
}
