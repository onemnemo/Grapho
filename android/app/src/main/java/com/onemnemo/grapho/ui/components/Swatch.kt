package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes

/**
 * A colour dot centred in a hit slot. The selected one gets a ring with a gap
 * around the dot, so selection never relies on colour alone. A [brush] replaces
 * the flat [color] fill, e.g. for a "pick your own" dot.
 */
@Composable
fun ColorSwatch(
    color: Color,
    selected: Boolean,
    ringColor: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dot: Dp = Grapho.metrics.sizeSwatch,
    brush: Brush? = null,
) {
    val edge = Grapho.colors.borderControl
    Box(
        modifier = modifier
            .size(Grapho.metrics.sizeHitMin)
            .clip(GraphoShapes.pill)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(dot + 10.dp)) {
            val r = dot.toPx() / 2f
            if (brush != null) drawCircle(brush, radius = r) else drawCircle(color, radius = r)
            // A faint edge keeps light colours visible on white and dark ones on black.
            drawCircle(edge, radius = r, style = Stroke(width = 1.dp.toPx()))
            if (selected) {
                drawCircle(ringColor, radius = r + 4.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}
