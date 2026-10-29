package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoMenuItem
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

@StringRes
private fun nativeName(tag: String): Int = when (tag) {
    "nb" -> R.string.lang_nb
    "fr" -> R.string.lang_fr
    "de" -> R.string.lang_de
    "es" -> R.string.lang_es
    "hi" -> R.string.lang_hi
    "it" -> R.string.lang_it
    "sv" -> R.string.lang_sv
    else -> R.string.lang_en
}

@StringRes
private fun englishName(tag: String): Int = when (tag) {
    "nb" -> R.string.lang_english_nb
    "fr" -> R.string.lang_english_fr
    "de" -> R.string.lang_english_de
    "es" -> R.string.lang_english_es
    "hi" -> R.string.lang_english_hi
    "it" -> R.string.lang_english_it
    "sv" -> R.string.lang_english_sv
    else -> R.string.lang_english_en
}

/**
 * The app's language: the system default first, then each language under its own name. Choosing one switches
 * the app at once. Before Android 13 the system has no such setting, so the page says so instead.
 */
@Composable
internal fun LanguagePage() {
    val context = LocalContext.current
    val app = remember { AppLanguage(context) }
    var chosen by remember { mutableStateOf(app.current()) }
    if (!app.supported) {
        Text(
            stringResource(R.string.settings_language_old_android),
            style = Grapho.type.body,
            color = Grapho.colors.textMuted,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
        )
        return
    }
    fun choose(tag: String?) {
        chosen = tag
        app.set(tag)
    }
    Column(Modifier.selectableGroup()) {
        GraphoMenuItem(
            stringResource(R.string.settings_language_system),
            checked = chosen == null,
            onClick = { choose(null) },
        )
        for (tag in appLanguageTags) {
            val english = stringResource(englishName(tag))
            val native = stringResource(nativeName(tag))
            GraphoMenuItem(
                native,
                checked = chosen == tag,
                value = english.takeIf { it != native },
                onClick = { choose(tag) },
            )
        }
    }
}
