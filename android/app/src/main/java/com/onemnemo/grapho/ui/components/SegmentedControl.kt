package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

enum class SegmentSize { Small, Default }

/**
 * Two to five exclusive choices in one pill. The chosen one holds a soft fill and keeps its label at full
 * strength. [disabled] lists values that cannot be picked right now. A value in [icons] shows that icon in
 * place of its text, and the label then names it for assistive tech only.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    size: SegmentSize = SegmentSize.Default,
    disabled: Set<T> = emptySet(),
    icons: Map<T, Int> = emptyMap(),
) {
    val c = Grapho.colors
    val track = if (size == SegmentSize.Small) Grapho.metrics.sizeControlSmall else Grapho.metrics.sizeControlDefault
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(GraphoShapes.pill)
            .background(c.surfaceControl)
            .padding(GraphoSpace.spaceNudge)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
    ) {
        for ((value, label) in options) {
            Segment(
                label = label,
                isSelected = value == selected,
                enabled = value !in disabled,
                icon = icons[value],
                height = track - GraphoSpace.spaceNudge * 2,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun Segment(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    @DrawableRes icon: Int?,
    height: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            !enabled -> Color.Transparent
            interaction.pressed -> c.pressFill
            isSelected -> c.selectionFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "segmentFill",
    )
    Box(
        modifier = modifier
            .height(height)
            .pressable(
                enabled = enabled,
                role = Role.RadioButton,
                scaleOnPress = false,
                interactionSource = interaction.source,
                onClick = onClick,
            )
            .focusRing(interaction.focused, GraphoShapes.pill, inset = true)
            .semantics {
                selected = isSelected
                if (icon != null) contentDescription = label
            }
            .clip(GraphoShapes.pill)
            .background(fill)
            .padding(horizontal = GraphoSpace.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
        val ink = when {
            !enabled -> c.textDisabled
            isSelected -> c.textDefault
            else -> c.textMuted
        }
        if (icon != null) {
            GraphoIcon(icon, null, ink)
        } else {
            Text(label, style = Grapho.type.label, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
