package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

/**
 * The custom colour panel, opened from the rainbow swatch: a saturation and
 * brightness square, hue and opacity bars, a hex field and the last custom
 * colours. Everything applies to the pen as it moves. Tapping outside closes it.
 */
@Composable
internal fun ColorPickerPanel(
    state: ToolbarState,
    book: ColourBook,
    editSlot: Pair<Int, Int>?,
    progress: Float,
    onAdded: (index: Int) -> Unit,
    onManage: () -> Unit,
    onDone: () -> Unit,
) {
    val editing = editSlot != null
    // Editing starts from the colour in that slot and leaves the pen alone until it is saved.
    val startArgb = remember {
        editSlot?.let { book.sets.getOrNull(it.first)?.colours?.getOrNull(it.second) } ?: currentSetting(state).color
    }
    val start = remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(startArgb, hsv)
        hsv
    }
    // The set that Add to palette fills: the one the current colour came from, else the last.
    var target by remember {
        val at = book.sets.indexOfFirst { it.colours.contains(currentSetting(state).color) }
        mutableIntStateOf(if (at >= 0) at else book.sets.lastIndex.coerceAtLeast(0))
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hex by remember { mutableStateOf(hexOf(startArgb)) }
    val hexLabel = stringResource(R.string.colour_hex)
    val alpha = state.alpha
    val color = Color.hsv(hue, sat, value)

    fun push(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        value = v
        val argb = Color.hsv(h, s, v).toArgb()
        hex = hexOf(argb)
        if (!editing) applyColor(state, argb)
    }

    fun pushArgb(argb: Int) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        push(hsv[0], hsv[1], hsv[2])
    }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDone,
        properties = PopupProperties(focusable = true),
    ) {
        // Room inside the window around the panel.
        Box(
            Modifier
                .padding(12.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * progress
                    scaleX = s
                    scaleY = s
                    this.alpha = progress
                },
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GraphoWheel.body)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SatValueSquare(hue, sat, value) { s, v -> push(hue, s, v) }
                BarSlider(
                    fraction = hue / 360f,
                    label = stringResource(R.string.colour_hue),
                    onChange = { push(it * 360f, sat, value) },
                ) {
                    drawRect(Brush.horizontalGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) }))
                }
                if (!editing) BarSlider(
                    fraction = (alpha - ALPHA_MIN).toFloat() / (ALPHA_MAX - ALPHA_MIN),
                    label = stringResource(R.string.colour_opacity),
                    onChange = { applyAlpha(state, ALPHA_MIN + (it * (ALPHA_MAX - ALPHA_MIN)).roundToInt()) },
                ) {
                    drawChecker(7.dp.toPx())
                    drawRect(Brush.horizontalGradient(listOf(color.copy(alpha = 0.1f), color)))
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
                            if (t.length == 6) {
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(t.toInt(16) or 0xFF000000.toInt(), hsv)
                                hue = hsv[0]
                                sat = hsv[1]
                                value = hsv[2]
                                if (!editing) applyColor(state, t.toInt(16))
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

                Text(stringResource(R.string.colour_recent), style = Grapho.type.caption, color = GraphoWheel.textMuted)
                Row {
                    for (argb in state.recent) {
                        val recentLabel = stringResource(R.string.colour_recent_item, hexOf(argb))
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .pressable { pushArgb(argb) }
                                .semantics { contentDescription = recentLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(Color(argb)))
                        }
                    }
                }

                if (!editing && book.sets.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for ((i, set) in book.sets.withIndex()) {
                            val on = i == target
                            Box(
                                Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (on) GraphoWheel.text else GraphoWheel.selectedFill)
                                    .pressable { target = i }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    set.name,
                                    style = Grapho.type.caption,
                                    color = if (on) GraphoWheel.body else GraphoWheel.text,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (editing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PanelButton(stringResource(R.string.action_cancel), filled = false, modifier = Modifier.weight(1f)) { onDone() }
                        PanelButton(stringResource(R.string.action_save), filled = true, modifier = Modifier.weight(1f)) {
                            editSlot?.let { book.replace(it.first, it.second, Color.hsv(hue, sat, value).toArgb()) }
                            onDone()
                        }
                    }
                } else {
                    PanelButton(stringResource(R.string.wheel_manage_colours), filled = false, modifier = Modifier.fillMaxWidth(), onClick = onManage)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PanelButton(stringResource(R.string.wheel_add_to_set, book.sets.getOrNull(target)?.name.orEmpty()), filled = false, modifier = Modifier.weight(1f)) {
                            val argb = Color.hsv(hue, sat, value).toArgb()
                            val at = book.add(target, argb)
                            state.addRecent(argb)
                            onAdded(rimIndexOf(book.sets, target, at))
                        }
                        PanelButton(stringResource(R.string.action_done), filled = true, modifier = Modifier.weight(1f)) {
                            state.addRecent(Color.hsv(hue, sat, value).toArgb())
                            onDone()
                        }
                    }
                }
            }
        }
    }
}

internal fun hexOf(argb: Int): String = String.format(Locale.US, "%06X", argb and 0xFFFFFF)

@Composable
internal fun PanelButton(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (filled) GraphoWheel.text else GraphoWheel.selectedFill)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Grapho.type.label, color = if (filled) GraphoWheel.body else GraphoWheel.text)
    }
}

@Composable
internal fun SatValueSquare(hue: Float, sat: Float, value: Float, onChange: (sat: Float, value: Float) -> Unit) {
    val latest by rememberUpdatedState(onChange)
    val description = stringResource(R.string.colour_sat_brightness)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                fun set(p: Offset) = latest(
                    (p.x / size.width).coerceIn(0f, 1f),
                    1f - (p.y / size.height).coerceIn(0f, 1f),
                )
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    set(down.position)
                    drag(down.id) { change ->
                        change.consume()
                        set(change.position)
                    }
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val p = Offset(sat * size.width, (1f - value) * size.height)
        drawCircle(GraphoWheel.thumbEdge, 10.dp.toPx(), p, style = Stroke(width = 4.dp.toPx()))
        drawCircle(GraphoWheel.text, 9.dp.toPx(), p, style = Stroke(width = 2.dp.toPx()))
    }
}

/** A bar painted by [paint], with a ring at [fraction] that follows the finger. */
@Composable
internal fun BarSlider(
    fraction: Float,
    label: String,
    onChange: (Float) -> Unit,
    paint: DrawScope.() -> Unit,
) {
    val latest by rememberUpdatedState(onChange)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                fun set(p: Offset) = latest((p.x / size.width).coerceIn(0f, 1f))
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    set(down.position)
                    drag(down.id) { change ->
                        change.consume()
                        set(change.position)
                    }
                }
            },
    ) {
        paint()
        val x = (fraction * size.width).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
        drawCircle(GraphoWheel.text, 12.dp.toPx(), Offset(x, size.height / 2f), style = Stroke(width = 3.dp.toPx()))
    }
}

internal fun DrawScope.drawChecker(sq: Float) {
    drawRect(GraphoWheel.checkerDark)
    var row = 0
    var y = 0f
    while (y < size.height) {
        var col = 0
        var x = 0f
        while (x < size.width) {
            if ((row + col) % 2 == 0) drawRect(GraphoWheel.checkerLight, Offset(x, y), Size(sq, sq))
            x += sq
            col++
        }
        y += sq
        row++
    }
}
