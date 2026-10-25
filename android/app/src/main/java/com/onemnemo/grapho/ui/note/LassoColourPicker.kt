package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel
import java.util.Locale

/** The rainbow swatch before the selection's colours, which opens [LassoColourPicker]. */
@Composable
internal fun CustomColourSwatch(onClick: () -> Unit) {
    val label = stringResource(R.string.lasso_custom_colour)
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .pressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.sweepGradient(RAINBOW)))
    }
}

/**
 * Any colour for the selection: a saturation and brightness square, a hue bar, a hex field and the last custom
 * colours. Unlike the pen's panel nothing applies while it moves, since every recolour is an undo step;
 * Apply recolours once. Starts from [initial], an ARGB colour.
 */
@Composable
internal fun LassoColourPicker(initial: Int, recent: List<Int>, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    val start = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initial, hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(hexOf(initial)) }
    val hexLabel = stringResource(R.string.colour_hex)
    val color = Color.hsv(hue, sat, value)

    fun setHsv(argb: Int) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GraphoWheel.body)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SatValueSquare(hue, sat, value) { s, v ->
                sat = s
                value = v
                hex = hexOf(Color.hsv(hue, s, v).toArgb())
            }
            BarSlider(
                fraction = hue / 360f,
                label = stringResource(R.string.colour_hue),
                onChange = {
                    hue = it * 360f
                    hex = hexOf(Color.hsv(hue, sat, value).toArgb())
                },
            ) {
                drawRect(Brush.horizontalGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) }))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(color))
                Text("#", style = Grapho.type.label, color = GraphoWheel.textMuted)
                BasicTextField(
                    value = hex,
                    onValueChange = { v ->
                        val t = v.uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        hex = t
                        if (t.length == 6) setHsv(t.toInt(16) or 0xFF000000.toInt())
                    },
                    singleLine = true,
                    textStyle = Grapho.type.label.copy(color = GraphoWheel.text),
                    cursorBrush = SolidColor(GraphoWheel.text),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphoWheel.field)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .semantics { contentDescription = hexLabel },
                )
            }
            if (recent.isNotEmpty()) {
                Text(
                    stringResource(R.string.colour_recent),
                    style = Grapho.type.caption,
                    color = GraphoWheel.textMuted,
                )
                Row {
                    for (argb in recent) {
                        val recentLabel = stringResource(R.string.colour_recent_item, hexOf(argb))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .pressable {
                                    setHsv(argb)
                                    hex = hexOf(argb)
                                }
                                .semantics { contentDescription = recentLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(Color(argb)))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PanelButton(
                    stringResource(R.string.action_cancel),
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                PanelButton(stringResource(R.string.action_apply), filled = true, modifier = Modifier.weight(1f)) {
                    onApply(Color.hsv(hue, sat, value).toArgb())
                }
            }
        }
    }
}
