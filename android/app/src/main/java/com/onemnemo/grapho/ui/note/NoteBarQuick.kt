package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.abs

/** How many colours the bar shows. */
internal const val QUICK_COLOURS = 5

/** The sizes the bar offers, as the wheel's 1 to 100 size numbers: fine, medium and bold. */
internal val QUICK_SIZES = listOf(12, 40, 72)

/** The room one colour or size takes on the bar. */
internal val QUICK_SLOT: Dp = GraphoSpace.space3xl

private val QUICK_DOT = GraphoSpace.spaceXl
private val SIZE_DOTS = listOf(4.dp, 8.dp, 12.dp)

/** The length the inline colours and sizes take on the bar, with the two rules and the gaps between items. */
internal val QUICK_LENGTH: Dp =
    (BAR_RULE_SLOT + GraphoSpace.spaceNudge) * 2 +
        (QUICK_SLOT + GraphoSpace.spaceNudge) * (QUICK_COLOURS + QUICK_SIZES.size)

/** The length the colour and size buttons take on the bar, with the rule before them. */
internal val QUICK_BUTTONS_LENGTH: Dp =
    BAR_RULE_SLOT + GraphoSpace.spaceNudge + (QUICK_SLOT + GraphoSpace.spaceNudge) * 2

/** The length [quick] takes on a horizontal bar, 0 when it is off. */
internal fun quickLength(quick: BarQuick): Dp = when (quick) {
    BarQuick.Buttons -> QUICK_BUTTONS_LENGTH
    BarQuick.Inline -> QUICK_LENGTH
    BarQuick.Off -> 0.dp
}

/** The sizes the size strip offers, as the wheel's 1 to 100 size numbers. */
internal val STRIP_SIZES = listOf(6, 20, 40, 60, 85)

/**
 * The colours shown on the bar: the first of [palette], with [current] taking the last place when it is not among
 * them, so the colour in use always shows.
 */
internal fun quickColours(palette: List<Int>, current: Int): List<Int> {
    val first = palette.map { it or OPAQUE_BITS }.distinct().take(QUICK_COLOURS)
    val now = current or OPAQUE_BITS
    return when {
        now in first -> first
        first.size < QUICK_COLOURS -> first + now
        else -> first.dropLast(1) + now
    }
}

/** The quick size nearest the [number] in use, or null when it is far from all of them. */
internal fun quickSizeAt(number: Int): Int? = QUICK_SIZES.firstOrNull { abs(it - number) <= SIZE_SLACK }

private const val SIZE_SLACK = 4
private const val OPAQUE_BITS = 0xFF000000.toInt()

/**
 * The colour and size buttons on the bar: each shows what the pen has now, and opens its strip under it, or closes
 * it when it is open.
 */
@Composable
internal fun NoteBarQuickButtons(toolbar: ToolbarState, ui: NoteUiState, vertical: Boolean) {
    val c = Grapho.colors
    val edge = c.borderControl
    val ink = c.textDefault
    val setting = currentSetting(toolbar)
    val now = Color(setting.color or OPAQUE_BITS)
    GroupRule(vertical)
    QuickDot(
        label = stringResource(R.string.text_colour),
        selected = ui.pane == BarPane.Colours,
        onClick = { ui.togglePane(BarPane.Colours) },
        modifier = Modifier.onGloballyPositioned { ui.colourAnchor = it.boundsInWindow() },
    ) { radius ->
        drawCircle(now, radius = radius)
        drawCircle(edge, radius = radius, style = Stroke(width = 1.dp.toPx()))
    }
    val number = sizeNumber(toolbar)
    QuickDot(
        label = stringResource(R.string.wheel_size),
        selected = ui.pane == BarPane.Sizes,
        onClick = { ui.togglePane(BarPane.Sizes) },
        modifier = Modifier.onGloballyPositioned { ui.sizeAnchor = it.boundsInWindow() },
    ) { radius ->
        drawCircle(ink, radius = sizeDotRadius(number, radius))
    }
}

/** A dot that grows with the size [number], from a speck up to [full]. */
private fun sizeDotRadius(number: Int, full: Float): Float = full * (0.2f + 0.8f * (number.coerceIn(1, 100) / 100f))

/**
 * The strip of colours that opens under the bar's colour button: a custom colour first, then the person's whole
 * palette. Picking one sets it on the pen and closes the strip.
 */
@Composable
internal fun BoxScope.ColourStripLayer(ui: NoteUiState, toolbar: ToolbarState, windowWidth: Dp, geometry: BarGeometry) {
    QuickStrip(ui, ui.colourAnchor, windowWidth, geometry) {
        ColourRow(
            current = currentSetting(toolbar).color,
            palette = toolbar.palette,
            recent = toolbar.recent,
            onPick = {
                backToDrawing(toolbar)
                applyColor(toolbar, it)
                ui.closePanes()
            },
            onCustomColour = {
                backToDrawing(toolbar)
                toolbar.addRecent(it)
                applyColor(toolbar, it)
                ui.closePanes()
            },
        )
    }
}

/** The strip of sizes that opens under the bar's size button. Picking one sets it on the pen and closes the strip. */
@Composable
internal fun BoxScope.SizeStripLayer(ui: NoteUiState, toolbar: ToolbarState, windowWidth: Dp, geometry: BarGeometry) {
    val ink = Grapho.colors.textDefault
    val label = stringResource(R.string.wheel_size)
    val now = if (hasQuickSize(toolbar.mode)) sizeNumber(toolbar) else null
    QuickStrip(ui, ui.sizeAnchor, windowWidth, geometry) {
        Row {
            for (number in STRIP_SIZES) {
                QuickDot(
                    label = label,
                    selected = now != null && STRIP_SIZES.minBy { abs(it - now) } == number,
                    onClick = {
                        backToDrawing(toolbar)
                        applySize(toolbar, widthOfNumber(toolbar, number.toFloat()))
                        ui.closePanes()
                    },
                    slot = Grapho.metrics.sizeHitMin,
                ) { radius ->
                    drawCircle(ink, radius = sizeDotRadius(number, radius))
                }
            }
        }
    }
}

/** A small floating strip opened from [anchor] on the bar, placed and measured like the tools' cards. */
@Composable
private fun BoxScope.QuickStrip(
    ui: NoteUiState,
    anchor: Rect?,
    windowWidth: Dp,
    geometry: BarGeometry,
    content: @Composable () -> Unit,
) {
    val placement = layersPlacement(windowWidth.value)
    Box(
        barPanelPlacement(placement, anchor, geometry)
            .onGloballyPositioned { ui.cardBounds = it.boundsInWindow() }
            .widthIn(max = GraphoSpace.sizePanelWidthOver + GraphoSpace.space5xl)
            .floatingCard(GraphoShapes.pill)
            .padding(horizontal = GraphoSpace.spaceXs),
    ) { content() }
}

/**
 * The colours and sizes on the bar, after the tools: tap one and the pen in use takes it at once. With the eraser or
 * the lasso on, a tap hands back to the pen first, since picking a colour means you want to write.
 */
@Composable
internal fun NoteBarQuick(toolbar: ToolbarState, vertical: Boolean) {
    val c = Grapho.colors
    val edge = c.borderControl
    val ink = c.textDefault
    val setting = currentSetting(toolbar)
    val colourLabel = stringResource(R.string.text_colour)
    GroupRule(vertical)
    for (argb in quickColours(toolbar.palette, setting.color)) {
        QuickDot(
            label = colourLabel,
            selected = argb == (setting.color or OPAQUE_BITS),
            onClick = {
                backToDrawing(toolbar)
                applyColor(toolbar, argb)
            },
        ) { radius ->
            drawCircle(Color(argb), radius = radius)
            drawCircle(edge, radius = radius, style = Stroke(width = 1.dp.toPx()))
        }
    }
    GroupRule(vertical)
    val sizeLabel = stringResource(R.string.wheel_size)
    val now = if (hasQuickSize(toolbar.mode)) quickSizeAt(sizeNumber(toolbar)) else null
    QUICK_SIZES.forEachIndexed { index, number ->
        QuickDot(
            label = sizeLabel,
            selected = now == number,
            onClick = {
                backToDrawing(toolbar)
                applySize(toolbar, widthOfNumber(toolbar, number.toFloat()))
            },
        ) { _ ->
            drawCircle(ink, radius = SIZE_DOTS[index].toPx() / 2f)
        }
    }
}

/** Tools that draw with the pen's colour and size. */
private fun hasQuickSize(mode: ToolMode): Boolean =
    mode == ToolMode.PEN || mode == ToolMode.HIGHLIGHTER || mode == ToolMode.SHAPE

private fun backToDrawing(toolbar: ToolbarState) {
    if (toolbar.mode == ToolMode.LASSO || toolbar.mode == ToolMode.FILL || toolbar.mode == ToolMode.ERASER) {
        toolbar.mode = toolbar.drawMode
    }
}

/** One round slot, ringed when [selected]. [face] paints it, given the dot's radius in px. */
@Composable
private fun QuickDot(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    slot: Dp = QUICK_SLOT,
    face: DrawScope.(Float) -> Unit,
) {
    val ring = Grapho.colors.selectionIndicator
    Box(
        modifier = modifier
            .size(slot)
            .clip(GraphoShapes.pill)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(slot)) {
            val radius = QUICK_DOT.toPx() / 2f
            face(radius)
            if (selected) drawCircle(ring, radius = radius + 3.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
        }
    }
}
