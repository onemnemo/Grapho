package com.onemnemo.grapho.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.theme.GraphoContent
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val BadgeSize = 28.dp
private val BadgeSizeSmall = 26.dp
private const val HIDDEN_SCALE = 0.5f

/**
 * The round tick on a cover or a row while choosing. Empty while [visible], filled with a check once
 * [selected]. The parent places it. It is drawn over media, so it takes the media colours, not the theme's.
 */
@Composable
fun SelectionBadge(visible: Boolean, selected: Boolean, modifier: Modifier = Modifier, small: Boolean = false) {
    val shown = visible || selected
    val scale by animateFloatAsState(
        if (shown) 1f else HIDDEN_SCALE,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "badgeScale",
    )
    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(GraphoMotion.motionDurationBaseMs),
        label = "badgeAlpha",
    )
    val fill by animateColorAsState(
        if (selected) GraphoContent.selectionMediaRing else GraphoContent.mediaGlass,
        tween(GraphoMotion.motionDurationFastMs),
        label = "badgeFill",
    )
    val size: Dp = if (small) BadgeSizeSmall else BadgeSize
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(fill)
            .border(GraphoSpace.borderWidthEmphasis, GraphoContent.selectionMediaRing, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) GraphoIcon(R.drawable.ic_check, null, GraphoContent.selectionMediaHalo, size = 16.dp)
    }
}
