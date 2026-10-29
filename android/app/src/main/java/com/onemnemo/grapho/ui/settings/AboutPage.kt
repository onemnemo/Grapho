package com.onemnemo.grapho.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import java.io.IOException

@Suppress("DEPRECATION")
private fun versionName(context: Context): String = try {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        pm.getPackageInfo(context.packageName, 0)
    }
    info.versionName.orEmpty()
} catch (e: PackageManager.NameNotFoundException) {
    ""
}

/** The app's name and version, then the licences of Grapho and of what it ships. A row opens its text. */
@Composable
internal fun AboutPage(onLicence: (Licence) -> Unit) {
    val context = LocalContext.current
    val version = remember { versionName(context) }
    val licences by produceState(emptyList<Licence>()) { value = loadLicences(context) }
    SettingsPage {
        Column(Modifier.padding(horizontal = GraphoSpace.spaceMd)) {
            Text(stringResource(R.string.app_name), style = Grapho.type.display, color = Grapho.colors.textDefault)
            if (version.isNotEmpty()) {
                Text(
                    stringResource(R.string.settings_version, version),
                    style = Grapho.type.body,
                    color = Grapho.colors.textMuted,
                )
            }
        }
        SettingsGroup(R.string.settings_licences) {
            for (licence in licences) {
                val kind = licence.kind.takeIf { it.isNotEmpty() }
                FieldRow(licence.name, value = kind, onClick = { onLicence(licence) })
            }
        }
    }
}

/** One licence text, read from the app's assets. */
@Composable
internal fun LicenceText(licence: Licence) {
    val context = LocalContext.current
    val failed = stringResource(R.string.settings_licence_failed)
    var text by remember(licence) { mutableStateOf("") }
    LaunchedEffect(licence) {
        text = try {
            loadLicenceText(context, licence)
        } catch (e: IOException) {
            failed
        }
    }
    Text(
        text,
        style = Grapho.type.caption,
        color = Grapho.colors.textMuted,
        modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
    )
}
