package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.GraphoWheel

internal const val TAB_W = 28f
internal const val TAB_H = 72f

/**
 * The closed tab on [edge], [centre] dp along it, its rounded side facing the page. It sits in the
 * host at its origin and is placed by layer translation, so its drag can follow the finger in host px.
 */
@Composable
internal fun WheelTab(
    edge: DockEdge,
    hostW: Float,
    hostH: Float,
    centre: () -> Float,
    color: Color,
    progress: Float,
    host: Array<LayoutCoordinates?>,
    onOpen: () -> Unit,
    onDragStart: () -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val r = TAB_W.dp
    val description = stringResource(R.string.wheel_open)
    val shape = when (edge) {
        DockEdge.Left -> AbsoluteRoundedCornerShape(topLeft = 0.dp, topRight = r, bottomRight = r, bottomLeft = 0.dp)
        DockEdge.Right -> AbsoluteRoundedCornerShape(topLeft = r, topRight = 0.dp, bottomRight = 0.dp, bottomLeft = r)
        DockEdge.Top -> AbsoluteRoundedCornerShape(topLeft = 0.dp, topRight = 0.dp, bottomRight = r, bottomLeft = r)
        DockEdge.Bottom -> AbsoluteRoundedCornerShape(topLeft = r, topRight = r, bottomRight = 0.dp, bottomLeft = 0.dp)
    }
    val w = if (edge.vertical) TAB_W else TAB_H
    val h = if (edge.vertical) TAB_H else TAB_W
    val origin = when (edge) {
        DockEdge.Left -> TransformOrigin(0f, 0.5f)
        DockEdge.Right -> TransformOrigin(1f, 0.5f)
        DockEdge.Top -> TransformOrigin(0.5f, 0f)
        DockEdge.Bottom -> TransformOrigin(0.5f, 1f)
    }
    val density = LocalDensity.current.density
    val latestOpen by rememberUpdatedState(onOpen)
    Box(
        modifier = Modifier
            .graphicsLayer {
                val c = centre()
                val x = when (edge) {
                    DockEdge.Left -> 0f
                    DockEdge.Right -> hostW - TAB_W
                    else -> c - TAB_H / 2f
                }
                val y = when (edge) {
                    DockEdge.Top -> 0f
                    DockEdge.Bottom -> hostH - TAB_W
                    else -> c - TAB_H / 2f
                }
                translationX = x * density
                translationY = y * density
            }
            .popIn(progress, 0.6f, origin)
            .size(width = w.dp, height = h.dp)
            .clip(shape)
            .background(GraphoWheel.body)
            .semantics { contentDescription = description }
            .pointerInput(Unit) { detectTapGestures { latestOpen() } }
            .hostDrag(host = host, onStart = onDragStart, onMove = onDragBy, onEnd = onDragEnd),
        contentAlignment = Alignment.Center,
    ) {
        // The dot sits a little toward the page, off the rounded side's edge.
        val pad = when (edge) {
            DockEdge.Left -> Modifier.padding(end = 4.dp)
            DockEdge.Right -> Modifier.padding(start = 4.dp)
            DockEdge.Top -> Modifier.padding(bottom = 4.dp)
            DockEdge.Bottom -> Modifier.padding(top = 4.dp)
        }
        Box(
            pad
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
