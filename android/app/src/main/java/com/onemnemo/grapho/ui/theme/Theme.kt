package com.onemnemo.grapho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalGraphoColors = staticCompositionLocalOf { GraphoThemes.inkDark }
val LocalGraphoMetrics = staticCompositionLocalOf { GraphoTouch }
val LocalGraphoDark = staticCompositionLocalOf { true }

/** Entry point for reading the current theme: `Grapho.colors.textDefault`, `Grapho.metrics.sizeBarTop`. */
object Grapho {
    val colors: GraphoColors
        @Composable @ReadOnlyComposable
        get() = LocalGraphoColors.current

    val metrics: GraphoMetrics
        @Composable @ReadOnlyComposable
        get() = LocalGraphoMetrics.current

    /** Whether the chosen variant is a dark one, for the few things drawn differently on dark. */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalGraphoDark.current

    val type: GraphoText get() = GraphoText
}

@Composable
fun GraphoTheme(
    appearance: Appearance = Appearance(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = appearance.isDark(systemDark)
    val c = appearance.colors(systemDark)
    val shapes = Shapes(
        extraSmall = GraphoShapes.item,
        small = GraphoShapes.item,
        medium = GraphoShapes.surface,
        large = GraphoShapes.container,
        extraLarge = GraphoShapes.container,
    )
    CompositionLocalProvider(
        LocalGraphoColors provides c,
        LocalGraphoMetrics provides appearance.metrics(),
        LocalGraphoDark provides dark,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = materialScheme(c, dark), shapes = shapes, content = content)
    }
}

// Only the Material parts in use (menus, dialogs, slider) read this scheme. Roles left unmapped
// keep the stock scheme of the same brightness.
private fun materialScheme(c: GraphoColors, dark: Boolean): ColorScheme {
    val stock = if (dark) darkColorScheme() else lightColorScheme()
    return stock.copy(
        primary = c.actionPrimaryBg,
        onPrimary = c.actionPrimaryFg,
        background = c.surfaceBase,
        onBackground = c.textDefault,
        surface = c.surfaceOverlay,
        onSurface = c.textDefault,
        surfaceVariant = c.surfaceControl,
        onSurfaceVariant = c.textMuted,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = c.surfaceOverlay,
        surfaceContainerLow = c.surfaceOverlay,
        surfaceContainer = c.surfaceOverlay,
        surfaceContainerHigh = c.surfaceOverlay,
        surfaceContainerHighest = c.surfaceStrong,
        outline = c.borderStrong,
        outlineVariant = c.borderSubtle,
        error = c.stateDangerFg,
        scrim = c.scrimDefault,
    )
}
