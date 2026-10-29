package com.onemnemo.grapho.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.focusRing
import com.onemnemo.grapho.ui.components.motionTween
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.components.rememberControlInteraction
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The list of pages: an icon and a name each, groups apart by space alone. Beside the page the open one holds
 * the selection fill. Stacked on a phone, every row ends in a chevron because a row opens a page.
 */
@Composable
internal fun SettingsNav(
    selected: SettingsSection?,
    stacked: Boolean,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GraphoSpace.spaceMd, vertical = GraphoSpace.spaceSm)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXl),
    ) {
        for (group in settingsGroups) {
            Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge)) {
                for (section in group) {
                    NavRow(section, section == selected, stacked) { onSelect(section) }
                }
            }
        }
    }
}

@Composable
private fun NavRow(section: SettingsSection, isSelected: Boolean, stacked: Boolean, onClick: () -> Unit) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            isSelected -> if (interaction.hovered) c.selectionFillHover else c.selectionFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "settingsNavFill",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Grapho.metrics.sizeHitMin)
            .pressable(role = Role.Tab, scaleOnPress = false, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.item, inset = true)
            .semantics { selected = isSelected }
            .clip(GraphoShapes.item)
            .background(fill)
            .padding(horizontal = GraphoSpace.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIcon(section.icon, null, if (isSelected) c.textDefault else c.textMuted)
        Text(
            stringResource(section.label),
            style = Grapho.type.label,
            color = if (isSelected) c.textDefault else c.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (stacked) GraphoIcon(R.drawable.ic_chevron_right, null, c.textSubtle)
    }
}
