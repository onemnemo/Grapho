package com.onemnemo.grapho.ui.components

import androidx.compose.runtime.compositionLocalOf

/** A popup's top-left corner in window pixels. */
data class PopupSpot(val x: Int, val y: Int)

/**
 * Which side of its anchor a popup opens on. [BelowStart] lines up the start edges and runs right. [BelowEnd] and
 * [AboveEnd] line up the end edges and run left, the way the note bar's panels open from the top or bottom edge.
 * [RightOf] and [LeftOf] sit beside the anchor with their tops level, for a bar down the left or right edge.
 */
enum class PopupSide { BelowStart, BelowEnd, AboveEnd, RightOf, LeftOf }

/** The side menus and popovers open on from the triggers under it. The note bar sets it from the edge it is on. */
val LocalPopupSide = compositionLocalOf { PopupSide.BelowStart }

/**
 * Places a popup on [side] of its anchor, flips it to the opposite side when it would run off the window and
 * there is room there, and keeps it [margin] pixels inside the window either way.
 */
fun anchoredPopupSpot(
    anchorLeft: Int,
    anchorTop: Int,
    anchorBottom: Int,
    windowWidth: Int,
    windowHeight: Int,
    contentWidth: Int,
    contentHeight: Int,
    gap: Int,
    margin: Int,
    anchorRight: Int = anchorLeft,
    side: PopupSide = PopupSide.BelowStart,
): PopupSpot {
    val maxX = maxOf(margin, windowWidth - contentWidth - margin)
    val maxY = maxOf(margin, windowHeight - contentHeight - margin)
    if (side == PopupSide.RightOf || side == PopupSide.LeftOf) {
        val right = anchorRight + gap
        val left = anchorLeft - gap - contentWidth
        val fitsRight = right + contentWidth <= windowWidth - margin
        val fitsLeft = left >= margin
        val x = when (side) {
            PopupSide.RightOf -> if (fitsRight || !fitsLeft) right else left
            else -> if (fitsLeft || !fitsRight) left else right
        }
        return PopupSpot(x.coerceIn(margin, maxX), anchorTop.coerceIn(margin, maxY))
    }
    val x = if (side == PopupSide.BelowStart) anchorLeft else anchorRight - contentWidth
    val below = anchorBottom + gap
    val above = anchorTop - gap - contentHeight
    val fitsBelow = below + contentHeight <= windowHeight - margin
    val fitsAbove = above >= margin
    val y = if (side == PopupSide.AboveEnd) {
        if (fitsAbove || !fitsBelow) above else below
    } else {
        if (fitsBelow || !fitsAbove) below else above
    }
    return PopupSpot(x.coerceIn(margin, maxX), y.coerceIn(margin, maxY))
}
