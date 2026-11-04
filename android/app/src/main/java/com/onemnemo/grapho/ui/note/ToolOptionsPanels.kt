package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The Eraser's options card: whether it takes whole strokes or only the part it passes over, and how big it is.
 * Both apply at once.
 */
@Composable
internal fun BoxScope.EraserOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    ToolOptionsLayer(
        ui,
        NoteBarTool.Eraser,
        stringResource(R.string.wheel_tool_eraser),
        windowWidth,
        geometry,
        barTools,
    ) {
        GlyphChoice(
            options = listOf(false to R.string.wheel_eraser_whole, true to R.string.wheel_eraser_part),
            selected = toolbar.eraserArea,
            onSelect = { toolbar.eraserArea = it },
        ) { area, ink -> drawEraserModeGlyph(area, ink) }
        ValueSlider(
            label = stringResource(R.string.wheel_size),
            value = toolbar.eraserPt,
            range = ERASER_MIN_PT..ERASER_MAX_PT,
            valueText = stringResource(R.string.pen_width_pt, toolbar.eraserPt),
            onValue = { toolbar.eraserPt = it },
        )
    }
}

/** The Lasso's options card: whether it picks with a freehand loop or a dragged rectangle. */
@Composable
internal fun BoxScope.LassoOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    ToolOptionsLayer(
        ui,
        NoteBarTool.Lasso,
        stringResource(R.string.wheel_tool_lasso),
        windowWidth,
        geometry,
        barTools,
    ) {
        GlyphChoice(
            options = listOf(false to R.string.wheel_lasso_freehand, true to R.string.wheel_lasso_rectangle),
            selected = toolbar.lassoRect,
            onSelect = { toolbar.lassoRect = it },
        ) { rect, ink -> drawLassoModeGlyph(rect, ink) }
    }
}

/**
 * Two or more choices as tiles side by side, each a drawn glyph over its name. The chosen tile is filled and edged
 * in the accent colour.
 */
@Composable
private fun <T> GlyphChoice(
    options: List<Pair<T, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
    glyph: DrawScope.(T, Color) -> Unit,
) {
    val c = Grapho.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm)) {
        for ((value, label) in options) {
            val on = value == selected
            val ink = if (on) c.selectionIndicator else c.textMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(GraphoShapes.item)
                    .background(if (on) c.selectionFill else c.surfaceSunken)
                    .border(
                        GraphoSpace.borderWidthHairline,
                        if (on) c.selectionIndicator else Color.Transparent,
                        GraphoShapes.item,
                    )
                    .pressable(role = Role.RadioButton, scaleOnPress = false, onClick = { onSelect(value) })
                    .semantics { this.selected = on }
                    .padding(vertical = GraphoSpace.spaceSm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
            ) {
                Canvas(Modifier.size(GraphoSpace.space3xl)) { glyph(value, ink) }
                Text(stringResource(label), style = Grapho.type.caption, color = if (on) c.textDefault else c.textMuted)
            }
        }
    }
}
