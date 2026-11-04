package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.PenGlyph
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

private const val PERCENT = 100f
private const val STABILIZER_STEPS = 100f

/**
 * The options card of the pen and the highlighter: the saved pens to pick from, then thickness, opacity and stroke
 * stabilisation, the line style, the colour, and a button that saves the pen as it is now into the pen box. Every
 * change applies at once to the tool in use.
 */
@Composable
internal fun BoxScope.PenOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    store: PenStore,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    val highlighter = toolbar.mode == ToolMode.HIGHLIGHTER
    ToolOptionsLayer(
        ui,
        if (highlighter) NoteBarTool.Highlighter else NoteBarTool.Pen,
        stringResource(if (highlighter) R.string.wheel_tool_highlighter else R.string.wheel_tool_pen),
        windowWidth,
        geometry,
        barTools,
    ) {
        PenOptionRows(toolbar, store)
    }
}

@Composable
private fun PenOptionRows(toolbar: ToolbarState, store: PenStore) {
    val context = LocalContext.current
    PenBox(toolbar, store)
    val width = currentSetting(toolbar).widthPt
    ValueSlider(
        label = stringResource(R.string.lasso_thickness),
        value = sizeNumber(toolbar).toFloat(),
        range = 1f..PERCENT,
        valueText = stringResource(R.string.pen_width_pt, width),
        onValue = { applySize(toolbar, widthOfNumber(toolbar, it)) },
    )
    ValueSlider(
        label = stringResource(R.string.layer_opacity),
        value = toolbar.alpha.toFloat(),
        range = ALPHA_MIN.toFloat()..ALPHA_MAX.toFloat(),
        valueText = stringResource(R.string.value_percent, percentOfAlpha(toolbar.alpha)),
        onValue = { toolbar.alpha = it.roundToInt() },
    )
    ValueSlider(
        label = stringResource(R.string.pen_stabilization),
        value = toolbar.stabilizer * STABILIZER_STEPS,
        range = 0f..STABILIZER_STEPS,
        valueText = stringResource(R.string.value_percent, (toolbar.stabilizer * PERCENT).roundToInt()),
        onValue = { toolbar.stabilizer = (it / STABILIZER_STEPS).coerceIn(0f, 1f) },
    )
    Group(R.string.wheel_line) {
        LineStyleChoice(current = currentSetting(toolbar).line) { applyLine(toolbar, it) }
    }
    Group(R.string.text_colour) {
        ColourRow(
            current = currentSetting(toolbar).color,
            palette = toolbar.palette,
            recent = toolbar.recent,
            onPick = { applyColor(toolbar, it) },
            onCustomColour = {
                toolbar.addRecent(it)
                applyColor(toolbar, it)
            },
        )
    }
    OutlinedAction(stringResource(R.string.pen_add_to_box)) {
        store.add(currentAsPreset(context, toolbar, store.pens.count { it.kind == nibKind(toolbar) }))
    }
}

/** The ink's opacity as a whole percent. */
private fun percentOfAlpha(alpha: Int): Int = (alpha * PERCENT / ALPHA_MAX).roundToInt()

/** The saved pens in one scrolling row, each standing with its name under it. The one in use is raised and filled. */
@Composable
private fun PenBox(toolbar: ToolbarState, store: PenStore) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = GraphoSpace.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        for (preset in store.pens) {
            PenBoxItem(preset, isCurrent(toolbar, preset)) { applyPreset(toolbar, preset) }
        }
    }
}

@Composable
private fun PenBoxItem(preset: PenPreset, current: Boolean, onPick: () -> Unit) {
    val ink = preset.color.copy(alpha = preset.alpha / ALPHA_MAX.toFloat())
    Column(
        modifier = Modifier
            .width(GraphoSpace.space5xl)
            .offset(y = if (current) -GraphoSpace.spaceSm else 0.dp)
            .clip(GraphoShapes.item)
            .background(if (current) Grapho.colors.selectionFill else Color.Transparent)
            .pressable(scaleOnPress = false, onClick = onPick)
            .padding(vertical = GraphoSpace.spaceXs)
            .semantics {
                contentDescription = preset.name
                selected = current
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PenGlyph(
            kind = preset.kind,
            color = ink,
            outline = Grapho.colors.textMuted,
            modifier = Modifier.size(width = GraphoSpace.space3xl, height = GraphoSpace.space4xl),
        )
        Text(
            preset.name,
            style = Grapho.type.caption,
            color = Grapho.colors.textDefault,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceXs),
        )
    }
}

/** A full-width button drawn as an outline, for the one action at the foot of a card. */
@Composable
private fun OutlinedAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Grapho.metrics.sizeControlDefault)
            .clip(GraphoShapes.pill)
            .border(GraphoSpace.borderWidthHairline, Grapho.colors.borderStrong, GraphoShapes.pill)
            .pressable(onClick = onClick)
            .padding(horizontal = GraphoSpace.spaceLg, vertical = GraphoSpace.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Grapho.type.label, color = Grapho.colors.textDefault, textAlign = TextAlign.Center)
    }
}
