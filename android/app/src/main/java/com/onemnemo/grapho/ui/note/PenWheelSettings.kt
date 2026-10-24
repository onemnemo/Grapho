package com.onemnemo.grapho.ui.note

import androidx.compose.ui.graphics.Color
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.PenKind
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** The pen that colour and size changes apply to. Eraser and fill borrow the pen's. */
internal fun currentSetting(state: ToolbarState): PenSetting = when (state.mode) {
    ToolMode.HIGHLIGHTER -> state.highlighter
    // Text has a colour of its own, so the picker and the tab show that one.
    ToolMode.TEXT -> state.pen.copy(color = state.text.argb)
    else -> state.pen
}

/** Sets the colour (opaque ARGB) of the pen in use. Picking one while erasing goes back to the pen in use. */
internal fun applyColor(state: ToolbarState, argb: Int) {
    val opaque = argb or 0xFF000000.toInt()
    if (state.mode == ToolMode.HIGHLIGHTER) {
        state.highlighter = state.highlighter.copy(color = opaque)
    } else if (state.mode == ToolMode.TEXT) {
        state.text = state.text.copy(argb = opaque).coerced()
    } else {
        state.pen = state.pen.copy(color = opaque)
        if (state.mode == ToolMode.ERASER) state.mode = state.drawMode
    }
}

internal fun applySize(state: ToolbarState, widthPt: Float) {
    if (state.mode == ToolMode.HIGHLIGHTER) {
        state.highlighter = state.highlighter.copy(widthPt = widthPt)
    } else {
        state.pen = state.pen.copy(widthPt = widthPt)
        if (state.mode == ToolMode.ERASER) state.mode = state.drawMode
    }
}

/** Sets the line style of the pen in use. Picking one while erasing goes back to the pen in use. */
internal fun applyLine(state: ToolbarState, line: LineStyle) {
    if (state.mode == ToolMode.HIGHLIGHTER) {
        state.highlighter = state.highlighter.copy(line = line)
    } else {
        state.pen = state.pen.copy(line = line)
        if (state.mode == ToolMode.ERASER) state.mode = state.drawMode
    }
}

/** The tools that hand the pen back when the wheel's core is tapped while one is on. */
internal val BACK_TO_PEN_MODES = setOf(ToolMode.ERASER, ToolMode.FILL, ToolMode.LASSO, ToolMode.SHAPE, ToolMode.TEXT)

/** Picks [mode], or goes back to the pen or highlighter last used when it is already the tool. */
internal fun toggleTool(state: ToolbarState, mode: ToolMode) {
    state.mode = if (state.mode == mode) state.drawMode else mode
}

private fun widthRange(highlighter: Boolean): Pair<Float, Float> =
    if (highlighter) HIGHLIGHTER_MIN_PT to HIGHLIGHTER_MAX_PT else PEN_MIN_PT to PEN_MAX_PT

/** The width as a number from 1 to 100, roughly logarithmic over the kind's range, the way the bubble shows it. */
internal fun numberOfWidth(highlighter: Boolean, widthPt: Float): Int {
    val (min, max) = widthRange(highlighter)
    val t = ln(widthPt.coerceIn(min, max) / min) / ln(max / min)
    return (1f + t * 99f).roundToInt().coerceIn(1, 100)
}

internal fun widthOfNumber(highlighter: Boolean, number: Float): Float {
    val (min, max) = widthRange(highlighter)
    val t = ((number - 1f) / 99f).coerceIn(0f, 1f)
    return min * (max / min).pow(t)
}

internal fun sizeNumber(state: ToolbarState): Int =
    numberOfWidth(state.mode == ToolMode.HIGHLIGHTER, currentSetting(state).widthPt)

internal fun widthOfNumber(state: ToolbarState, number: Float): Float =
    widthOfNumber(state.mode == ToolMode.HIGHLIGHTER, number)

/** The tip colour as it would ink. */
internal fun tipColor(state: ToolbarState): Color =
    Color(currentSetting(state).color).copy(alpha = state.alpha / 255f)

internal fun nibKind(state: ToolbarState): PenKind =
    if (state.mode == ToolMode.HIGHLIGHTER) PenKind.Highlighter else PenKind.Pen

internal fun sameSize(a: Float, b: Float): Boolean = abs(a - b) < 0.05f
