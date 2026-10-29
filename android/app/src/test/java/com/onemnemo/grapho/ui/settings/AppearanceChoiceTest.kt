package com.onemnemo.grapho.ui.settings

import com.onemnemo.grapho.ui.theme.Appearance
import com.onemnemo.grapho.ui.theme.GraphoThemes
import com.onemnemo.grapho.ui.theme.ThemeFamily
import com.onemnemo.grapho.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceChoiceTest {
    @Test
    fun everyFamilyResolvesToItsRealThemeInBothModes() {
        for (family in ThemeFamily.entries) {
            val light = Appearance(family = family, mode = ThemeMode.Light).colors(systemDark = true)
            val dark = Appearance(family = family, mode = ThemeMode.Dark).colors(systemDark = false)
            assertEquals(GraphoThemes.all.getValue("${family.id}-light"), light)
            assertEquals(GraphoThemes.all.getValue("${family.id}-dark"), dark)
        }
    }

    @Test
    fun theSystemModeFollowsTheDevice() {
        val system = Appearance(family = ThemeFamily.Paper, mode = ThemeMode.System)
        assertEquals(GraphoThemes.paperDark, system.colors(systemDark = true))
        assertEquals(GraphoThemes.paperLight, system.colors(systemDark = false))
    }

    @Test
    fun aTilePreviewKeepsTheModeAndChangesOnlyTheFamily() {
        val current = Appearance(family = ThemeFamily.Ink, mode = ThemeMode.Light)
        val preview = current.copy(family = ThemeFamily.Graphite)
        assertEquals(GraphoThemes.graphiteLight, preview.colors(systemDark = true))
        assertEquals(current.mode, preview.mode)
    }
}
