package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * Where the note bar sits and what keeps clear of it, in dp. [occupied] is how far the bar reaches in from its
 * edge, [reserve] is what a flat bar takes from the page itself, and [overlay] is what a floating bar covers of the
 * page, which the pen wheel and the cards on the page must stay out of.
 */
internal data class BarGeometry(val position: BarPosition, val occupied: EdgeInsets, val reserve: EdgeInsets) {
    val overlay: EdgeInsets get() = occupied - reserve

    /** How far the bar slides to leave: out past its own depth. */
    val travel: Dp get() = maxOf(occupied.left, occupied.top, occupied.right, occupied.bottom).dp
}

@Composable
internal fun barGeometry(position: BarPosition, style: NoteBarStyle): BarGeometry {
    val thickness = Grapho.metrics.sizeBarNote.value
    return BarGeometry(
        position,
        barOccupied(position, style, thickness, FLOATING_BAR_GAP.value),
        barReserve(position, style, thickness),
    )
}

/** The insets as padding on the window's physical sides, which is how the bar is placed whatever the layout direction. */
internal fun EdgeInsets.toPadding(): PaddingValues = PaddingValues.Absolute(left.dp, top.dp, right.dp, bottom.dp)

/** The bar's place in the window's box, on the physical edge it is against. */
internal fun BarPosition.alignment(): Alignment = when (this) {
    BarPosition.Top -> Alignment.TopCenter
    BarPosition.Bottom -> Alignment.BottomCenter
    BarPosition.Left -> BiasAbsoluteAlignment(-1f, 0f)
    BarPosition.Right -> BiasAbsoluteAlignment(1f, 0f)
}

/**
 * The tools of [tools] that fit the bar along the window's width or height, whichever the bar runs along. A vertical
 * bar has no title, and a floating one keeps its margin from the window like the horizontal pill does.
 */
@Composable
internal fun barFitFor(
    tools: List<NoteBarTool>,
    mode: ToolMode,
    position: BarPosition,
    style: NoteBarStyle,
    wantTitle: Boolean,
    windowWidth: Dp,
    windowHeight: Dp,
): BarFit {
    val insertBar = insertOnBar(windowWidth.value)
    val length = if (position.vertical) windowHeight.value else windowWidth.value
    return fitBar(
        tools = tools,
        active = NoteBarTool.forMode(mode),
        compact = !insertBar,
        wantTitle = wantTitle && !position.vertical,
        m = BarMeasure(
            barLengthDp = if (style == NoteBarStyle.Floating) {
                floatingBarMaxWidthDp(length, GraphoSpace.sizeBarNoteMax.value)
            } else {
                length
            },
            fixedSlots = barFixedSlots(insertBar, position.vertical),
            slotDp = (Grapho.metrics.sizeHitMin + GraphoSpace.spaceNudge).value,
            separatorDp = BAR_RULE_SLOT.value,
            paddingDp = (GraphoSpace.spaceXs * 2).value,
            titleMinDp = BAR_TITLE_MIN.value,
        ),
    )
}
