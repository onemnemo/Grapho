package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.ui.components.ColorSwatch
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The font size moves this many points for each press of the size buttons. */
private const val SIZE_STEP = 2

/** The widest the palette row under the format bar grows, so a long palette scrolls. */
private val PALETTE_MAX_WIDTH = 320.dp

/** Where the format bar goes, in view pixels. */
internal object FormatBarPlacement {
    /**
     * Puts a bar [barWidth] by [barHeight] centred over a box spanning [left] to [right] and [top] to [bottom], [gap]
     * above it. When there is no room above, it goes [belowGap] under the box instead, and always inside the window
     * [windowWidth] by [windowHeight] less the margins.
     */
    fun above(
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        gap: Float,
        belowGap: Float,
        barWidth: Float,
        barHeight: Float,
        windowWidth: Float,
        windowHeight: Float,
        margins: EdgeInsets,
    ): PillSpot {
        val xLow = min(margins.left, max(0f, windowWidth - barWidth))
        val xHigh = max(xLow, windowWidth - barWidth - margins.right)
        val x = ((left + right) / 2f - barWidth / 2f).coerceIn(xLow, xHigh)
        val yLow = min(margins.top, max(0f, windowHeight - barHeight))
        val yHigh = max(yLow, windowHeight - barHeight - margins.bottom)
        val over = top - gap - barHeight
        val y = (if (over >= yLow) over else bottom + belowGap).coerceIn(yLow, yHigh)
        return PillSpot(x, y)
    }
}

/** Puts the bar over [rect] as [FormatBarPlacement.above] says, a frame at a time, without recomposing. */
private fun Modifier.placedAbove(rect: State<RectF?>, gap: Float, belowGap: Float, margins: EdgeInsets): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val r = rect.value
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (r != null) {
                val spot = FormatBarPlacement.above(
                    r.left,
                    r.right,
                    r.top,
                    r.bottom,
                    gap,
                    belowGap,
                    placeable.width.toFloat(),
                    placeable.height.toFloat(),
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat(),
                    margins,
                )
                placeable.place(spot.x.roundToInt(), spot.y.roundToInt())
            }
        }
    }

/** Makes the look of the open box [style] and remembers it for the next box. */
internal fun applyTextStyle(ui: NoteUiState, toolbar: ToolbarState, style: TextSettings) {
    toolbar.text = style.coerced()
    ui.view[0]?.setTextStyle(toolbar.text)
}

/** Colours the open box with a picked custom colour, keeps it among the recent ones, and remembers it. */
internal fun applyTextColour(ui: NoteUiState, toolbar: ToolbarState, argb: Int) {
    toolbar.addRecent(argb)
    applyTextStyle(ui, toolbar, (ui.textEdit?.style ?: toolbar.text).copy(argb = argb))
}

/**
 * The pill over the open text box with the choices people expect there: size, bold, italic, underline, alignment and
 * colour. It follows [rect], the box in view pixels, [gap] clear of it (and [belowRoom] more when it has to go
 * under the box), and stays inside [margins]. It is drawn in the page's own layer and takes no focus, so the
 * keyboard stays up while it is used. A change goes to [onChange] with the whole new look of the box.
 */
@Composable
internal fun TextFormatBar(
    style: TextSettings,
    rect: State<RectF?>,
    gap: Float,
    belowRoom: Float,
    margins: EdgeInsets,
    palette: List<Int>,
    recent: List<Int>,
    onChange: (TextSettings) -> Unit,
    onCustomColour: (Int) -> Unit,
) {
    var colourOpen by remember { mutableStateOf(false) }
    val shape = if (colourOpen) GraphoShapes.surface else GraphoShapes.pill
    Column(
        Modifier
            .placedAbove(rect, gap, gap + belowRoom, margins)
            .floatingCard(shape)
            .padding(horizontal = GraphoSpace.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GraphoIconButton(
                R.drawable.ic_minus,
                stringResource(R.string.text_smaller),
                onClick = { onChange(style.copy(size = style.size - SIZE_STEP)) },
                enabled = style.size > TextSettings.MIN_SIZE,
            )
            Text(
                style.size.toString(),
                style = Grapho.type.label,
                color = Grapho.colors.textDefault,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp),
            )
            GraphoIconButton(
                R.drawable.ic_plus,
                stringResource(R.string.text_larger),
                onClick = { onChange(style.copy(size = style.size + SIZE_STEP)) },
                enabled = style.size < TextSettings.MAX_SIZE,
            )
            MarkButton(R.drawable.ic_bold, R.string.text_bold, style.bold) { onChange(style.copy(bold = it)) }
            MarkButton(R.drawable.ic_italic, R.string.text_italic, style.italic) { onChange(style.copy(italic = it)) }
            MarkButton(R.drawable.ic_underline, R.string.text_underline, style.underline) {
                onChange(style.copy(underline = it))
            }
            GraphoIconButton(
                ALIGN_ICONS.getValue(style.align),
                stringResource(alignName(style.align)),
                onClick = { onChange(style.copy(align = nextAlignment(style.align))) },
            )
            ColorSwatch(
                color = Color(style.argb),
                selected = colourOpen,
                ringColor = Grapho.colors.selectionIndicator,
                label = stringResource(R.string.text_colour),
                onClick = { colourOpen = !colourOpen },
            )
        }
        if (colourOpen) {
            Column(Modifier.widthIn(max = PALETTE_MAX_WIDTH), verticalArrangement = Arrangement.Center) {
                ColourRow(
                    current = style.argb,
                    palette = palette,
                    recent = recent,
                    onPick = { onChange(style.copy(argb = it)) },
                    onCustomColour = onCustomColour,
                )
            }
        }
    }
}

/** The alignment after [align] when the button is pressed: start, centre, end, and round again. */
internal fun nextAlignment(align: TextAlignment): TextAlignment = when (align) {
    TextAlignment.Start -> TextAlignment.Centre
    TextAlignment.Centre -> TextAlignment.End
    TextAlignment.End -> TextAlignment.Start
}
