package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoSlider
import kotlin.math.roundToInt

private const val MIN_THICKNESS = 1f
private const val MAX_THICKNESS = 12f
private const val FALLBACK_THICKNESS = 3f
private val SLIDER_WIDTH = 220.dp

/**
 * The lasso bar's Thickness page: a back button and a slider from 1 to 12, starting at [current], the width the
 * selection is drawn at (0 when it has none). The width is applied once, when the thumb is released, so a drag
 * makes one undo step.
 */
@Composable
internal fun ThicknessPage(current: Float, onBack: () -> Unit, onApply: (Float) -> Unit) {
    val start = if (current > 0f) current.coerceIn(MIN_THICKNESS, MAX_THICKNESS) else FALLBACK_THICKNESS
    var value by remember(start) { mutableFloatStateOf(start) }
    GraphoIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), onBack)
    GraphoSlider(
        value = value,
        onValueChange = { value = it },
        label = stringResource(R.string.lasso_thickness),
        modifier = Modifier.width(SLIDER_WIDTH),
        valueRange = MIN_THICKNESS..MAX_THICKNESS,
        onValueChangeFinished = { onApply(value.roundToInt().toFloat()) },
        valueText = value.roundToInt().toString(),
    )
}
