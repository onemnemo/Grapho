package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.onemnemo.grapho.ui.theme.GraphoMotion

private const val PRESS_SCALE = 0.97f

/**
 * Click handling without a ripple. Pressed controls shrink slightly instead,
 * which is the feedback the design asks for. Pass [interactionSource] to read the
 * pressed, hovered and focused state from outside.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    scaleOnPress: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val calm = motionCalm()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && scaleOnPress && !calm) PRESS_SCALE else 1f,
        animationSpec = motionTween(GraphoMotion.motionDurationPressMs),
        label = "press",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .hoverable(source, enabled)
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}
