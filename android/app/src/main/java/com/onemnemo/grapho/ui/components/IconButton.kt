package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

/**
 * An icon button in a hit slot, with a soft disc that shows on hover and press. [selected] shows the selection
 * fill and the toggled icon colour, never a solid white disc. [label] names it for assistive tech, and a long
 * press or a hover shows it as a tooltip on [tooltipSide] (null for none), the side away from the edge the
 * button sits on.
 */
@Composable
fun GraphoIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    tooltipSide: TooltipSide? = TooltipSide.Below,
    size: Dp = Grapho.metrics.sizeHitMin,
    tint: Color = if (selected) Grapho.colors.actionToggleFgOn else Grapho.colors.actionQuietFg,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val tip = rememberTooltipState()
    LaunchedEffect(interaction.hovered) {
        if (interaction.hovered) {
            delay(GraphoMotion.feedbackDelayTooltipMs.toLong())
            tip.show()
        } else {
            tip.hide()
        }
    }
    val disc = when {
        !enabled -> Color.Transparent
        interaction.pressed -> c.pressFill
        selected -> if (interaction.hovered) c.selectionFillHover else c.selectionFill
        interaction.hovered -> c.hoverFill
        else -> Color.Transparent
    }
    val fill by animateColorAsState(disc, motionTween(GraphoMotion.motionDurationFastMs), label = "iconButtonFill")
    Box(
        modifier = modifier
            .size(size)
            .pressable(
                enabled = enabled,
                interactionSource = interaction.source,
                onLongClick = if (tooltipSide != null) tip::show else null,
                onClick = onClick,
            )
            .focusRing(interaction.focused, GraphoShapes.pill)
            .semantics {
                contentDescription = label
                if (selected) this.selected = true
            }
            .padding(GraphoSpace.spaceXs)
            .clip(GraphoShapes.pill)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        GraphoIcon(icon, null, if (enabled) tint else c.stateDisabledFg)
        if (tooltipSide != null) GraphoTooltip(label, tip, tooltipSide)
    }
}
