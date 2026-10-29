package com.onemnemo.grapho.ui.theme

import android.content.Context

enum class ThemeFamily(val id: String) {
    Ink("ink"),
    Graphite("graphite"),
    Paper("paper"),
}

enum class ThemeMode(val id: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
}

/** Automatic picks touch or compact from the last input device; the other two are fixed. */
enum class DensitySetting(val id: String) {
    Automatic("automatic"),
    Touch("touch"),
    Compact("compact"),
}

/** What the person chose in Appearance. With nothing chosen, Ink follows the system. */
data class Appearance(
    val family: ThemeFamily = ThemeFamily.Ink,
    val mode: ThemeMode = ThemeMode.System,
    val density: DensitySetting = DensitySetting.Automatic,
) {
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    fun colors(systemDark: Boolean): GraphoColors {
        val scheme = if (isDark(systemDark)) "dark" else "light"
        return GraphoThemes.all.getValue("${family.id}-$scheme")
    }

    /**
     * Automatic currently resolves to touch. Every size in the app reads [GraphoMetrics], and none
     * reads a window or pointer type.
     */
    fun metrics(): GraphoMetrics = when (density) {
        DensitySetting.Compact -> GraphoCompact
        DensitySetting.Automatic, DensitySetting.Touch -> GraphoTouch
    }
}

/** The appearance choice in shared preferences. */
class AppearanceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Appearance = Appearance(
        family = pick(KEY_FAMILY, ThemeFamily.entries, ThemeFamily.Ink) { it.id },
        mode = pick(KEY_MODE, ThemeMode.entries, ThemeMode.System) { it.id },
        density = pick(KEY_DENSITY, DensitySetting.entries, DensitySetting.Automatic) { it.id },
    )

    fun save(appearance: Appearance) {
        prefs.edit()
            .putString(KEY_FAMILY, appearance.family.id)
            .putString(KEY_MODE, appearance.mode.id)
            .putString(KEY_DENSITY, appearance.density.id)
            .apply()
    }

    private fun <T> pick(key: String, all: List<T>, default: T, id: (T) -> String): T {
        val stored = prefs.getString(key, null)
        return all.firstOrNull { id(it) == stored } ?: default
    }

    private companion object {
        const val PREFS = "grapho_appearance"
        const val KEY_FAMILY = "family"
        const val KEY_MODE = "mode"
        const val KEY_DENSITY = "density"
    }
}
