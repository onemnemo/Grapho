package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes

/** The strip round the open text box that holds its handles. It is also the least any handle measures to touch. */
internal val TEXT_HANDLE_SLOT: Dp = 40.dp

private val GRIP_MARK = DpSize(32.dp, 6.dp)
private val SIDE_MARK = DpSize(6.dp, 28.dp)
private val CORNER_MARK = DpSize(14.dp, 14.dp)

/**
 * The handles of the open text box, to be placed in a [Box] that is [TEXT_HANDLE_SLOT] larger than the box on every
 * side. The strip above and below the box moves it, the strips on its left and right sides make it wider or narrower,
 * and the four corners scale the box and its text together. A finger dragged across the text moves the box too, see
 * `TextBoxInput`. Drags report in the box's own pixels, which [scale] turns into page-local units; [onMove] gets the
 * drag across and down, [onStretch] the drag along the box, with `true` for the left side, and [onScale] the drag at a
 * corner, with `true` for a left corner and `true` for a top one.
 */
@Composable
internal fun BoxScope.TextHandles(
    scale: () -> Float,
    onMove: (dx: Float, dy: Float) -> Unit,
    onStretch: (fromLeft: Boolean, delta: Float) -> Unit,
    onScale: (fromLeft: Boolean, fromTop: Boolean, dx: Float, dy: Float) -> Unit,
) {
    val move by rememberUpdatedState(onMove)
    val stretch by rememberUpdatedState(onStretch)
    val resize by rememberUpdatedState(onScale)
    val units by rememberUpdatedState(scale)
    val moveLabel = stringResource(R.string.text_move_handle)
    val sizeLabel = stringResource(R.string.text_resize_handle)
    val scaleLabel = stringResource(R.string.text_scale_handle)
    val slot = TEXT_HANDLE_SLOT
    for (top in listOf(true, false)) {
        Handle(
            Modifier.align(if (top) Alignment.TopCenter else Alignment.BottomCenter).fillMaxWidth().height(slot),
            GRIP_MARK,
            Alignment.Center,
            moveLabel,
        ) { move(it.x / units(), it.y / units()) }
    }
    for (left in listOf(true, false)) {
        Handle(
            Modifier.align(if (left) Alignment.CenterStart else Alignment.CenterEnd).fillMaxHeight().width(slot),
            SIDE_MARK,
            if (left) Alignment.CenterEnd else Alignment.CenterStart,
            sizeLabel,
        ) { stretch(left, it.x / units()) }
    }
    for (left in listOf(true, false)) {
        for (top in listOf(true, false)) {
            val at = when {
                left && top -> Alignment.TopStart
                top -> Alignment.TopEnd
                left -> Alignment.BottomStart
                else -> Alignment.BottomEnd
            }
            val mark = when {
                left && top -> Alignment.BottomEnd
                top -> Alignment.BottomStart
                left -> Alignment.TopEnd
                else -> Alignment.TopStart
            }
            Handle(Modifier.align(at).size(slot), CORNER_MARK, mark, scaleLabel, round = true) {
                resize(left, top, it.x / units(), it.y / units())
            }
        }
    }
}

/** A touch area showing a small pill of size [mark] at [markAt], which reports each drag step. */
@Composable
private fun Handle(
    modifier: Modifier,
    mark: DpSize,
    markAt: Alignment,
    label: String,
    round: Boolean = false,
    onDrag: (Offset) -> Unit,
) {
    val drag by rememberUpdatedState(onDrag)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    drag(amount)
                }
            }
            .semantics { contentDescription = label },
        contentAlignment = markAt,
    ) {
        Box(
            Modifier
                .size(mark)
                .clip(if (round) CircleShape else GraphoShapes.pill)
                .background(Grapho.colors.selectionIndicator),
        )
    }
}
