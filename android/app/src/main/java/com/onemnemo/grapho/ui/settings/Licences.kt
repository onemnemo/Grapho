package com.onemnemo.grapho.ui.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One licence text shipped in the app: the [name] of what it covers, its [kind], and the asset it is read from. */
data class Licence(val name: String, val kind: String, val asset: String)

private const val LICENCE_DIR = "licenses"

/** Reads `geist-OFL.txt` as Geist under OFL, and `grapho-Apache-2.0.txt` as Grapho under Apache 2.0. */
fun licenceFor(file: String): Licence {
    val stem = file.substringBeforeLast('.')
    val name = stem.substringBefore('-').replaceFirstChar { it.uppercaseChar() }
    val kind = if ('-' in stem) stem.substringAfter('-').replace('-', ' ') else ""
    return Licence(name, kind, "$LICENCE_DIR/$file")
}

private const val OWN_NAME = "Grapho"

/** Grapho's own licence first, then the fonts and icons it ships, by name. */
suspend fun loadLicences(context: Context): List<Licence> = withContext(Dispatchers.IO) {
    context.assets.list(LICENCE_DIR).orEmpty().filter { it.endsWith(".txt") }.map(::licenceFor)
        .sortedWith(compareBy({ it.name != OWN_NAME }, { it.name }))
}

suspend fun loadLicenceText(context: Context, licence: Licence): String = withContext(Dispatchers.IO) {
    context.assets.open(licence.asset).bufferedReader().use { it.readText() }
}
