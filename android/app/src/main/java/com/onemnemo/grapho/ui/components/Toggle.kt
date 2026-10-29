package com.onemnemo.grapho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The on-track mixes the text colour into a raised surface, so a column of switches stays calm. */
private const val ON_TRACK_MIX = 0.38f

/**
 * A switch in a hit slot. [label] names it for assistive tech when no visible text sits beside it. Use
 * [GraphoSwitchRow] when there is a visible label, so the whole row is the target.
 */
@Composable
fun GraphoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = rememberControlInteraction()
    Box(
        modifier = modifier
            .size(width = Grapho.metrics.sizeSwitchWidth, height = Grapho.metrics.sizeHitMin)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction.source,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SwitchVisual(checked, enabled, interaction.focused)
    }
}

/**
 * A label with a switch on the right, and a quiet [hint] under the label when the name alone is not enough.
 * The whole row toggles, and is at least a hit slot tall.
 */
@Composable
fun GraphoSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String? = null,
) {
    val interaction = rememberControlInteraction()
    Row(
        modifier = modifier
            .heightIn(min = Grapho.metrics.sizeHitMin)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interaction.source,
                indication = null,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = Grapho.type.body,
                color = if (enabled) Grapho.colors.textDefault else Grapho.colors.textDisabled,
            )
            if (hint != null) Text(hint, style = Grapho.type.caption, color = Grapho.colors.textSubtle)
        }
        SwitchVisual(checked, enabled, interaction.focused)
    }
}

@Composable
internal fun SwitchVisual(checked: Boolean, enabled: Boolean, focused: Boolean) {
    val c = Grapho.colors
    val width = Grapho.metrics.sizeSwitchWidth
    val height = Grapho.metrics.sizeSwitchHeight
    val thumb = Grapho.metrics.sizeThumb
    val gap = (height - thumb) / 2
    val spec = motionTween<Dp>(GraphoMotion.motionDurationBaseMs)
    val x by animateDpAsState(if (checked) width - thumb - gap else gap, spec, label = "switchThumb")
    val track by animateColorAsState(
        when {
            !enabled -> c.stateDisabledBg
            checked -> lerp(c.surfaceStrong, c.textDefault, ON_TRACK_MIX)
            else -> c.selectionFill
        },
        motionTween(GraphoMotion.motionDurationBaseMs),
        label = "switchTrack",
    )
    val knob = when {
        !enabled -> c.stateDisabledFg
        checked -> c.textDefault
        else -> c.textMuted
    }
    Box(
        modifier = Modifier
            .size(width, height)
            .focusRing(focused, GraphoShapes.pill)
            .clip(GraphoShapes.pill)
            .background(track)
            .then(
                if (checked) {
                    Modifier
                } else {
                    Modifier.border(
                        GraphoSpace.borderWidthHairline,
                        c.borderControl,
                        GraphoShapes.pill,
                    )
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = x).size(thumb).clip(CircleShape).background(knob))
    }
}
