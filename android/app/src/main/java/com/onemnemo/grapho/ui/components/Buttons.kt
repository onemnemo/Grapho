package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * Primary is the one solid white action on a surface. Secondary is the quiet default, Quiet has no fill until
 * touched, and Danger tints red for a destructive verb.
 */
enum class ButtonKind { Primary, Secondary, Quiet, Danger }

/**
 * Heights come from the control tokens, so a density change resizes every button. Large is for the one primary
 * action of a sheet, a dialog or a first-run screen, and never goes inside a bar. Everything else is Default.
 */
enum class ButtonSize { Small, Default, Large }

@Composable
fun GraphoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    size: ButtonSize = ButtonSize.Default,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val resting = when (kind) {
        ButtonKind.Primary -> c.actionPrimaryBg
        ButtonKind.Secondary -> c.actionSecondaryBg
        ButtonKind.Quiet -> c.actionQuietBg
        ButtonKind.Danger -> c.stateDangerBg
    }
    val hover = when (kind) {
        ButtonKind.Primary -> c.actionPrimaryBgHover
        ButtonKind.Secondary -> c.actionSecondaryBgHover
        ButtonKind.Quiet -> c.actionQuietBgHover
        ButtonKind.Danger -> c.stateDangerBgHover
    }
    val pressed = when (kind) {
        ButtonKind.Primary -> c.actionPrimaryBgPressed
        ButtonKind.Secondary -> c.actionSecondaryBgPressed
        ButtonKind.Quiet -> c.actionQuietBgPressed
        ButtonKind.Danger -> c.stateDangerBgPressed
    }
    val disabledBg = if (kind == ButtonKind.Primary) c.actionPrimaryBgDisabled else c.stateDisabledBg
    val background = when {
        !enabled -> if (kind == ButtonKind.Quiet) Color.Transparent else disabledBg
        interaction.pressed -> pressed
        interaction.hovered -> hover
        else -> resting
    }
    val foreground = when {
        !enabled -> if (kind == ButtonKind.Primary) c.actionPrimaryFgDisabled else c.stateDisabledFg

        else -> when (kind) {
            ButtonKind.Primary -> c.actionPrimaryFg
            ButtonKind.Secondary -> c.actionSecondaryFg
            ButtonKind.Quiet -> c.actionQuietFg
            ButtonKind.Danger -> c.stateDangerFg
        }
    }
    val fill by animateColorAsState(background, motionTween(GraphoMotion.motionDurationFastMs), label = "buttonFill")
    val height = when (size) {
        ButtonSize.Small -> Grapho.metrics.sizeControlSmall
        ButtonSize.Default -> Grapho.metrics.sizeControlDefault
        ButtonSize.Large -> Grapho.metrics.sizeControlLarge
    }
    val iconSize = if (size == ButtonSize.Small) Grapho.metrics.sizeIconSmall else Grapho.metrics.sizeIconDefault
    Row(
        modifier = modifier
            .pressable(enabled = enabled, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.pill)
            .heightIn(min = height)
            .clip(GraphoShapes.pill)
            .background(fill)
            .padding(horizontal = buttonPadding(kind, size)),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) GraphoIcon(iconRes, null, foreground, size = iconSize)
        Text(text, style = Grapho.type.label, color = foreground, maxLines = 1)
    }
}

private fun buttonPadding(kind: ButtonKind, size: ButtonSize): Dp = when {
    kind == ButtonKind.Quiet -> GraphoSpace.spaceMd
    size == ButtonSize.Small -> GraphoSpace.spaceLg
    size == ButtonSize.Large -> GraphoSpace.space2xl
    else -> GraphoSpace.spaceXl
}
