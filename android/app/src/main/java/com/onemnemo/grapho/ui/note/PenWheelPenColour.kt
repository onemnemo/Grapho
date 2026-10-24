package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel
import java.util.Locale

private const val SWATCH_COLUMNS = 7

/**
 * The pen editor's colour: the [swatches] in a wrapping grid ending with a rainbow swatch that opens an
 * inline picker (the colour picker's square and hue bar, and a hex field) for any other colour. Every
 * change is reported through [onColor] as it happens.
 */
@Composable
internal fun PenColourField(swatches: List<Int>, color: Color, onColor: (Color) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    val hexLabel = stringResource(R.string.colour_hex)
    val start = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(hexOf(color.toArgb())) }
    val argb = color.argbOpaque()

    fun push(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        val picked = Color.hsv(h, s, v)
        hex = hexOf(picked.toArgb())
        onColor(picked)
    }

    val cells: List<Int?> = swatches + null
    Column {
        for (row in cells.chunked(SWATCH_COLUMNS)) {
            Row {
                for (cell in row) {
                    if (cell == null) {
                        CustomSwatch(selected = custom || argb !in swatches) { custom = !custom }
                    } else {
                        Swatch(cell, selected = !custom && cell == argb) {
                            custom = false
                            onColor(Color(cell))
                        }
                    }
                }
            }
        }
    }
    if (custom) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SatValueSquare(hue, sat, value) { s, v -> push(hue, s, v) }
            BarSlider(
                fraction = hue / 360f,
                label = stringResource(R.string.colour_hue),
                onChange = { push(it * 360f, sat, value) },
            ) {
                drawRect(Brush.horizontalGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) }))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color.hsv(hue, sat, value)))
                Text("#", style = Grapho.type.label, color = GraphoWheel.textMuted)
                BasicTextField(
                    value = hex,
                    onValueChange = { v ->
                        val t = v.uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        hex = t
                        if (t.length == 6) {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(t.toInt(16) or 0xFF000000.toInt(), hsv)
                            hue = hsv[0]
                            sat = hsv[1]
                            value = hsv[2]
                            onColor(Color(t.toInt(16) or 0xFF000000.toInt()))
                        }
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
        }
    }
}

/** The rainbow swatch, like the wheel's Custom colour button, ringed while the picker is open or the colour is not a swatch. */
@Composable
private fun CustomSwatch(selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.colour_custom)
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(28.dp)) {
            drawCircle(Brush.sweepGradient(RAINBOW, center), radius = size.minDimension / 2f)
        }
        if (selected) Box(Modifier.size(36.dp).border(2.dp, GraphoWheel.text, CircleShape))
    }
}
