package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.Hsl
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

@Composable
fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val confirm = {
        val name = value.text.trim()
        if (name.isNotEmpty()) onConfirm(name)
    }
    GraphoDialog(
        title = stringResource(R.string.note_rename),
        onDismiss = onDismiss,
        actions = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(R.string.action_save),
                onClick = confirm,
                size = ButtonSize.Large,
                enabled = value.text.isNotBlank(),
            )
        },
    ) {
        GraphoTextField(
            value = value,
            onValueChange = { value = it },
            focusRequester = focus,
            onDone = confirm,
        )
    }
}

/** Picks a paper colour by hue and lightness, with a preview. */
@Composable
fun PaperColorDialog(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = Grapho.colors
    val start = remember { Hsl.from(initial) }
    var hue by remember { mutableFloatStateOf(start.hue) }
    var lightness by remember { mutableFloatStateOf(start.lightness) }
    // Greys have no hue to start from, so give the hue slider something to show.
    val saturation = if (start.saturation > 0.05f) start.saturation else 0.25f
    val color = Hsl(hue, saturation, lightness).toArgb()
    GraphoDialog(
        title = stringResource(R.string.colour_custom),
        onDismiss = onDismiss,
        actions = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(stringResource(R.string.action_apply), onClick = { onConfirm(color) }, size = ButtonSize.Large)
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(GraphoShapes.key)
                .background(Color(color))
                .border(GraphoSpace.borderWidthHairline, c.borderSubtle, GraphoShapes.key),
        )
        val hueLabel = stringResource(R.string.colour_hue)
        val lightnessLabel = stringResource(R.string.colour_lightness)
        Text(hueLabel, style = Grapho.type.caption, color = c.textMuted)
        GraphoSlider(value = hue, onValueChange = { hue = it }, label = hueLabel, valueRange = 0f..360f)
        Text(lightnessLabel, style = Grapho.type.caption, color = c.textMuted)
        GraphoSlider(value = lightness, onValueChange = { lightness = it }, label = lightnessLabel)
    }
}
