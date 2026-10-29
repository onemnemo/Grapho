package com.onemnemo.grapho.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.COMPACT_WIDTH_DP

/** The pages of Settings that have something behind them. */
enum class SettingsSection(@StringRes val label: Int, @DrawableRes val icon: Int) {
    General(R.string.settings_general, R.drawable.ic_settings),
    Appearance(R.string.settings_appearance, R.drawable.ic_sun),
    Language(R.string.settings_language, R.drawable.ic_globe),
    Pen(R.string.settings_pen, R.drawable.ic_pencil),
    Gestures(R.string.settings_gestures, R.drawable.ic_finger),
    Library(R.string.settings_library, R.drawable.ic_notebook),
    About(R.string.settings_about, R.drawable.ic_info),
}

/** Groups are separated by space in the list, never by a line or a label. */
val settingsGroups: List<List<SettingsSection>> = listOf(
    listOf(
        SettingsSection.General,
        SettingsSection.Appearance,
        SettingsSection.Language,
        SettingsSection.Pen,
        SettingsSection.Gestures,
        SettingsSection.Library,
    ),
    listOf(SettingsSection.About),
)

/** A phone stacks the list and one page; anything from 600 dp wide keeps the list beside the page. */
fun settingsStacked(windowWidthDp: Float): Boolean = windowWidthDp < COMPACT_WIDTH_DP

/**
 * Where Settings is: a page, or the list on a stacked window ([section] null), and whether a view is pushed
 * inside a page: a licence inside About, or the choice of tools inside General ([barTools]).
 */
data class SettingsPlace(val section: SettingsSection?, val licence: Licence? = null, val barTools: Boolean = false) {
    /** A view is pushed over its page, and Back closes it before anything else. */
    val pushed: Boolean get() = licence != null || barTools

    /** What Back or Escape goes to, or null when the next step is to leave Settings. */
    fun back(stacked: Boolean): SettingsPlace? = when {
        licence != null -> SettingsPlace(SettingsSection.About)
        barTools -> SettingsPlace(SettingsSection.General)
        stacked && section != null -> SettingsPlace(null)
        else -> null
    }

    companion object {
        /** A wide window opens on its first page, a stacked one on the list. */
        fun start(stacked: Boolean) = SettingsPlace(if (stacked) null else settingsGroups.first().first())
    }
}
