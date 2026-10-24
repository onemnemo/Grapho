package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.ui.theme.Grapho

internal val HueSweep = Brush.sweepGradient(List(7) { Color.hsv(it * 60f, 0.45f, 0.9f) })

/**
 * The paper colour presets as a row of dots, plus a last dot for a custom colour.
 * [selected] is the current paper (ARGB). A paper that matches no preset marks the custom dot.
 */
@Composable
fun PaperColorRow(selected: Int, onPick: (Int) -> Unit, onCustom: () -> Unit, modifier: Modifier = Modifier) {
    val ring = Grapho.colors.selectionIndicator
    val isPreset = PaperColors.presets.any { it.argb == selected }
    Row(modifier) {
        for (preset in PaperColors.presets) {
            ColorSwatch(
                color = Color(preset.argb),
                selected = preset.argb == selected,
                ringColor = ring,
                label = stringResource(preset.nameRes),
                onClick = { onPick(preset.argb) },
            )
        }
        ColorSwatch(
            color = if (isPreset) Color.Gray else Color(selected),
            selected = !isPreset,
            ringColor = ring,
            label = stringResource(R.string.colour_custom),
            onClick = onCustom,
            brush = if (isPreset) HueSweep else null,
        )
    }
}
