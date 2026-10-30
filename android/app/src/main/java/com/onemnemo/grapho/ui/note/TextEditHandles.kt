package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
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

private val GRIP_HIT = DpSize(72.dp, TEXT_HANDLE_SLOT)
private val GRIP_MARK = DpSize(32.dp, 6.dp)
private val SIDE_MARK = DpSize(6.dp, 28.dp)

/**
 * The handles of the open text box, to be placed in a [Box] that is [TEXT_HANDLE_SLOT] larger than the box on every
 * side: a grip above it to move it, and a handle on each side to make it wider or narrower. Drags report in the
 * box's own pixels, which [scale] turns into page-local units; [onMove] gets the drag across and down and
 * [onStretch] the drag along the box, with `true` for the left side.
 */
@Composable
internal fun BoxScope.TextHandles(
    scale: Float,
    onMove: (dx: Float, dy: Float) -> Unit,
    onStretch: (fromLeft: Boolean, delta: Float) -> Unit,
) {
    val move by rememberUpdatedState(onMove)
    val stretch by rememberUpdatedState(onStretch)
    val units by rememberUpdatedState(scale)
    val moveLabel = stringResource(R.string.text_move_handle)
    val sizeLabel = stringResource(R.string.text_resize_handle)
    Handle(
        Modifier.align(Alignment.TopCenter),
        GRIP_HIT,
        GRIP_MARK,
        Alignment.Center,
        moveLabel,
    ) { move(it.x / units, it.y / units) }
    Handle(
        Modifier.align(Alignment.CenterStart),
        DpSize(TEXT_HANDLE_SLOT, TEXT_HANDLE_SLOT),
        SIDE_MARK,
        Alignment.CenterEnd,
        sizeLabel,
    ) { stretch(true, it.x / units) }
    Handle(
        Modifier.align(Alignment.CenterEnd),
        DpSize(TEXT_HANDLE_SLOT, TEXT_HANDLE_SLOT),
        SIDE_MARK,
        Alignment.CenterStart,
        sizeLabel,
    ) { stretch(false, it.x / units) }
}

/** A touch area of size [hit] showing a small pill of size [mark] at [markAt], which reports each drag step. */
@Composable
private fun Handle(
    modifier: Modifier,
    hit: DpSize,
    mark: DpSize,
    markAt: Alignment,
    label: String,
    onDrag: (Offset) -> Unit,
) {
    val drag by rememberUpdatedState(onDrag)
    Box(
        modifier = modifier
            .size(hit)
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    drag(amount)
                }
            }
            .semantics { contentDescription = label },
        contentAlignment = markAt,
    ) {
        Box(Modifier.size(mark).clip(GraphoShapes.pill).background(Grapho.colors.selectionIndicator))
    }
}
