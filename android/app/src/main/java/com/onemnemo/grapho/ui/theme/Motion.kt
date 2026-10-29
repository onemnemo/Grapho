package com.onemnemo.grapho.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf

/** True when the person turned system animations off. Read once by the activity and provided by [GraphoTheme]. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * True while the pen is down on the page. Nothing animates then, so a stroke is never competing with chrome
 * for a frame. The note screen provides it when it can report a stroke in progress.
 */
val LocalStrokeInProgress = staticCompositionLocalOf { false }

/** The system animator scale is 0 when animations are off, and that is the reduced motion setting. */
fun isReducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

fun readReducedMotion(context: Context): Boolean = isReducedMotion(
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
)
