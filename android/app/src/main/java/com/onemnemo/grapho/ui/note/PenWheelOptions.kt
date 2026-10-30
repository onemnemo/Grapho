package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LineEnd
import com.onemnemo.grapho.note.ShapeKind
import com.onemnemo.grapho.note.ShapeSettings
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The options of Shapes, Text and the lasso sit on the rim like the eraser's: sizes on the upper rim as dots, the
 * pen's way, and the tool's choices as buttons on the lower rim.
 */
private const val ROW_BUTTON = 30f

/** Where the buttons on the lower rim sit, in the left-docked layout. */
private const val LOW_FIRST = 40f
private const val LOW_LAST = 74f
private const val LOW_MIDDLE = 57f

/** The line's arrow toggle, in the middle of the upper rim. */
private const val ARROW_AT = -52f

/** The five kinds of Shapes share the lower rim between these angles. */
private const val KINDS_FROM = 18f
private const val KINDS_TO = 82f

/** The corner radii in points that the dots of a box stand for. */
private val RADII = listOf(0, 4, 8, 14, 22, 32, 50)

/** A row of dots on the upper rim, small to large, with the one in use ringed, like the pen's sizes. */
internal class DotRow(val values: List<Int>, val selected: Int, val set: (Int) -> Unit)

/** The font sizes the dots stand for, in points, small to large. */
internal val TEXT_STEPS = listOf(12, 18, 24, 36, 54, 80)

private fun nearestIndex(values: List<Int>, value: Int): Int =
    values.indices.minByOrNull { abs(values[it] - value) } ?: 0

/** The size dots of the open tool: font size for Text, sides, points or corner radius for Shapes, else none. */
internal fun dotRow(state: ToolbarState): DotRow? {
    val shape = state.shape
    return when (state.mode) {
        ToolMode.TEXT -> DotRow(TEXT_STEPS, nearestIndex(TEXT_STEPS, state.text.size)) {
            state.text = state.text.copy(size = it).coerced()
        }
        ToolMode.SHAPE -> when (shape.kind) {
            ShapeKind.POLYGON -> DotRow(SIDE_VALUES, nearestIndex(SIDE_VALUES, shape.sides)) {
                state.shape = shape.copy(sides = it).coerced()
            }
            ShapeKind.STAR -> DotRow(SIDE_VALUES, nearestIndex(SIDE_VALUES, shape.points)) {
                state.shape = shape.copy(points = it).coerced()
            }
            ShapeKind.BOX -> DotRow(RADII, nearestIndex(RADII, shape.radius)) {
                state.shape = shape.copy(radius = it).coerced()
            }
            else -> null
        }
        else -> null
    }
}

private val SIDE_VALUES = (ShapeSettings.MIN_SIDES..ShapeSettings.MAX_SIDES).toList()

private fun dotAngle(index: Int, count: Int): Float = SIZE_FROM + (SIZE_TO - SIZE_FROM) * index / (count - 1)

/** The dot of [row] nearest to [degrees] on the rim. */
internal fun dotIndexAt(row: DotRow, degrees: Float): Int {
    val t = ((degrees - SIZE_FROM) / (SIZE_TO - SIZE_FROM)).coerceIn(0f, 1f)
    return (t * (row.values.size - 1)).roundToInt()
}

/** Draws [row] along the rim, each dot a little larger than the last, the chosen one ringed. */
internal fun DrawScope.drawDotRow(geo: Geo, c: Offset, u: Float, row: DotRow) {
    val n = row.values.size
    fun radius(i: Int): Float = 2f + 7f * i / (n - 1)
    fun at(i: Int): Offset {
        val p = polar(geo.rimR, geo.edge.toScreen(dotAngle(i, n)))
        return Offset(c.x + p.x * u, c.y + p.y * u)
    }
    for (i in 0 until n) drawCircle(GraphoWheel.dot, radius(i) * u, at(i))
    val sp = at(row.selected)
    drawCircle(GraphoWheel.selectedIndicator, radius(row.selected) * u, sp)
    drawCircle(GraphoWheel.selectedIndicator, (radius(row.selected) + 5f) * u, sp, style = Stroke(width = 2f * u))
}

/** The tools whose options the wheel can open on the rim, as pen and highlighter open their sizes. */
internal fun hasToolOptions(mode: ToolMode): Boolean =
    mode == ToolMode.SHAPE || mode == ToolMode.TEXT || mode == ToolMode.LASSO

/** True while [mode]'s options are on the rim, which the tool's own colour and size rim gives way to. */
internal fun optionsOpen(state: ToolbarState, sizeMode: Boolean): Boolean = sizeMode && hasToolOptions(state.mode)

/** The name of the tool whose options are up, for the plate that closes them. */
internal fun optionsLabel(mode: ToolMode): Int = when (mode) {
    ToolMode.SHAPE -> R.string.wheel_tool_shapes
    ToolMode.TEXT -> R.string.wheel_tool_text
    else -> R.string.wheel_tool_lasso
}

/** The colour the hub shows and opens the picker for while Shapes or Text options are up, or null without one. */
internal fun optionsColour(state: ToolbarState): Color? = when (state.mode) {
    ToolMode.SHAPE -> Color(state.pen.color)
    ToolMode.TEXT -> Color(state.text.argb)
    else -> null
}

/** The number on the open tool's plate: the font size for Text, the sides or points of a polygon or star. */
internal fun optionsReadout(state: ToolbarState): String = when (state.mode) {
    ToolMode.TEXT -> state.text.size.toString()
    ToolMode.SHAPE -> when (state.shape.kind) {
        ShapeKind.POLYGON -> state.shape.sides.toString()
        ShapeKind.STAR -> state.shape.points.toString()
        ShapeKind.BOX -> state.shape.radius.toString()
        else -> ""
    }
    else -> ""
}

private val KIND_ICONS = mapOf(
    ShapeKind.BOX to R.drawable.ic_shape_box,
    ShapeKind.OVAL to R.drawable.ic_shape_oval,
    ShapeKind.POLYGON to R.drawable.ic_shape_polygon,
    ShapeKind.STAR to R.drawable.ic_star,
    ShapeKind.LINE to R.drawable.ic_shape_line,
)

private val KIND_NAMES = mapOf(
    ShapeKind.BOX to R.string.shape_kind_box,
    ShapeKind.OVAL to R.string.shape_kind_oval,
    ShapeKind.POLYGON to R.string.shape_kind_polygon,
    ShapeKind.STAR to R.string.shape_kind_star,
    ShapeKind.LINE to R.string.shape_kind_line,
)

/** The open tool's choices on the rim: Shapes' kinds and sides, Text's sizes and marks, the lasso's loop or rectangle. */
@Composable
internal fun ToolOptionButtons(geo: Geo, state: ToolbarState) {
    when (state.mode) {
        ToolMode.SHAPE -> ShapeOptionButtons(geo, state)
        ToolMode.TEXT -> TextOptionButtons(geo, state)
        ToolMode.LASSO -> LassoOptionButtons(geo, state)
        else -> Unit
    }
}

@Composable
private fun RimButton(
    geo: Geo,
    angle: Float,
    label: String,
    active: Boolean,
    size: Float = ROW_BUTTON,
    enabled: Boolean = true,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val p = polar(geo.rimR, geo.edge.toScreen(angle))
    WheelButton(
        cx = geo.cx + p.x,
        cy = geo.cy + p.y,
        label = label,
        onClick = { if (enabled) onClick() },
        size = size,
        active = active,
    ) {
        glyph()
    }
}

@Composable
private fun ShapeOptionButtons(geo: Geo, state: ToolbarState) {
    val shape = state.shape
    val kinds = ShapeKind.entries
    kinds.forEachIndexed { i, kind ->
        val on = shape.kind == kind
        val angle = KINDS_FROM + (KINDS_TO - KINDS_FROM) * i / (kinds.size - 1)
        RimButton(geo, angle, stringResource(KIND_NAMES.getValue(kind)), on, onClick = {
            state.shape = shape.copy(kind = kind)
        }) {
            GraphoIcon(KIND_ICONS.getValue(kind), null, if (on) GraphoWheel.text else GraphoWheel.icon, size = 20.dp)
        }
    }
    if (shape.kind == ShapeKind.LINE) {
        // A line has no size to set, so its arrow sits where the size dots would.
        val arrow = shape.end == LineEnd.ARROW
        RimButton(geo, ARROW_AT, stringResource(R.string.shape_end_arrow), arrow, size = 44f, onClick = {
            state.shape = shape.copy(end = if (arrow) LineEnd.NONE else LineEnd.ARROW)
        }) {
            ArrowGlyph(arrow)
        }
    }
}

/** A short line with a head, ringed when the arrow is on. */
@Composable
private fun ArrowGlyph(on: Boolean) {
    Canvas(Modifier.size(26.dp)) {
        val ink = if (on) GraphoWheel.text else GraphoWheel.icon
        val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        drawLine(ink, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = line.width, cap = StrokeCap.Round)
        if (on) {
            val head = Path().apply {
                moveTo(w * 0.66f, h * 0.3f)
                lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.66f, h * 0.7f)
            }
            drawPath(head, ink, style = line)
        }
    }
}

@Composable
private fun TextOptionButtons(geo: Geo, state: ToolbarState) {
    val text = state.text
    MarkButton(geo, LOW_FIRST, R.drawable.ic_bold, R.string.text_bold, text.bold) { state.text = text.copy(bold = !text.bold) }
    MarkButton(geo, LOW_MIDDLE, R.drawable.ic_italic, R.string.text_italic, text.italic) { state.text = text.copy(italic = !text.italic) }
    MarkButton(geo, LOW_LAST, R.drawable.ic_underline, R.string.text_underline, text.underline) {
        state.text = text.copy(underline = !text.underline)
    }
}

@Composable
private fun MarkButton(geo: Geo, angle: Float, icon: Int, label: Int, on: Boolean, onClick: () -> Unit) {
    RimButton(geo, angle, stringResource(label), on, size = 44f, onClick = onClick) {
        GraphoIcon(icon, null, if (on) GraphoWheel.text else GraphoWheel.icon, size = 22.dp)
    }
}

@Composable
private fun LassoOptionButtons(geo: Geo, state: ToolbarState) {
    for (rect in listOf(false, true)) {
        val on = state.lassoRect == rect
        val angle = if (rect) LOW_LAST else LOW_FIRST
        RimButton(
            geo,
            angle,
            stringResource(if (rect) R.string.wheel_lasso_rectangle else R.string.wheel_lasso_freehand),
            on,
            size = 44f,
            onClick = { state.lassoRect = rect },
        ) {
            Canvas(Modifier.size(26.dp)) {
                val ink = if (on) GraphoWheel.text else GraphoWheel.icon
                val dash = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
                val w = size.width
                val h = size.height
                if (rect) {
                    drawRect(ink, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.6f), style = dash)
                } else {
                    val loop = Path().apply {
                        moveTo(w * 0.15f, h * 0.55f)
                        cubicTo(w * 0.1f, h * 0.1f, w * 0.7f, h * 0.05f, w * 0.85f, h * 0.4f)
                        cubicTo(w * 0.95f, h * 0.75f, w * 0.35f, h * 0.95f, w * 0.15f, h * 0.55f)
                    }
                    drawPath(loop, ink, style = dash)
                }
            }
        }
    }
}
