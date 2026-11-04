package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The card every tool's options sit in: a centred [title] over rows of controls, the look of the Shapes and Text
 * cards. At the foot of a compact window it is a sheet along the bottom, else a panel of fixed width. It scrolls
 * when its rows are taller than the room.
 */
@Composable
internal fun ToolOptionsCard(
    title: String,
    placement: LayersPlacement,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
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
        Text(
            title,
            style = Grapho.type.label,
            color = Grapho.colors.textDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = GraphoSpace.spaceMd),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(GraphoSpace.borderWidthHairline)
                .background(Grapho.colors.borderSubtle),
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(GraphoSpace.spaceLg),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
            content = content,
        )
    }
}

/**
 * A tool's options card placed like the layers panel: at the foot of a compact window, else open from the tool's
 * button on the bar ([tool], when [barTools] shows it).
 */
@Composable
internal fun BoxScope.ToolOptionsLayer(
    ui: NoteUiState,
    tool: NoteBarTool,
    title: String,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
    content: @Composable ColumnScope.() -> Unit,
) {
    val placement = layersPlacement(windowWidth.value)
    ToolOptionsCard(
        title = title,
        placement = placement,
        modifier = barPanelPlacement(placement, ui.toolAnchors[tool].takeIf { tool in barTools }, geometry)
            // After the placement, so these are the card's own bounds and not the box it is placed in.
            .onGloballyPositioned { ui.cardBounds = it.boundsInWindow() },
        content = content,
    )
}

/** A soft sunken panel a control sits in, so each row of a card reads as one block. */
internal fun Modifier.well(): Modifier = composed {
    fillMaxWidth()
        .clip(GraphoShapes.item)
        .background(Grapho.colors.surfaceSunken)
        .padding(horizontal = GraphoSpace.spaceSm, vertical = GraphoSpace.spaceXs)
}

/** A label over [content] in a [well]. */
@Composable
internal fun WellRow(label: String, value: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Grapho.type.caption, color = Grapho.colors.textDefault, modifier = Modifier.weight(1f))
            if (value != null) Text(value, style = Grapho.type.tabular, color = Grapho.colors.textMuted)
        }
        Box(Modifier.well()) { content() }
    }
}

/** A label with its value on the right, over a slider across [range] in a [well]. */
@Composable
internal fun ValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValue: (Float) -> Unit,
) {
    WellRow(label, valueText) {
        GraphoSlider(
            value = value.coerceIn(range),
            onValueChange = onValue,
            label = label,
            valueRange = range,
            valueText = valueText,
        )
    }
}
