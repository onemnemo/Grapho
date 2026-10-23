package com.onemnemo.grapho.ui.note

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.note.ShapeSettings
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.note.ToolState
import com.onemnemo.grapho.ui.theme.PenColors

/** Colour (opaque ARGB), width and line style of one pen. */
data class PenSetting(val color: Int, val widthPt: Float, val line: LineStyle = LineStyle.SOLID)

const val PEN_MIN_PT = 0.3f
const val PEN_MAX_PT = 12f
const val HIGHLIGHTER_MIN_PT = 4f
const val HIGHLIGHTER_MAX_PT = 30f
const val ERASER_PT = 16f
const val ERASER_MIN_PT = 4f
const val ERASER_MAX_PT = 60f

/** Ink opacity runs from 10% to 100%. */
const val ALPHA_MIN = 26
const val ALPHA_MAX = 255

/**
 * What the person has picked in the pen wheel. Pen and highlighter keep their own colour, width and
 * opacity for the session.
 */
@Stable
class ToolbarState {
    private var modeState by mutableStateOf(ToolMode.PEN)

    var mode: ToolMode
        get() = modeState
        set(value) {
            modeState = value
            if (value == ToolMode.PEN || value == ToolMode.HIGHLIGHTER) drawMode = value
        }

    /** The last of pen and highlighter in use, which eraser and fill hand back to. */
    var drawMode by mutableStateOf(ToolMode.PEN)
        private set

    /** The wheel stays open when the page is touched. */
    var wheelPinned by mutableStateOf(false)

    /** The wheel is being carried, so the page leaves it open. */
    var wheelCarrying by mutableStateOf(false)
    var pen by mutableStateOf(PenSetting(PenColors.pens.first().second.toArgb(), 1.5f))
    var highlighter by mutableStateOf(PenSetting(PenColors.highlighters.first().second.toArgb(), 12f))

    /** Ink alpha (0 to 255) of the pen and of the highlighter, set by the wheel's opacity band. */
    var penAlpha by mutableIntStateOf(ALPHA_MAX)
    var highlighterAlpha by mutableIntStateOf(PenColors.HIGHLIGHT_ALPHA)

    /** What the Shapes tool draws. Its colour, width and line style are the pen's. */
    var shape by mutableStateOf(ShapeSettings())

    /** What the Text tool starts a box with: size, colour, alignment and weight. */
    var text by mutableStateOf(TextSettings())

    /** The lasso picks a dragged rectangle instead of a freehand loop. */
    var lassoRect by mutableStateOf(false)

    /** The eraser cuts only the part it passes over, instead of taking whole strokes. */
    var eraserArea by mutableStateOf(false)

    /** The eraser's diameter, kept within [ERASER_MIN_PT] and [ERASER_MAX_PT]. */
    var eraserPt: Float
        get() = eraserPtState
        set(value) {
            eraserPtState = value.coerceIn(ERASER_MIN_PT, ERASER_MAX_PT)
        }
    private var eraserPtState by mutableFloatStateOf(ERASER_PT)

    /** The person's colour sets, filled from the colour store when the wheel first opens. */
    internal val sets = mutableStateListOf<ColourSet>()

    /** Every colour of every set, flattened. */
    val palette: List<Int> get() = sets.flatMap { it.colours }

    /** The rim's slots in order: colours, with [RIM_STRIP] between sets. */
    val rim: List<Int> by derivedStateOf { rimOf(sets) }

    /** The last custom colours, newest first, at most [RECENT_MAX]. */
    val recent = mutableStateListOf<Int>()

    /** The alpha of whichever pen is in use. Eraser and fill borrow the pen's. */
    var alpha: Int
        get() = if (mode == ToolMode.HIGHLIGHTER) highlighterAlpha else penAlpha
        set(value) {
            val a = value.coerceIn(ALPHA_MIN, ALPHA_MAX)
            if (mode == ToolMode.HIGHLIGHTER) highlighterAlpha = a else penAlpha = a
        }

    fun addRecent(argb: Int) {
        val opaque = argb or 0xFF000000.toInt()
        recent.remove(opaque)
        recent.add(0, opaque)
        while (recent.size > RECENT_MAX) recent.removeAt(recent.size - 1)
    }

    /** The tool as the canvas wants it. The pens' alpha is applied here. */
    val toolState: ToolState
        get() = when (mode) {
            ToolMode.PEN -> ToolState(
                ToolMode.PEN,
                withAlpha(pen.color, penAlpha),
                pen.widthPt,
                eraserArea,
                eraserPt,
                pen.line,
            )

            ToolMode.HIGHLIGHTER -> ToolState(
                ToolMode.HIGHLIGHTER,
                withAlpha(highlighter.color, highlighterAlpha),
                highlighter.widthPt,
                eraserArea,
                eraserPt,
                highlighter.line,
            )

            ToolMode.ERASER -> ToolState(ToolMode.ERASER, pen.color, eraserPt, eraserArea, eraserPt)

            // Fill paints with the pen's colour, opaque.
            ToolMode.FILL -> ToolState(ToolMode.FILL, pen.color, pen.widthPt, eraserArea, eraserPt)

            ToolMode.LASSO -> ToolState(
                ToolMode.LASSO,
                pen.color,
                pen.widthPt,
                eraserArea,
                eraserPt,
                lassoRect = lassoRect,
            )

            // Shapes ink like the pen, with the pen's opacity.
            ToolMode.SHAPE -> ToolState(
                ToolMode.SHAPE,
                withAlpha(pen.color, penAlpha),
                pen.widthPt,
                eraserArea,
                eraserPt,
                pen.line,
                shape,
            )

            ToolMode.TEXT -> ToolState(
                ToolMode.TEXT,
                text.argb,
                pen.widthPt,
                eraserArea,
                eraserPt,
                text = text,
            )
        }

    private fun withAlpha(argb: Int, alpha: Int): Int = (argb and 0x00FFFFFF) or (alpha shl 24)

    companion object {
        const val RECENT_MAX = 8
    }
}
