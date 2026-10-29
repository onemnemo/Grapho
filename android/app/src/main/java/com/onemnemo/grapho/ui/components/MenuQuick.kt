package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The row of icon actions at the top of a [GraphoMenu]. Fill it with [GraphoMenuQuick]. */
@Composable
fun ColumnScope.GraphoMenuQuickRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = GraphoSpace.spaceNudge),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
        content = content,
    )
}

/** One quick action: an icon over its name, sharing the row's width with its neighbours. */
@Composable
fun RowScope.GraphoMenuQuick(text: String, @DrawableRes icon: Int, onClick: () -> Unit) {
    val c = Grapho.colors
    val interaction = rememberControlInteraction()
    val fill by animateColorAsState(
        when {
            interaction.pressed -> c.pressFill
            interaction.hovered -> c.hoverFill
            else -> Color.Transparent
        },
        motionTween(GraphoMotion.motionDurationFastMs),
        label = "menuQuickFill",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .pressable(role = Role.Button, interactionSource = interaction.source, onClick = onClick)
            .focusRing(interaction.focused, GraphoShapes.item, inset = true)
            .clip(GraphoShapes.item)
            .background(fill)
            .padding(top = GraphoSpace.spaceMd, bottom = GraphoSpace.spaceSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs),
    ) {
        GraphoIcon(icon, null, c.textMuted)
        Text(text, style = Grapho.type.micro, color = c.textDefault, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
