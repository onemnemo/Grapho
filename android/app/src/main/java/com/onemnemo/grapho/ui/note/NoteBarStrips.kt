package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

private val SIZE_SLIDER_WIDTH = GraphoSpace.sizePanelWidthOver - GraphoSpace.space5xl - GraphoSpace.space4xl

/**
 * The strip of colours that drops from the bar's colour button: a custom colour first, then the person's palette,
 * in slots as small as the bar's own. Picking one sets it on the pen and closes the strip.
 */
@Composable
internal fun BoxScope.ColourStripLayer(ui: NoteUiState, toolbar: ToolbarState, position: BarPosition) {
    val edge = Grapho.colors.borderControl
    val label = stringResource(R.string.text_colour)
    val current = currentSetting(toolbar).color or OPAQUE_BITS
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        LassoColourPicker(
            initial = current,
            recent = toolbar.recent,
            onApply = {
                picking = false
                backToDrawing(toolbar)
                toolbar.addRecent(it)
                applyColor(toolbar, it)
                ui.closePanes()
            },
            onDismiss = { picking = false },
        )
    }
    QuickStrip(ui, ui.colourAnchor, position) {
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            val custom = stringResource(R.string.lasso_custom_colour)
            QuickDot(label = custom, selected = false, onClick = { picking = true }) { radius ->
                drawCircle(Brush.sweepGradient(RAINBOW_STOPS), radius = radius)
            }
            for (argb in toolbar.palette.map { it or OPAQUE_BITS }.distinct()) {
                QuickDot(
                    label = label,
                    selected = argb == current,
                    onClick = {
                        backToDrawing(toolbar)
                        applyColor(toolbar, argb)
                        ui.closePanes()
                    },
                ) { radius ->
                    drawCircle(Color(argb), radius = radius)
                    drawCircle(edge, radius = radius, style = Stroke(width = 1.dp.toPx()))
                }
            }
        }
    }
}

private val RAINBOW_STOPS = listOf(
    Color(0xFFFF4D4D),
    Color(0xFFFFC94D),
    Color(0xFF7BDA5B),
    Color(0xFF4DB8FF),
    Color(0xFFB36BFF),
    Color(0xFFFF4D4D),
)

/**
 * The size strip that drops from the bar's size button: a preview of the line, a slider over the same range as the
 * wheel's, and the width in points. It stays open while you slide, and closes when you touch the page.
 */
@Composable
internal fun BoxScope.SizeStripLayer(ui: NoteUiState, toolbar: ToolbarState, position: BarPosition) {
    val ink = Grapho.colors.textDefault
    val label = stringResource(R.string.wheel_size)
    val number = sizeNumber(toolbar)
    QuickStrip(ui, ui.sizeAnchor, position) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceXs),
        ) {
            Canvas(Modifier.size(QUICK_SLOT)) { drawSizeGlyph(ink, number, size.minDimension / 2f) }
            GraphoSlider(
                value = number.toFloat(),
                onValueChange = {
                    backToDrawing(toolbar)
                    applySize(toolbar, widthOfNumber(toolbar, it))
                },
                label = label,
                valueRange = 1f..100f,
                modifier = Modifier.width(SIZE_SLIDER_WIDTH),
            )
            Text(
                stringResource(R.string.pen_width_pt, currentSetting(toolbar).widthPt),
                style = Grapho.type.tabular,
                color = Grapho.colors.textMuted,
                modifier = Modifier.widthIn(min = GraphoSpace.space4xl),
            )
        }
    }
}

/** A slim floating strip that drops from [anchor] on the bar, centred on it and as tall as the bar's buttons. */
@Composable
private fun BoxScope.QuickStrip(
    ui: NoteUiState,
    anchor: Rect?,
    position: BarPosition,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .dropFrom(anchor, position)
            .onGloballyPositioned { ui.cardBounds = it.boundsInWindow() }
            .widthIn(max = GraphoSpace.sizePanelWidthOver + GraphoSpace.space5xl)
            .floatingCard(GraphoShapes.pill),
    ) { content() }
}

/**
 * Fills the box and places what follows next to [anchor], on the side away from the bar at [position], centred on
 * the anchor and kept inside the box. With no anchor it sits centred against the bar's edge.
 */
private fun Modifier.dropFrom(anchor: Rect?, position: BarPosition): Modifier = composed {
    val density = LocalDensity.current
    val gap = with(density) { GraphoSpace.spaceSm.roundToPx() }
    val margin = with(density) { GraphoSpace.spaceSm.roundToPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    fillMaxSize()
        .onGloballyPositioned { origin = it.positionInWindow() }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val a = anchor?.translate(-origin.x, -origin.y) ?: Rect(0f, 0f, width.toFloat(), 0f)
            val cx = a.center.x.roundToInt() - placeable.width / 2
            val cy = a.center.y.roundToInt() - placeable.height / 2
            val (x, y) = when (position) {
                BarPosition.Top -> cx to a.bottom.roundToInt() + gap
                BarPosition.Bottom -> cx to a.top.roundToInt() - gap - placeable.height
                BarPosition.Left -> a.right.roundToInt() + gap to cy
                BarPosition.Right -> a.left.roundToInt() - gap - placeable.width to cy
            }
            val maxX = maxOf(margin, width - placeable.width - margin)
            val maxY = maxOf(margin, height - placeable.height - margin)
            layout(width, height) { placeable.place(x.coerceIn(margin, maxX), y.coerceIn(margin, maxY)) }
        }
}
