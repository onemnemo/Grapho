package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ColorSwatch
import com.onemnemo.grapho.ui.components.HueSweep
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** New makes something and Edit changes what is there. Both use the same sheet. */
enum class SheetMode { New, Edit }

/** The live preview on the left and the fields beside it, which every look sheet starts with. */
@Composable
fun SheetTop(preview: @Composable () -> Unit, fields: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { preview() }
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) { fields() }
    }
}

/** A choice with a picture and a name under it. The chosen one holds the selection fill and a ring. */
@Composable
fun PresetTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val c = Grapho.colors
    Column(
        modifier = modifier
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(GraphoShapes.surface)
                .background(if (selected) c.selectionFill else c.surfaceControl)
                .border(
                    if (selected) GraphoSpace.borderWidthEmphasis else GraphoSpace.borderWidthHairline,
                    if (selected) c.selectionIndicator else c.borderSubtle,
                    GraphoShapes.surface,
                )
                .padding(GraphoSpace.spaceSm),
            contentAlignment = Alignment.Center,
        ) { preview() }
        Text(
            label,
            style = Grapho.type.caption,
            color = if (selected) c.textDefault else c.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Colour dots in as many columns as the width holds. [onCustom] adds a last dot that opens a picker. */
@Composable
fun SwatchGrid(
    colors: List<Color>,
    isSelected: (Color) -> Boolean,
    onPick: (Color) -> Unit,
    modifier: Modifier = Modifier,
    custom: Color? = null,
    onCustom: (() -> Unit)? = null,
) {
    val ring = Grapho.colors.selectionIndicator
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val hit = Grapho.metrics.sizeHitMin
        val columns = (maxWidth / hit).toInt().coerceIn(1, MAX_SWATCH_COLUMNS)
        val cells = colors.size + if (onCustom != null) 1 else 0
        Column {
            for (rowStart in 0 until cells step columns) {
                Row {
                    for (i in rowStart until minOf(rowStart + columns, cells)) {
                        if (i < colors.size) {
                            val color = colors[i]
                            ColorSwatch(
                                color = color,
                                selected = isSelected(color),
                                ringColor = ring,
                                label = stringResource(R.string.library_colour_n, i + 1),
                                onClick = { onPick(color) },
                            )
                        } else {
                            ColorSwatch(
                                color = custom ?: Color.Gray,
                                selected = custom != null,
                                ringColor = ring,
                                label = stringResource(R.string.lasso_custom_colour),
                                onClick = { onCustom?.invoke() },
                                brush = if (custom == null) HueSweep else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_SWATCH_COLUMNS = 8

/** The small grey heading over a group of options in a sheet. */
@Composable
fun FieldLabel(text: String) {
    Text(text, style = Grapho.type.caption, color = Grapho.colors.textMuted)
}
