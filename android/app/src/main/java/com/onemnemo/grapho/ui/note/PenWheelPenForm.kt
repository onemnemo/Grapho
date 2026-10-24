package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.ui.components.PenKind
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel
import com.onemnemo.grapho.ui.theme.PenColors
import kotlin.math.roundToInt

private const val NAME_LIMIT = 24

/** The editor under a pen's row: name, kind, size dots like the wheel's, opacity and colour. */
@Composable
internal fun PenEditor(state: ToolbarState, store: PenStore, index: Int) {
    val pen = store.pens[index]
    val nameLabel = stringResource(R.string.wheel_pen_name)
    var name by remember(index) { mutableStateOf(pen.name) }
    val highlighter = pen.kind == PenKind.Highlighter
    val min = if (highlighter) HIGHLIGHTER_MIN_PT else PEN_MIN_PT
    val max = if (highlighter) HIGHLIGHTER_MAX_PT else PEN_MAX_PT
    val number = numberOfWidth(highlighter, pen.widthPt)
    // Fixed while this pen is open, so a colour made with the custom picker does not add swatches as it moves.
    val swatches = remember(index) {
        (state.palette + currentSetting(state).color + pen.color.argbOpaque()).distinct()
    }
    fun update(
        kind: PenKind = pen.kind,
        color: Color = pen.color,
        widthPt: Float = pen.widthPt,
        alpha: Int = pen.alpha,
        line: LineStyle = pen.line,
    ) = store.replace(index, PenPreset(pen.name, kind, color, widthPt, alpha, line))

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, GraphoWheel.edge, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = { v ->
                name = v.take(NAME_LIMIT)
                if (name.isNotBlank()) store.replace(index, PenPreset(name.trim(), pen.kind, pen.color, pen.widthPt, pen.alpha, pen.line))
            },
            singleLine = true,
            textStyle = Grapho.type.label.copy(color = GraphoWheel.text),
            cursorBrush = SolidColor(GraphoWheel.text),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GraphoWheel.field)
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .semantics { contentDescription = nameLabel },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((kind, label) in listOf(
                PenKind.Pen to stringResource(R.string.wheel_tool_pen),
                PenKind.Highlighter to stringResource(R.string.wheel_tool_highlighter),
            )) {
                val on = pen.kind == kind
                val kindLabel = stringResource(R.string.wheel_pen_kind, label)
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) GraphoWheel.text else GraphoWheel.selectedFill)
                        .pressable {
                            if (!on) {
                                val range = if (kind == PenKind.Highlighter) {
                                    HIGHLIGHTER_MIN_PT..HIGHLIGHTER_MAX_PT
                                } else {
                                    PEN_MIN_PT..PEN_MAX_PT
                                }
                                val alpha = if (kind == PenKind.Highlighter) PenColors.HIGHLIGHT_ALPHA else ALPHA_MAX
                                update(kind = kind, widthPt = pen.widthPt.coerceIn(range), alpha = alpha)
                            }
                        }
                        .semantics { contentDescription = kindLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Grapho.type.caption, color = if (on) GraphoWheel.body else GraphoWheel.text, maxLines = 1)
                }
            }
        }
        Text(
            stringResource(R.string.wheel_size_pt, number, pen.widthPt.coerceIn(min, max)),
            style = Grapho.type.caption,
            color = GraphoWheel.textMuted,
        )
        SizeDots(number) { update(widthPt = widthOfNumber(highlighter, it)) }
        Text(stringResource(R.string.wheel_line), style = Grapho.type.caption, color = GraphoWheel.textMuted)
        LineStyleChoice(pen.line) { update(line = it) }
        Text(
            stringResource(R.string.wheel_opacity_percent, (pen.alpha * 100f / ALPHA_MAX).roundToInt()),
            style = Grapho.type.caption,
            color = GraphoWheel.textMuted,
        )
        BarSlider(
            fraction = (pen.alpha - ALPHA_MIN).toFloat() / (ALPHA_MAX - ALPHA_MIN),
            label = stringResource(R.string.colour_opacity),
            onChange = { update(alpha = ALPHA_MIN + (it * (ALPHA_MAX - ALPHA_MIN)).roundToInt()) },
        ) {
            drawChecker(7.dp.toPx())
            drawRect(Brush.horizontalGradient(listOf(pen.color.copy(alpha = 0.1f), pen.color)))
        }
        Text(stringResource(R.string.colour_label), style = Grapho.type.caption, color = GraphoWheel.textMuted)
        key(index) { PenColourField(swatches, pen.color) { update(color = it) } }
    }
}
