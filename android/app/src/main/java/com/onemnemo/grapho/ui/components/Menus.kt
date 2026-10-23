package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val PLATE_SIZE = 28.dp

/**
 * Commands for one trigger, opened under the parent it is declared in. A tap outside or Back calls
 * [onDismiss]. Fill it with [GraphoMenuItem], [GraphoMenuLabel] and [GraphoMenuDivider]. An item that opens
 * something else closes the menu first.
 */
@Composable
fun GraphoMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    AnchoredPanel(
        onDismiss = onDismiss,
        modifier = modifier
            .widthIn(min = GraphoSpace.sizeMenuMinWidth, max = GraphoSpace.sizeMenuMaxWidth)
            .width(IntrinsicSize.Max)
            .padding(GraphoSpace.spaceXs),
        content = content,
    )
}

/** How a row's icon sits: bare, or in a 28 dp slot that is tinted for what you can make and bare for what you bring in. */
enum class MenuPlate { None, Tinted, Bare }

/**
 * One row of a [GraphoMenu]. [checked] marks the current choice with the selection fill and a check on the
 * right. A [toggle] row shows a switch there instead, and a [value] shows the current setting in muted text. A
 * [plate] gives the icon a fixed slot, so two groups of rows keep their labels in line.
 */
@Composable
fun GraphoMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
    checked: Boolean = false,
    @DrawableRes icon: Int? = null,
    plate: MenuPlate = MenuPlate.None,
    value: String? = null,
    toggle: Boolean? = null,
) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val color = when {
        !enabled -> c.textDisabled
        danger -> c.stateDangerFg
        else -> c.textDefault
    }
    val fill by animateColorAsState(
        when {
            !enabled -> Color.Transparent
            interaction.pressed -> c.pressFill
            checked -> if (interaction.hovered) c.selectionFillHover else c.selectionFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "menuItemFill",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Grapho.metrics.sizeRowMenu)
            .pressable(
                enabled = enabled,
                scaleOnPress = false,
                role = if (toggle != null) Role.Switch else Role.Button,
                interactionSource = interaction.source,
                onClick = onClick,
            )
            .focusRing(interaction.focused, GraphoShapes.item, inset = true)
            .semantics {
                if (checked) selected = true
                if (toggle != null) toggleableState = if (toggle) ToggleableState.On else ToggleableState.Off
            }
            .clip(GraphoShapes.item)
            .background(fill)
            .padding(horizontal = GraphoSpace.spaceMd),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && plate != MenuPlate.None) {
            Box(
                Modifier
                    .size(PLATE_SIZE)
                    .clip(GraphoShapes.item)
                    .background(if (plate == MenuPlate.Tinted) c.selectionFill else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                GraphoIcon(icon, null, if (plate == MenuPlate.Tinted) c.textDefault else c.textMuted)
            }
        } else if (icon != null) {
            GraphoIcon(icon, null, if (danger || !enabled) color else c.textMuted)
        }
        Text(
            text,
            style = Grapho.type.label,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) Text(value, style = Grapho.type.caption, color = c.textMuted, maxLines = 1)
        if (toggle != null) SwitchVisual(toggle, enabled, interaction.focused)
        if (checked) GraphoIcon(R.drawable.ic_check, stringResource(R.string.state_selected), c.selectionIndicator)
    }
}

/** The small heading over a group of rows, such as Paper colour. */
@Composable
fun GraphoMenuLabel(text: String) {
    Text(
        text,
        style = Grapho.type.label,
        color = Grapho.colors.textMuted,
        modifier = Modifier.padding(
            start = GraphoSpace.spaceMd,
            end = GraphoSpace.spaceMd,
            top = GraphoSpace.spaceSm,
            bottom = GraphoSpace.spaceXs,
        ),
    )
}

@Composable
fun GraphoMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = GraphoSpace.spaceXs, vertical = GraphoSpace.spaceXs)
            .height(GraphoSpace.borderWidthHairline)
            .background(Grapho.colors.borderSubtle),
    )
}
