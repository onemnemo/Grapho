package com.onemnemo.grapho.ui.library

import android.content.Context

/** How the library is laid out, and the last searches, in shared preferences. Notebooks themselves are never touched. */
class LibraryPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadView(): LibraryView = LibraryView(
        sortKey = pick(KEY_SORT, SortKey.entries, SortKey.Edited),
        sortDir = pick(KEY_DIR, SortDir.entries, SortDir.Desc),
        group = pick(KEY_GROUP, GroupBy.entries, GroupBy.None),
        mode = pick(KEY_MODE, ViewMode.entries, ViewMode.Grid),
        coverSize = prefs.getInt(KEY_SIZE, LibraryView.DEFAULT_COVER_SIZE).coerceIn(0, 100),
    )

    fun saveView(view: LibraryView) {
        prefs.edit()
            .putString(KEY_SORT, view.sortKey.name)
            .putString(KEY_DIR, view.sortDir.name)
            .putString(KEY_GROUP, view.group.name)
            .putString(KEY_MODE, view.mode.name)
            .putInt(KEY_SIZE, view.coverSize)
            .apply()
    }

    fun loadRecent(): List<String> =
        prefs.getString(KEY_RECENT, null)?.split('\n')?.filter { it.isNotBlank() }?.take(MAX_RECENT) ?: emptyList()

    fun saveRecent(recent: List<String>) {
        prefs.edit().putString(KEY_RECENT, recent.joinToString("\n")).apply()
    }

    private fun <T : Enum<T>> pick(key: String, all: List<T>, default: T): T {
        val stored = prefs.getString(key, null)
        return all.firstOrNull { it.name == stored } ?: default
    }

    private companion object {
        const val PREFS = "grapho_library"
        const val KEY_SORT = "sort"
        const val KEY_DIR = "dir"
        const val KEY_GROUP = "group"
        const val KEY_MODE = "mode"
        const val KEY_SIZE = "size"
        const val KEY_RECENT = "recent"
    }
}
