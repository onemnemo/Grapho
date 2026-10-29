package com.onemnemo.grapho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * Tabs for the pages of one sheet or panel, three to five of them. Nothing is drawn at rest, hover shows a
 * soft fill, and the chosen tab holds the selection fill. [hug] sizes tabs to their labels instead of
 * sharing the width, and scrolls when they do not fit.
 */
@Composable
fun <T> GraphoTabs(
    tabs: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    hug: Boolean = false,
) {
    Row(
        modifier = modifier
            .then(if (hug) Modifier.horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        for ((value, label) in tabs) {
            Tab(
                label = label,
                isSelected = value == selected,
                modifier = if (hug) Modifier else Modifier.weight(1f),
                padding = if (hug) GraphoSpace.spaceLg else GraphoSpace.spaceSm,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun Tab(label: String, isSelected: Boolean, modifier: Modifier, padding: Dp, onClick: () -> Unit) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            isSelected -> c.selectionFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "tabFill",
    )
    Box(
        modifier = modifier
            .height(Grapho.metrics.sizeControlSmall)
            .pressable(role = Role.Tab, scaleOnPress = false, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.pill, inset = true)
            .semantics { selected = isSelected }
            .clip(GraphoShapes.pill)
            .background(fill)
            .padding(horizontal = padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = Grapho.type.label,
            color = if (isSelected) c.textDefault else c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
