package com.onemnemo.grapho.ui.note

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/** The canvas edge the wheel can dock against. */
enum class DockEdge { Left, Top, Right, Bottom }

internal val DockEdge.vertical: Boolean get() = this == DockEdge.Left || this == DockEdge.Right

/** Screen angle the docked wheel faces, into the page (clockwise from the right, y down). */
internal val DockEdge.face: Float
    get() = when (this) {
        DockEdge.Left -> 0f
        DockEdge.Top -> 90f
        DockEdge.Right -> 180f
        DockEdge.Bottom -> -90f
    }

/**
 * +1 or -1. The wheel is laid out as if docked on the left; the other edges
 * turn that layout to face the page and mirror it where needed, so the size
 * button always ends up on the top or left end of the arc.
 */
internal val DockEdge.mirror: Float
    get() = when (this) {
        DockEdge.Left, DockEdge.Bottom -> 1f
        DockEdge.Top, DockEdge.Right -> -1f
    }

/** Screen angle of [degrees] in the left-docked layout. */
internal fun DockEdge.toScreen(degrees: Float): Float = normDeg(face + mirror * degrees)

/** Left-docked layout angle of the screen angle [degrees]. */
internal fun DockEdge.fromScreen(degrees: Float): Float = normDeg(mirror * (degrees - face))

/**
 * Size in dp of the docked wheel's window: the half disc plus [DOCK_TOP] of
 * room for the size bubble, above it on the side edges and left of it on the
 * top and bottom.
 */
internal fun dockedSize(edge: DockEdge): Size =
    if (edge.vertical) Size(DISC_R, 2f * DISC_R + DOCK_TOP) else Size(2f * DISC_R + DOCK_TOP, DISC_R)

/** Centre of the disc in dp inside its docked window. It sits on the window's edge that touches the canvas edge. */
internal fun dockedCentre(edge: DockEdge): Offset = when (edge) {
    DockEdge.Left -> Offset(0f, DOCK_TOP + DISC_R)
    DockEdge.Right -> Offset(DISC_R, DOCK_TOP + DISC_R)
    DockEdge.Top -> Offset(DOCK_TOP + DISC_R, 0f)
    DockEdge.Bottom -> Offset(DOCK_TOP + DISC_R, DISC_R)
}

/** Where the disc centre may sit along an edge of length [len] so the whole docked window stays on screen, when it fits. */
internal fun clampAlong(along: Float, len: Float): Float =
    if (len >= DISC_CY + DISC_R) along.coerceIn(DISC_CY, len - DISC_R) else len / 2f

internal fun clampCircleX(x: Float, hostW: Float): Float =
    x.coerceIn(minOf(POPUP_R, hostW / 2f), maxOf(POPUP_R, hostW - POPUP_R))

internal fun clampCircleY(y: Float, hostH: Float): Float =
    y.coerceIn(minOf(POPUP_R + DOCK_TOP, hostH / 2f), maxOf(POPUP_R + DOCK_TOP, hostH - POPUP_R))

/** A carried wheel targets an edge when its centre is within this many dp of it. */
private const val DOCK_GAP = 110f

private fun gapTo(edge: DockEdge, x: Float, y: Float, hostW: Float, hostH: Float): Float = when (edge) {
    DockEdge.Left -> x
    DockEdge.Top -> y
    DockEdge.Right -> hostW - x
    DockEdge.Bottom -> hostH - y
}

/** The edge a wheel carried to centre ([x], [y]) would dock on, or null when it is too far from all of them. */
internal fun dockTarget(x: Float, y: Float, hostW: Float, hostH: Float): DockEdge? {
    val near = DockEdge.values().minBy { gapTo(it, x, y, hostW, hostH) }
    return if (gapTo(near, x, y, hostW, hostH) < DOCK_GAP) near else null
}

/** The point on [edge], [along] dp along it, where a docked disc has its centre. */
internal fun edgePoint(edge: DockEdge, along: Float, hostW: Float, hostH: Float): Offset = when (edge) {
    DockEdge.Left -> Offset(0f, along)
    DockEdge.Right -> Offset(hostW, along)
    DockEdge.Top -> Offset(along, 0f)
    DockEdge.Bottom -> Offset(along, hostH)
}
