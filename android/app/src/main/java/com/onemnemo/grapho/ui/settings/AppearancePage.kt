package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.OptionTile
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.theme.DensitySetting
import com.onemnemo.grapho.ui.theme.GraphoSpace
import com.onemnemo.grapho.ui.theme.ThemeFamily
import com.onemnemo.grapho.ui.theme.ThemeMode

@StringRes
private fun familyName(family: ThemeFamily): Int = when (family) {
    ThemeFamily.Ink -> R.string.theme_ink
    ThemeFamily.Graphite -> R.string.theme_graphite
    ThemeFamily.Paper -> R.string.theme_paper
}

@StringRes
private fun modeName(mode: ThemeMode): Int = when (mode) {
    ThemeMode.System -> R.string.mode_system
    ThemeMode.Light -> R.string.mode_light
    ThemeMode.Dark -> R.string.mode_dark
}

@StringRes
private fun densityName(density: DensitySetting): Int = when (density) {
    DensitySetting.Automatic -> R.string.density_automatic
    DensitySetting.Touch -> R.string.density_touch
    DensitySetting.Compact -> R.string.density_compact
}

/** Theme, mode and density. Each choice repaints the app as it is made. */
@Composable
internal fun AppearancePage(settings: AppSettings) {
    val appearance = settings.appearance
    val systemDark = isSystemInDarkTheme()
    SettingsPage {
        SettingsGroup(R.string.settings_theme) {
            // Each tile previews its family in the mode that would apply, so the picture matches what you get.
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            ) {
                for (family in ThemeFamily.entries) {
                    val look = appearance.copy(family = family)
                    OptionTile(
                        label = stringResource(familyName(family)),
                        selected = appearance.family == family,
                        onClick = { settings.changeAppearance(look) },
                        modifier = Modifier.weight(1f),
                    ) { ThemePreview(look.colors(systemDark)) }
                }
            }
            FieldRow(stringResource(R.string.settings_mode)) {
                SegmentedControl(
                    ThemeMode.entries.map { it to stringResource(modeName(it)) },
                    appearance.mode,
                    { settings.changeAppearance(appearance.copy(mode = it)) },
                )
            }
            FieldRow(stringResource(R.string.settings_density)) {
                SegmentedControl(
                    DensitySetting.entries.map { it to stringResource(densityName(it)) },
                    appearance.density,
                    { settings.changeAppearance(appearance.copy(density = it)) },
                )
            }
        }
    }
}
