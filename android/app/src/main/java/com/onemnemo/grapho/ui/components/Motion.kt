package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.LocalReducedMotion
import com.onemnemo.grapho.ui.theme.LocalStrokeInProgress

/** True when motion must not run: reduced motion is on, or the pen is down. State then shows by fill and colour. */
@Composable
@ReadOnlyComposable
fun motionCalm(): Boolean = LocalReducedMotion.current || LocalStrokeInProgress.current

/** A token duration, which is 0 whenever [motionCalm]. */
@Composable
@ReadOnlyComposable
fun motionDurationMs(tokenMs: Int): Int = if (motionCalm()) 0 else tokenMs

/** The enter curve, or the standard one under reduced motion. */
@Composable
@ReadOnlyComposable
fun motionEnterEasing(): Easing =
    if (LocalReducedMotion.current) GraphoMotion.motionEasingStandard else GraphoMotion.motionEasingEnter

/** The one way a component animates. Every component animation goes through it so the calm rule holds. */
@Composable
fun <T> motionTween(tokenMs: Int, easing: Easing = GraphoMotion.motionEasingStandard): TweenSpec<T> =
    tween(motionDurationMs(tokenMs), easing = easing)
