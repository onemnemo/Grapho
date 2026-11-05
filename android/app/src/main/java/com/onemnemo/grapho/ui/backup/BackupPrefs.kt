package com.onemnemo.grapho.ui.backup

import android.content.Context
import android.net.Uri

/** The backup choices in shared preferences: the folder, whether it runs by itself, and when it last did. */
class BackupPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var folder: Uri?
        get() = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse)
        set(value) = prefs.edit().putString(KEY_FOLDER, value?.toString()).apply()

    var auto: Boolean
        get() = prefs.getBoolean(KEY_AUTO, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    /** When the last backup finished, or 0 when there has not been one. */
    var lastBackupMs: Long
        get() = prefs.getLong(KEY_LAST, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST, value).apply()

    /** The newest notebook change that the last backup held, so an unchanged library is not copied again. */
    var lastCoveredMs: Long
        get() = prefs.getLong(KEY_COVERED, 0L)
        set(value) = prefs.edit().putLong(KEY_COVERED, value).apply()

    private companion object {
        const val PREFS = "grapho_backup"
        const val KEY_FOLDER = "folder"
        const val KEY_AUTO = "auto"
        const val KEY_LAST = "last"
        const val KEY_COVERED = "covered"
    }
}
