package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.StrokeId
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.components.motionCalm
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoContent
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

/** The glow around the scan line is this many times as thick as the line, at this strength. */
private const val GLOW_SCALE = 6f
private const val GLOW_ALPHA = 0.25f

/** Under reduced motion the line rests across the middle at this strength. */
private const val STILL_LINE_ALPHA = 0.6f

/**
 * Follows the picture [id] on [page] in view pixels, a frame at a time, so what is drawn over it keeps up with
 * scrolling and zooming. The value is null while the picture is not on the page, unless [keepLast] holds the last
 * place it was seen.
 */
@Composable
internal fun rememberImageRect(
    view: () -> NoteCanvasView?,
    page: Int,
    id: StrokeId,
    keepLast: Boolean = false,
): State<RectF?> {
    val rect = remember(page, id) { mutableStateOf<RectF?>(null) }
    LaunchedEffect(page, id) {
        val probe = RectF()
        while (true) {
            withFrameNanos { }
            val found = view()?.imageViewRect(page, id, probe) == true
            val now = rect.value
            if (!found) {
                if (now != null && !keepLast) rect.value = null
            } else if (now != probe) {
                rect.value = RectF(probe)
            }
        }
    }
    return rect
}

/**
 * A picture's working state: a veil over the picture, one line that sweeps it, and a pill under it with the job's
 * own icon, its name and Cancel. Touches reach the page through the veil, so the note stays usable; the picture
 * itself is held by the canvas. [insets] keep the pill clear of a floating note bar.
 */
@Composable
internal fun PictureWorking(job: PictureJob, rect: State<RectF?>, insets: EdgeInsets, onCancel: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        PictureVeil(job.kind, rect)
        PictureStatusPill(job, rect, insets, onCancel)
    }
}

@Composable
private fun PictureVeil(kind: PictureJobKind, rect: State<RectF?>) {
    val color = Grapho.colors.textDefault
    val border = Grapho.colors.borderSubtle
    val sweep = if (motionCalm()) {
        null
    } else {
        rememberInfiniteTransition(label = "scan").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ScanSweep.PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "scan",
        )
    }
    val line = with(LocalDensity.current) { GraphoSpace.borderWidthEmphasis.toPx() }
    Box(
        Modifier
            .pinnedTo(rect)
            .clip(GraphoShapes.key)
            .border(GraphoSpace.borderWidthHairline, border, GraphoShapes.key)
            .drawBehind {
                drawRect(GraphoContent.mediaVeil)
                val across = kind == PictureJobKind.ConvertToInk
                val t = sweep?.value
                val extent = if (across) size.width else size.height
                val at = if (t == null) (extent - line) / 2f else ScanSweep.offset(t, extent, line)
                val shown = if (t == null) STILL_LINE_ALPHA else ScanSweep.alpha(t)
                val glow = line * GLOW_SCALE
                val glowAt = at - (glow - line) / 2f
                if (across) {
                    drawRect(color.copy(alpha = GLOW_ALPHA * shown), Offset(glowAt, 0f), Size(glow, size.height))
                    drawRect(color.copy(alpha = shown), Offset(at, 0f), Size(line, size.height))
                } else {
                    drawRect(color.copy(alpha = GLOW_ALPHA * shown), Offset(0f, glowAt), Size(size.width, glow))
                    drawRect(color.copy(alpha = shown), Offset(0f, at), Size(size.width, line))
                }
            },
    )
}

@Composable
private fun PictureStatusPill(job: PictureJob, rect: State<RectF?>, insets: EdgeInsets, onCancel: () -> Unit) {
    val density = LocalDensity.current
    val gap = with(density) { GraphoSpace.spaceMd.toPx() }
    val margins = with(density) {
        EdgeInsets(insets.left.dp.toPx(), insets.top.dp.toPx(), insets.right.dp.toPx(), insets.bottom.dp.toPx())
    }
    val icon = if (job.kind == PictureJobKind.RemoveBackground) R.drawable.ic_cutout else R.drawable.ic_to_ink
    Row(
        Modifier
            .placedUnder(rect, gap, margins)
            .floatingCard(GraphoShapes.pill)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIcon(icon, null, Grapho.colors.textMuted)
        Text(
            stringResource(job.label),
            style = Grapho.type.label,
            color = Grapho.colors.textDefault,
            modifier = Modifier.padding(start = GraphoSpace.spaceSm, end = GraphoSpace.spaceXs),
        )
        GraphoButton(
            stringResource(R.string.action_cancel),
            onClick = onCancel,
            kind = ButtonKind.Quiet,
            size = ButtonSize.Small,
        )
    }
}

/** Sizes the box to the picture's rectangle and puts it there, a frame at a time, without recomposing. */
internal fun Modifier.pinnedTo(rect: State<RectF?>): Modifier = layout { measurable, constraints ->
    val r = rect.value
    val w = r?.width()?.roundToInt()?.coerceAtLeast(0) ?: 0
    val h = r?.height()?.roundToInt()?.coerceAtLeast(0) ?: 0
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) {
        if (r != null) placeable.place(r.left.roundToInt(), r.top.roundToInt())
    }
}

/** Puts the box under the picture's rectangle as [PillPlacement.below] says, a frame at a time. */
internal fun Modifier.placedUnder(rect: State<RectF?>, gap: Float, margins: EdgeInsets): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val r = rect.value
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (r != null) {
                val spot = PillPlacement.below(
                    r.left,
                    r.right,
                    r.bottom,
                    gap,
                    placeable.width.toFloat(),
                    placeable.height.toFloat(),
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat(),
                    margins,
                )
                placeable.place(spot.x.roundToInt(), spot.y.roundToInt())
            }
        }
    }
