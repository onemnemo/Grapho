package com.onemnemo.grapho.ui.note

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LineEnd
import com.onemnemo.grapho.note.LinePath
import com.onemnemo.grapho.note.ShapeKind
import com.onemnemo.grapho.note.ShapeSettings
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

private val KIND_ICONS = mapOf(
    ShapeKind.BOX to R.drawable.ic_shape_box,
    ShapeKind.OVAL to R.drawable.ic_shape_oval,
    ShapeKind.POLYGON to R.drawable.ic_shape_polygon,
    ShapeKind.STAR to R.drawable.ic_star,
    ShapeKind.LINE to R.drawable.ic_shape_line,
)

private val PATH_ICONS = mapOf(
    LinePath.STRAIGHT to R.drawable.ic_path_straight,
    LinePath.CURVED to R.drawable.ic_path_curved,
    LinePath.ELBOW to R.drawable.ic_path_elbow,
)

private fun kindName(kind: ShapeKind): Int = when (kind) {
    ShapeKind.BOX -> R.string.shape_kind_box
    ShapeKind.OVAL -> R.string.shape_kind_oval
    ShapeKind.POLYGON -> R.string.shape_kind_polygon
    ShapeKind.STAR -> R.string.shape_kind_star
    ShapeKind.LINE -> R.string.shape_kind_line
}

private fun pathName(path: LinePath): Int = when (path) {
    LinePath.STRAIGHT -> R.string.shape_path_straight
    LinePath.CURVED -> R.string.shape_path_curved
    LinePath.ELBOW -> R.string.shape_path_elbow
}

private fun endName(end: LineEnd): Int = when (end) {
    LineEnd.NONE -> R.string.shape_end_none
    LineEnd.ARROW -> R.string.shape_end_arrow
}

/**
 * The choices of the Shapes tool in a quiet card beside the page: the kind of shape, then only the rows that fit it
 * (points and sharpness for a star, sides for a polygon, corners for a box, path and ends for a line). Colour,
 * width and line style are the pen's, set on the wheel. Every change applies at once, and the card closes from its
 * button, from tapping the Shapes tool again or from Back.
 */
@Composable
internal fun ShapeOptionsPanel(
    settings: ShapeSettings,
    onChange: (ShapeSettings) -> Unit,
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
                stringResource(R.string.wheel_tool_shapes),
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
            ShapeOptionRows(settings, onChange)
        }
    }
}

/** The options card placed like the layers panel: at the foot of a compact window, else from Shapes on the bar. */
@Composable
internal fun BoxScope.ShapeOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    val placement = layersPlacement(windowWidth.value)
    ShapeOptionsPanel(
        settings = toolbar.shape,
        onChange = { toolbar.shape = it.coerced() },
        onClose = { ui.shapeOptionsOpen = false },
        placement = placement,
        modifier = barPanelPlacement(placement, ui.shapesAnchor.takeIf { NoteBarTool.Shapes in barTools }, geometry),
    )
}

@Composable
private fun ShapeOptionRows(settings: ShapeSettings, onChange: (ShapeSettings) -> Unit) {
    Group(R.string.shape_group_shape) {
        SegmentedControl(
            options = ShapeKind.entries.map { it to stringResource(kindName(it)) },
            selected = settings.kind,
            onSelect = { onChange(settings.copy(kind = it)) },
            size = SegmentSize.Small,
            icons = KIND_ICONS,
        )
    }
    when (settings.kind) {
        ShapeKind.STAR -> {
            CountRow(R.string.shape_points, settings.points) { onChange(settings.copy(points = it)) }
            PercentRow(R.string.shape_sharpness, settings.sharpness) { onChange(settings.copy(sharpness = it)) }
        }

        ShapeKind.POLYGON -> CountRow(R.string.shape_sides, settings.sides) { onChange(settings.copy(sides = it)) }

        ShapeKind.BOX -> SliderRow(
            R.string.shape_corners,
            settings.radius,
            0..ShapeSettings.MAX_RADIUS,
        ) { onChange(settings.copy(radius = it)) }

        ShapeKind.LINE -> LineRows(settings, onChange)

        ShapeKind.OVAL -> Unit
    }
}

@Composable
private fun LineRows(settings: ShapeSettings, onChange: (ShapeSettings) -> Unit) {
    Group(R.string.shape_path) {
        SegmentedControl(
            options = LinePath.entries.map { it to stringResource(pathName(it)) },
            selected = settings.path,
            onSelect = { onChange(settings.copy(path = it)) },
            size = SegmentSize.Small,
            icons = PATH_ICONS,
        )
    }
    Group(R.string.shape_end_start) {
        SegmentedControl(
            options = LineEnd.entries.map { it to stringResource(endName(it)) },
            selected = settings.start,
            onSelect = { onChange(settings.copy(start = it)) },
            size = SegmentSize.Small,
        )
    }
    Group(R.string.shape_end_end) {
        SegmentedControl(
            options = LineEnd.entries.map { it to stringResource(endName(it)) },
            selected = settings.end,
            onSelect = { onChange(settings.copy(end = it)) },
            size = SegmentSize.Small,
        )
    }
}

/** A quiet label over a control. */
@Composable
internal fun Group(label: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
        Text(stringResource(label), style = Grapho.type.caption, color = Grapho.colors.textMuted)
        content()
    }
}

@Composable
private fun CountRow(label: Int, value: Int, onValue: (Int) -> Unit) =
    SliderRow(label, value, ShapeSettings.MIN_SIDES..ShapeSettings.MAX_SIDES, onValue = onValue)

@Composable
private fun PercentRow(label: Int, value: Int, onValue: (Int) -> Unit) =
    SliderRow(label, value, ShapeSettings.MIN_SHARPNESS..ShapeSettings.MAX_SHARPNESS, "%", onValue)

/** A label with its number, over a slider across [range]. */
@Composable
internal fun SliderRow(label: Int, value: Int, range: IntRange, suffix: String = "", onValue: (Int) -> Unit) {
    val name = stringResource(label)
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Grapho.type.caption, color = Grapho.colors.textMuted, modifier = Modifier.weight(1f))
            Text("$value$suffix", style = Grapho.type.tabular, color = Grapho.colors.textDefault)
        }
        GraphoSlider(
            value = value.toFloat(),
            onValueChange = { onValue(it.roundToInt().coerceIn(range)) },
            label = name,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            valueText = "$value$suffix",
        )
    }
}
