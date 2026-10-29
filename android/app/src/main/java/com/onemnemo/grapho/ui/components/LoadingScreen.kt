package com.onemnemo.grapho.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

/** Where the loading screen is in its short life. */
enum class SplashPhase { Hidden, Showing, Cheering }

private const val SHOW_AFTER_MS = 250L
private const val MIN_SHOWN_MS = 700L
private const val CHEER_MS = 700L

/**
 * Decides when the loading screen shows. A load that finishes within [showAfterMs] never shows it. Once shown
 * it stays at least [MIN_SHOWN_MS], then ends with a short cheer.
 */
@Composable
fun rememberSplashPhase(loading: Boolean, showAfterMs: Long = SHOW_AFTER_MS): SplashPhase {
    var phase by remember { mutableStateOf(SplashPhase.Hidden) }
    var shownAt by remember { mutableLongStateOf(0L) }
    val latest by rememberUpdatedState(loading)
    LaunchedEffect(loading) {
        if (loading) {
            if (phase == SplashPhase.Hidden) {
                delay(showAfterMs)
                if (latest) {
                    shownAt = System.currentTimeMillis()
                    phase = SplashPhase.Showing
                }
            }
        } else if (phase == SplashPhase.Showing) {
            val left = MIN_SHOWN_MS - (System.currentTimeMillis() - shownAt)
            if (left > 0) delay(left)
            phase = SplashPhase.Cheering
            delay(CHEER_MS)
            phase = SplashPhase.Hidden
        }
    }
    return phase
}

private val WALK = listOf(
    R.drawable.mascot_walk_0 to 115L,
    R.drawable.mascot_walk_1 to 115L,
    R.drawable.mascot_walk_2 to 115L,
    R.drawable.mascot_walk_3 to 115L,
)
private val CHEER = listOf(
    R.drawable.mascot_cheer_0 to 130L,
    R.drawable.mascot_cheer_1 to 80L,
    R.drawable.mascot_cheer_2 to 120L,
    R.drawable.mascot_cheer_3 to 120L,
    R.drawable.mascot_cheer_4 to 80L,
    R.drawable.mascot_cheer_5 to 100L,
    R.drawable.mascot_cheer_6 to 170L,
)

/**
 * Full screen loading screen: the mascot walking on the spot, an optional name and kind below it, and a thin line that
 * sweeps while the work runs. It takes every touch so nothing underneath reacts. [cheering] swaps the walk for the
 * cheer when the work is done.
 */
@Composable
fun LoadingScreen(cheering: Boolean, modifier: Modifier = Modifier, title: String? = null, kind: String? = null) {
    val c = Grapho.colors
    val still = remember { !ValueAnimator.areAnimatorsEnabled() }
    Box(
        modifier
            .fillMaxSize()
            .background(c.surfaceBase)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = GraphoSpace.space2xl),
        ) {
            Mascot(cheering = cheering, still = still)
            Spacer(Modifier.height(GraphoSpace.space2xl))
            if (title != null) {
                Text(
                    title,
                    style = Grapho.type.heading,
                    color = c.textDefault,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            if (kind != null) {
                Text(kind, style = Grapho.type.caption, color = c.textMuted, textAlign = TextAlign.Center)
            }
            if (title != null || kind != null) Spacer(Modifier.height(GraphoSpace.spaceLg))
            SweepLine(still = still)
        }
    }
}

/** Each sprite pixel is 10 dp, so the 13 by 14 sprite is 130 by 140 dp and stays crisp. */
@Composable
private fun Mascot(cheering: Boolean, still: Boolean) {
    val frames = if (cheering) CHEER else WALK
    val bitmaps = frames.map { ImageBitmap.imageResource(it.first) }
    val stand = ImageBitmap.imageResource(R.drawable.mascot_stand)
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(cheering, still) {
        index = 0
        if (still) return@LaunchedEffect
        while (true) {
            delay(frames[index].second)
            index = (index + 1) % frames.size
        }
    }
    Image(
        bitmap = if (still) stand else bitmaps[index % bitmaps.size],
        contentDescription = null,
        modifier = Modifier.size(130.dp, 140.dp),
        filterQuality = FilterQuality.None,
        contentScale = ContentScale.FillBounds,
    )
}

private val Ribbon = listOf(Color(0xFFD9487A), Color(0xFFE0402E), Color(0xFFF28A2E))

@Composable
private fun SweepLine(still: Boolean) {
    val c = Grapho.colors
    val t = rememberInfiniteTransition(label = "sweep")
    val at by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweepAt",
    )
    Box(
        Modifier
            .width(160.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(c.surfaceSunken),
    ) {
        if (still) {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(Ribbon)))
        } else {
            Box(
                Modifier
                    .offset(x = (96f * at).dp)
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(Ribbon)),
            )
        }
    }
}
