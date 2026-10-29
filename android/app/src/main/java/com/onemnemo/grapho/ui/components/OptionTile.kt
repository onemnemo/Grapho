package com.onemnemo.grapho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * One choice shown as a picture with its name under it, such as a theme or a bar style. The chosen tile holds
 * the selection fill and a check, with no border. Several go in a row inside a selectable group.
 */
@Composable
fun OptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visual: @Composable () -> Unit,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            selected -> if (interaction.hovered) c.selectionFillHover else c.selectionFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "optionTileFill",
    )
    val tick by animateFloatAsState(
        if (selected) 1f else 0f,
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "optionTileTick",
    )
    Column(
        modifier = modifier
            .pressable(role = Role.RadioButton, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.surface)
            .semantics { this.selected = selected }
            .clip(GraphoShapes.surface)
            .background(fill)
            .padding(GraphoSpace.spaceSm),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
    ) {
        visual()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = GraphoSpace.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = Grapho.type.label,
                color = if (selected) c.textDefault else c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            GraphoIcon(R.drawable.ic_check, null, c.selectionIndicator, Modifier.alpha(tick))
        }
    }
}
