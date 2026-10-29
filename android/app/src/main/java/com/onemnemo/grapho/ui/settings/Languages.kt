package com.onemnemo.grapho.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

/** The languages the app is set up for, as language tags, in the order the list shows them. */
val appLanguageTags: List<String> = listOf("en", "nb", "fr", "de", "es", "hi", "it", "sv")

/**
 * The listed language that [tags] (the app's chosen locales, most wanted first) comes down to, or null when
 * none is chosen and the system language rules. A regional tag such as nb-NO counts as its language.
 */
fun chosenLanguage(tags: List<String>): String? {
    for (tag in tags) {
        val language = tag.substringBefore('-').substringBefore('_').lowercase()
        if (language in appLanguageTags) return language
    }
    return null
}

/** The per app language of the operating system. Before Android 13 there is no such setting. */
class AppLanguage(private val context: Context) {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** The language chosen for this app, or null for the system language. */
    fun current(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return chosenLanguage((0 until locales.size()).map { locales[it].toLanguageTag() })
    }

    /** Sets the app language, or hands it back to the system with null. The activity restyles itself. */
    fun set(tag: String?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = if (tag == null) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    }
}
