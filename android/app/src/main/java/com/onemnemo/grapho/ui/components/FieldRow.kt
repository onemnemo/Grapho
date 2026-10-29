package com.onemnemo.grapho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * A settings row: the [label] and a quiet [hint] under it, and one control. A [value] with a chevron makes the
 * row a button that opens something ([onClick]). A [below] control takes the full width under the label, for
 * a segmented control that needs the room.
 */
@Composable
fun FieldRow(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            onClick == null -> Color.Transparent
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "fieldRowFill",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier
                        .pressable(scaleOnPress = false, interactionSource = interaction.source, onClick = onClick)
                        .focusRing(interaction.focused, GraphoShapes.item, inset = true)
                },
            )
            .clip(GraphoShapes.item)
            .background(fill)
            .padding(horizontal = GraphoSpace.spaceMd),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Grapho.metrics.sizeRowList),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = GraphoSpace.spaceSm)) {
                Text(label, style = Grapho.type.label, color = c.textDefault)
                if (hint != null) Text(hint, style = Grapho.type.caption, color = c.textSubtle)
            }
            if (value != null) Text(value, style = Grapho.type.caption, color = c.textMuted, maxLines = 1)
            if (onClick != null) GraphoIcon(R.drawable.ic_chevron_right, null, c.textSubtle)
        }
        if (below != null) {
            Column(Modifier.fillMaxWidth().padding(bottom = GraphoSpace.spaceSm)) { below() }
        }
    }
}
