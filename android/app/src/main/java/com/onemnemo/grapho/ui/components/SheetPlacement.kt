package com.onemnemo.grapho.ui.components

/** Where a sheet sits: pinned to the bottom edge on a narrow window, floating in the middle otherwise. */
enum class SheetPlacement { Bottom, Centered }

/** Windows narrower than this, in dp, are compact. */
internal const val COMPACT_WIDTH_DP = 600f

/** The sheet sits at the bottom of a compact window and in the centre of anything wider. */
fun sheetPlacement(windowWidthDp: Float): SheetPlacement =
    if (windowWidthDp < COMPACT_WIDTH_DP) SheetPlacement.Bottom else SheetPlacement.Centered

/** A centred sheet takes its token width but always leaves [marginDp] clear on both sides. */
fun centredSheetWidthDp(windowWidthDp: Float, preferredDp: Float, marginDp: Float): Float =
    minOf(preferredDp, maxOf(0f, windowWidthDp - 2f * marginDp))
