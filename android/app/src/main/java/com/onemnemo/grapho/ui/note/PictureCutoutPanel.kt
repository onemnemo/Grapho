package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSlider
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

private fun outlineName(outline: CutoutOutline): Int = when (outline) {
    CutoutOutline.None -> R.string.picture_outline_none
    CutoutOutline.Thin -> R.string.picture_outline_thin
    CutoutOutline.Thick -> R.string.picture_outline_thick
}

private fun toolName(tool: CutoutTool): Int = when (tool) {
    CutoutTool.Auto -> R.string.picture_tool_auto
    CutoutTool.Erase -> R.string.picture_tool_erase
    CutoutTool.Restore -> R.string.picture_tool_restore
}

/**
 * The choices for a cut-out under its picture: Auto, which takes how much counts as background and the band drawn
 * around what is kept, or the Erase and Restore brushes with their [brush] size. Every change to [options] goes to
 * [onChange] at once; the caller decides when to cut again. [onTool] picks the tool, and [onDone] ends the brushing.
 */
@Composable
internal fun PictureCutoutPanel(
    options: CutoutOptions,
    onChange: (CutoutOptions) -> Unit,
    brush: CutoutBrush,
    onTool: (CutoutTool) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    Column(
        modifier = modifier
            .width(GraphoSpace.sizePanelWidthOver)
            .floatingCard(GraphoShapes.surface)
            .padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceSm),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        SegmentedControl(
            options = CutoutTool.entries.map { it to stringResource(toolName(it)) },
            selected = brush.tool,
            onSelect = onTool,
            size = SegmentSize.Small,
        )
        if (brush.tool == CutoutTool.Auto) {
            Text(stringResource(R.string.picture_strength), style = Grapho.type.label, color = c.textMuted)
            GraphoSlider(
                value = options.strength.toFloat(),
                onValueChange = { onChange(options.copy(strength = it.roundToInt())) },
                label = stringResource(R.string.picture_strength_hint),
                valueRange = 0f..CutoutOptions.MAX_STRENGTH.toFloat(),
                valueText = options.strength.toString(),
            )
            Text(stringResource(R.string.picture_outline), style = Grapho.type.label, color = c.textMuted)
            SegmentedControl(
                options = CutoutOutline.entries.map { it to stringResource(outlineName(it)) },
                selected = options.outline,
                onSelect = { onChange(options.copy(outline = it)) },
                size = SegmentSize.Small,
            )
        } else {
            Text(stringResource(R.string.picture_brush_size), style = Grapho.type.label, color = c.textMuted)
            GraphoSlider(
                value = brush.size,
                onValueChange = { brush.size = it },
                label = stringResource(R.string.picture_brush_hint),
                valueRange = CutoutBrush.MIN_SIZE..CutoutBrush.MAX_SIZE,
                valueText = brush.size.roundToInt().toString(),
            )
            GraphoButton(
                stringResource(R.string.action_done),
                onClick = onDone,
                kind = ButtonKind.Primary,
                size = ButtonSize.Small,
            )
        }
    }
}
