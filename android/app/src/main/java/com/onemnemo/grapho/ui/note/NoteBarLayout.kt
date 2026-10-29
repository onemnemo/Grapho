package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.ui.components.COMPACT_WIDTH_DP
import com.onemnemo.grapho.ui.components.PopupSide
import com.onemnemo.grapho.ui.components.TooltipSide

/** The note bar floats over the page as a pill, or runs the full width as a flat strip that the page sits under. */
enum class NoteBarStyle { Floating, Line }

/** The edge of the window the note bar sits against. */
enum class BarPosition { Top, Bottom, Left, Right }

/** True for a bar that runs down the left or right edge. */
val BarPosition.vertical: Boolean get() = this == BarPosition.Left || this == BarPosition.Right

/** The side a tip opens on: away from the edge the bar sits against, so the tip never runs off the window. */
val BarPosition.tipSide: TooltipSide
    get() = when (this) {
        BarPosition.Top -> TooltipSide.Below
        BarPosition.Bottom -> TooltipSide.Above
        BarPosition.Left -> TooltipSide.End
        BarPosition.Right -> TooltipSide.Start
    }

/** The side the bar's menus and panels open on: off the bar toward the page, ending at their button and running left. */
val BarPosition.popupSide: PopupSide
    get() = when (this) {
        BarPosition.Top -> PopupSide.BelowEnd
        BarPosition.Bottom -> PopupSide.AboveEnd
        BarPosition.Left -> PopupSide.RightOf
        BarPosition.Right -> PopupSide.LeftOf
    }

/** When the bar steps away: never, while the pen or a finger is drawing, or until it is called back from its edge. */
enum class AutoHide { Off, WhileWriting, Always }

/** Where the layers panel opens: a panel at the foot of a compact window, a card on the end edge of anything wider. */
enum class LayersPlacement { Bottom, Side }

/** The floating bar keeps this clear of the window on both sides. */
private const val FLOATING_MARGIN_DP = 12f

fun layersPlacement(windowWidthDp: Float): LayersPlacement =
    if (windowWidthDp < COMPACT_WIDTH_DP) LayersPlacement.Bottom else LayersPlacement.Side

/** Insert has its own button on the bar from medium width up. A compact bar folds it into More to leave room for the title. */
fun insertOnBar(windowWidthDp: Float): Boolean = windowWidthDp >= COMPACT_WIDTH_DP

/** The widest the floating bar may grow: its token cap, and never closer to the window edge than the margin. */
fun floatingBarMaxWidthDp(windowWidthDp: Float, capDp: Float): Float =
    minOf(capDp, maxOf(0f, windowWidthDp - 2f * FLOATING_MARGIN_DP))

/** The saved [BarPosition] named [stored], Top when there is none or it is not known. */
fun barPositionFrom(stored: String?): BarPosition =
    BarPosition.entries.firstOrNull { it.name == stored } ?: BarPosition.Top

/** The saved [AutoHide] named [stored], Off when there is none or it is not known. */
fun autoHideFrom(stored: String?): AutoHide = AutoHide.entries.firstOrNull { it.name == stored } ?: AutoHide.Off

/** The room set aside along each edge of the window, in dp. */
data class EdgeInsets(val left: Float = 0f, val top: Float = 0f, val right: Float = 0f, val bottom: Float = 0f) {
    operator fun minus(other: EdgeInsets) =
        EdgeInsets(left - other.left, top - other.top, right - other.right, bottom - other.bottom)
}

private fun inFrom(position: BarPosition, depth: Float) = when (position) {
    BarPosition.Top -> EdgeInsets(top = depth)
    BarPosition.Bottom -> EdgeInsets(bottom = depth)
    BarPosition.Left -> EdgeInsets(left = depth)
    BarPosition.Right -> EdgeInsets(right = depth)
}

/**
 * How far the bar reaches in from its edge, which is everything that must keep clear of it: a flat bar is as
 * thick as it is, a floating one adds the [gapDp] it floats off the edge.
 */
fun barOccupied(position: BarPosition, style: NoteBarStyle, thicknessDp: Float, gapDp: Float): EdgeInsets =
    inFrom(position, if (style == NoteBarStyle.Line) thicknessDp else thicknessDp + gapDp)

/** What a flat bar takes from the page, which then sits beside it. A floating bar takes none. */
fun barReserve(position: BarPosition, style: NoteBarStyle, thicknessDp: Float): EdgeInsets =
    if (style == NoteBarStyle.Line) inFrom(position, thicknessDp) else EdgeInsets()

/** These insets with [extraDp] more on every side that has room set aside, so what sits there keeps a little clear. */
fun EdgeInsets.clearOf(extraDp: Float) = EdgeInsets(
    left = if (left > 0f) left + extraDp else 0f,
    top = if (top > 0f) top + extraDp else 0f,
    right = if (right > 0f) right + extraDp else 0f,
    bottom = if (bottom > 0f) bottom + extraDp else 0f,
)

/**
 * The margins of the layers panel inside the window: [marginDp] all round for the panel at the foot, or on the top
 * and end sides for the card, and in both cases clear of the bar that [occupied] says is there.
 */
fun layersInsets(placement: LayersPlacement, occupied: EdgeInsets, marginDp: Float): EdgeInsets = when (placement) {
    LayersPlacement.Bottom -> EdgeInsets(
        left = marginDp + occupied.left,
        top = marginDp,
        right = marginDp + occupied.right,
        bottom = marginDp + occupied.bottom,
    )

    LayersPlacement.Side -> EdgeInsets(top = marginDp + occupied.top, right = marginDp + occupied.right)
}

/** Whether the bar is away: reading keeps it, and an auto-hide bar leaves while [writing] or until [summoned]. */
fun barHidden(autoHide: AutoHide, readMode: Boolean, writing: Boolean, summoned: Boolean): Boolean = when {
    readMode -> false
    autoHide == AutoHide.WhileWriting -> writing
    autoHide == AutoHide.Always -> !summoned
    else -> false
}
