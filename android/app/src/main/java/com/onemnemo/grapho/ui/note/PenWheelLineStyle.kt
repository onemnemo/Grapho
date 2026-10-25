package com.onemnemo.grapho.ui.note

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.GraphoWheel

private const val DASH_ON = 4f
private const val DASH_OFF = 3f
private const val DOT_PERIOD = 2.5f

/**
 * The dash pattern for [line] at stroke width [width] px, with the on and off lengths in proportion to the
 * width so a thick pen gets bigger dashes. Round caps are expected: they make a dot of a near-zero dash and
 * add half a width at each end of a dash, so the "on" lengths are shortened to match.
 */
internal fun linePathEffect(line: LineStyle, width: Float): PathEffect? = when (line) {
    LineStyle.SOLID -> null
    LineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf((DASH_ON - 1f) * width, (DASH_OFF + 1f) * width))
    LineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(0.01f, DOT_PERIOD * width))
}

@StringRes
private fun descriptionOf(line: LineStyle): Int = when (line) {
    LineStyle.SOLID -> R.string.wheel_line_solid
    LineStyle.DASHED -> R.string.wheel_line_dashed
    LineStyle.DOTTED -> R.string.wheel_line_dotted
}

/**
 * Three small tiles to choose the line style, each showing a short sample of its line; the current one
 * is lit. [onPick] is told the style that was tapped.
 */
@Composable
internal fun LineStyleChoice(current: LineStyle, modifier: Modifier = Modifier, onPick: (LineStyle) -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (line in LineStyle.entries) {
            val on = line == current
            val description = stringResource(descriptionOf(line))
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) GraphoWheel.press else GraphoWheel.field)
                    .pressable(scaleOnPress = false) { onPick(line) }
                    .semantics {
                        contentDescription = description
                        selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                LineGlyph(line, if (on) GraphoWheel.text else GraphoWheel.textMuted)
            }
        }
    }
}

/** A short horizontal sample of [line] in [color]. */
@Composable
private fun LineGlyph(line: LineStyle, color: Color) {
    Canvas(Modifier.size(width = 36.dp, height = 12.dp)) {
        val w = 2.dp.toPx()
        val y = size.height / 2f
        drawLine(
            color,
            Offset(w / 2f, y),
            Offset(size.width - w / 2f, y),
            strokeWidth = w,
            cap = StrokeCap.Round,
            pathEffect = linePathEffect(line, w),
        )
    }
}
