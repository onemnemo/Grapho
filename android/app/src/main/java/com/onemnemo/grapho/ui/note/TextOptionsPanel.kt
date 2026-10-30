package com.onemnemo.grapho.ui.note

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.ui.components.ColorSwatch
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

internal val ALIGN_ICONS = mapOf(
    TextAlignment.Start to R.drawable.ic_align_left,
    TextAlignment.Centre to R.drawable.ic_align_center,
    TextAlignment.End to R.drawable.ic_align_right,
)

internal fun alignName(align: TextAlignment): Int = when (align) {
    TextAlignment.Start -> R.string.text_align_start
    TextAlignment.Centre -> R.string.text_align_centre
    TextAlignment.End -> R.string.text_align_end
}

private const val OPAQUE = 0xFF000000.toInt()

/**
 * The choices of the Text tool in a quiet card beside the page: size, colour, alignment, weight, slant and
 * underline. They are what the next box starts with, and they change the box being typed into at once. The
 * card closes from its button, from tapping the Text tool again or from Back.
 */
@Composable
internal fun TextOptionsPanel(
    settings: TextSettings,
    palette: List<Int>,
    recent: List<Int>,
    onChange: (TextSettings) -> Unit,
    onCustomColour: (Int) -> Unit,
    onClose: () -> Unit,
    placement: LayersPlacement,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    val shape = if (placement == LayersPlacement.Bottom) {
        RoundedCornerShape(topStart = GraphoSpace.radiusContainer, topEnd = GraphoSpace.radiusContainer)
    } else {
        GraphoShapes.surface
    }
    val sized = if (placement == LayersPlacement.Bottom) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(GraphoSpace.sizePanelWidthOver)
    }
    Column(
        modifier = modifier
            .then(sized)
            .heightIn(max = GraphoSpace.sizePopoverMaxHeight)
            .floatingCard(shape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = GraphoSpace.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.wheel_tool_text),
                style = Grapho.type.heading,
                color = c.textDefault,
                modifier = Modifier.weight(1f),
            )
            GraphoIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onClose)
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceXs),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        ) {
            TextOptionRows(settings, palette, recent, onChange, onCustomColour)
        }
    }
}

/** The options card placed like the layers panel: at the foot of a compact window, else from Text on the bar. */
@Composable
internal fun BoxScope.TextOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    val placement = layersPlacement(windowWidth.value)
    TextOptionsPanel(
        // A box being typed into shows its own look, so a change builds on it and not on the last box's.
        settings = ui.textEdit?.style ?: toolbar.text,
        palette = toolbar.palette,
        recent = toolbar.recent,
        onChange = { applyTextStyle(ui, toolbar, it) },
        onCustomColour = { applyTextColour(ui, toolbar, it) },
        onClose = { ui.textOptionsOpen = false },
        placement = placement,
        modifier = barPanelPlacement(placement, ui.textAnchor.takeIf { NoteBarTool.Text in barTools }, geometry),
    )
}

@Composable
private fun TextOptionRows(
    settings: TextSettings,
    palette: List<Int>,
    recent: List<Int>,
    onChange: (TextSettings) -> Unit,
    onCustomColour: (Int) -> Unit,
) {
    SliderRow(
        R.string.text_size,
        settings.size,
        TextSettings.MIN_SIZE..TextSettings.MAX_SIZE,
    ) { onChange(settings.copy(size = it)) }
    Group(R.string.text_colour) {
        ColourRow(
            current = settings.argb,
            palette = palette,
            recent = recent,
            onPick = { onChange(settings.copy(argb = it)) },
            onCustomColour = onCustomColour,
        )
    }
    Group(R.string.text_align) {
        SegmentedControl(
            options = TextAlignment.entries.map { it to stringResource(alignName(it)) },
            selected = settings.align,
            onSelect = { onChange(settings.copy(align = it)) },
            size = SegmentSize.Small,
            icons = ALIGN_ICONS,
        )
    }
    Group(R.string.text_style) {
        Row {
            MarkButton(R.drawable.ic_bold, R.string.text_bold, settings.bold) { onChange(settings.copy(bold = it)) }
            MarkButton(R.drawable.ic_italic, R.string.text_italic, settings.italic) {
                onChange(settings.copy(italic = it))
            }
            MarkButton(R.drawable.ic_underline, R.string.text_underline, settings.underline) {
                onChange(settings.copy(underline = it))
            }
        }
    }
}

/** One of the bold, italic and underline buttons: it shows whether the mark is on and flips it when pressed. */
@Composable
internal fun MarkButton(@DrawableRes icon: Int, @StringRes label: Int, on: Boolean, onSet: (Boolean) -> Unit) {
    GraphoIconButton(icon, stringResource(label), onClick = { onSet(!on) }, selected = on)
}

/** The person's palette in one scrolling row, with the colour picker's swatch first so a long palette never hides it. */
@Composable
internal fun ColourRow(
    current: Int,
    palette: List<Int>,
    recent: List<Int>,
    onPick: (Int) -> Unit,
    onCustomColour: (Int) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        LassoColourPicker(
            initial = current,
            recent = recent,
            onApply = {
                picking = false
                onCustomColour(it)
            },
            onDismiss = { picking = false },
        )
    }
    val label = stringResource(R.string.text_colour)
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        CustomColourSwatch { picking = true }
        for (argb in palette) {
            ColorSwatch(
                color = Color(argb),
                selected = (argb or OPAQUE) == (current or OPAQUE),
                ringColor = Grapho.colors.selectionIndicator,
                label = label,
                onClick = { onPick(argb or OPAQUE) },
            )
        }
    }
}
