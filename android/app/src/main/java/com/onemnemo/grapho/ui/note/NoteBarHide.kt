package com.onemnemo.grapho.ui.note

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.components.motionEnterEasing
import com.onemnemo.grapho.ui.components.motionTween
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

/** How long a bar that was called back stays once nobody is using it. */
internal const val BAR_LINGER_MS = 4000L

/** How long after the last stroke a bar that stepped away while writing comes back. */
internal const val BAR_RETURN_MS = 600L

/** How long the handle stays faintly visible after the bar leaves, so the way back can be learned. */
private const val HANDLE_SHOWN_MS = 2500L

private const val HANDLE_SHOWN_ALPHA = 0.4f

/** The handle never vanishes completely, so the hidden bar can always be found again. */
private const val HANDLE_REST_ALPHA = 0.15f

/** A drag from the handle this far toward the page calls the bar back. */
private val HANDLE_SWIPE = GraphoSpace.spaceLg

/** What the screen knows about whether the bar is away: the pen, the calls back from the edge and what holds it open. */
@Stable
internal class BarVisibility {
    /** The pen or a finger is down on the page, as the canvas reports it. */
    var penDown by mutableStateOf(false)

    /** A bar that is always hidden was called back from its edge. */
    var summoned by mutableStateOf(false)
        private set

    /** Counts touches on the bar, so the time it lingers starts over while it is in use. */
    var touches by mutableIntStateOf(0)
        private set

    /** The bar's More menu, or a popover from it, is open. */
    var menuOpen by mutableStateOf(false)

    fun summon() {
        summoned = true
        touches++
    }

    fun touched() {
        touches++
    }

    internal fun put() {
        summoned = false
    }
}

/**
 * Whether the note bar is away now. A bar that hides while writing comes back [BAR_RETURN_MS] after the last
 * stroke. A bar that is always hidden goes [BAR_LINGER_MS] after it was last used, never while [held] by an open
 * menu or panel. Reading keeps it.
 */
@Composable
internal fun rememberBarHidden(v: BarVisibility, autoHide: AutoHide, readMode: Boolean, held: Boolean): Boolean {
    var writing by remember { mutableStateOf(false) }
    LaunchedEffect(v.penDown, autoHide) {
        when {
            autoHide != AutoHide.WhileWriting -> writing = false

            v.penDown -> writing = true

            else -> {
                delay(BAR_RETURN_MS)
                writing = false
            }
        }
    }
    LaunchedEffect(v.summoned, v.touches, held, autoHide) {
        if (autoHide != AutoHide.Always) {
            v.put()
        } else if (v.summoned && !held) {
            delay(BAR_LINGER_MS)
            v.put()
        }
    }
    return barHidden(autoHide, readMode, writing, v.summoned)
}

/**
 * Slides [content], the note bar, away toward its edge by [travel] and fades it when [hidden], and back with the
 * same motion. Under reduced motion it just goes. Once it is out of sight it leaves the composition, so it takes
 * no touches. The page never moves.
 */
@Composable
internal fun BarSlide(
    hidden: Boolean,
    position: BarPosition,
    travel: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val away by animateFloatAsState(
        targetValue = if (hidden) 1f else 0f,
        animationSpec = if (hidden) {
            motionTween(GraphoMotion.motionDurationChromeRecedeMs, GraphoMotion.motionEasingExit)
        } else {
            motionTween(GraphoMotion.motionDurationChromeReturnMs, motionEnterEasing())
        },
        label = "noteBarAway",
    )
    val gone by remember { derivedStateOf { away >= 1f } }
    if (hidden && gone) return
    Box(
        modifier.graphicsLayer {
            val distance = travel.toPx() * away
            when (position) {
                BarPosition.Top -> translationY = -distance
                BarPosition.Bottom -> translationY = distance
                BarPosition.Left -> translationX = -distance
                BarPosition.Right -> translationX = distance
            }
            alpha = 1f - away
        },
    ) { content() }
}

/**
 * The way back for a bar that is always hidden: a small bar on the edge it left toward, faint for a moment so it
 * can be learned and then unseen, and still there to tap or to drag toward the page ([onSummon]). It is only as
 * large as its own hit area, so the page and the pen wheel's tab beside it keep their touches.
 */
@Composable
internal fun NoteBarHandle(position: BarPosition, shown: Boolean, onSummon: () -> Unit, modifier: Modifier = Modifier) {
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(shown) {
        seen = shown
        if (shown) {
            delay(HANDLE_SHOWN_MS)
            seen = false
        }
    }
    val faint by animateFloatAsState(
        targetValue = if (seen) HANDLE_SHOWN_ALPHA else HANDLE_REST_ALPHA,
        animationSpec = motionTween(GraphoMotion.motionDurationSlowMs),
        label = "noteBarHandle",
    )
    val reach = GraphoSpace.space5xl
    val depth = GraphoSpace.space2xl
    val long = GraphoSpace.space3xl + GraphoSpace.spaceSm
    val short = GraphoSpace.sizeTrack
    val vertical = position.vertical
    Box(
        modifier = modifier
            .then(if (vertical) Modifier.width(depth).height(reach) else Modifier.width(reach).height(depth))
            .pointerInput(position) { detectTapGestures { onSummon() } }
            .pointerInput(position) {
                var pulled = 0f
                val threshold = HANDLE_SWIPE.toPx()
                detectDragGestures(
                    onDragStart = { pulled = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        pulled += when (position) {
                            BarPosition.Top -> drag.y
                            BarPosition.Bottom -> -drag.y
                            BarPosition.Left -> drag.x
                            BarPosition.Right -> -drag.x
                        }
                        if (pulled >= threshold) {
                            pulled = Float.NEGATIVE_INFINITY
                            onSummon()
                        }
                    },
                )
            },
        contentAlignment = position.alignment(),
    ) {
        Box(
            Modifier
                .padding(GraphoSpace.spaceSm)
                .then(if (vertical) Modifier.size(short, long) else Modifier.size(long, short))
                .alpha(faint)
                .background(Grapho.colors.textDefault, GraphoShapes.pill),
        )
    }
}
