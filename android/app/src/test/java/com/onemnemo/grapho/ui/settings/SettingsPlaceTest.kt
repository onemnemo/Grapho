package com.onemnemo.grapho.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPlaceTest {
    private val licence = Licence("Geist", "OFL", "licenses/geist-OFL.txt")

    @Test
    fun everySectionIsListedOnceAndAboutIsApart() {
        val listed = settingsGroups.flatten()
        assertEquals(SettingsSection.entries.toSet(), listed.toSet())
        assertEquals(listed.size, listed.toSet().size)
        assertEquals(listOf(SettingsSection.About), settingsGroups.last())
    }

    @Test
    fun windowsUnderSixHundredStack() {
        assertTrue(settingsStacked(360f))
        assertTrue(settingsStacked(599.9f))
        assertFalse(settingsStacked(600f))
        assertFalse(settingsStacked(1280f))
    }

    @Test
    fun aWideWindowOpensOnTheFirstPageAndANarrowOneOnTheList() {
        assertEquals(SettingsSection.General, SettingsPlace.start(stacked = false).section)
        assertNull(SettingsPlace.start(stacked = true).section)
    }

    @Test
    fun backClosesAPushedViewBeforeAnythingElse() {
        val pushed = SettingsPlace(SettingsSection.About, licence)
        assertEquals(SettingsPlace(SettingsSection.About), pushed.back(stacked = true))
        assertEquals(SettingsPlace(SettingsSection.About), pushed.back(stacked = false))
    }

    @Test
    fun backClosesTheToolsViewToGeneral() {
        val tools = SettingsPlace(SettingsSection.General, barTools = true)
        assertTrue(tools.pushed)
        assertEquals(SettingsPlace(SettingsSection.General), tools.back(stacked = true))
        assertEquals(SettingsPlace(SettingsSection.General), tools.back(stacked = false))
    }

    @Test
    fun generalIsFirstInTheList() {
        assertEquals(SettingsSection.General, settingsGroups.first().first())
    }

    @Test
    fun backFromAPageGoesToTheListOnlyWhenStacked() {
        val page = SettingsPlace(SettingsSection.Language)
        assertEquals(SettingsPlace(null), page.back(stacked = true))
        assertNull(page.back(stacked = false))
    }

    @Test
    fun backFromTheListLeavesSettings() {
        assertNull(SettingsPlace(null).back(stacked = true))
    }
}
