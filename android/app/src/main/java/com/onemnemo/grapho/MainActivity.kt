package com.onemnemo.grapho

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.onemnemo.grapho.ui.GraphoApp
import com.onemnemo.grapho.ui.NotebookStore
import com.onemnemo.grapho.ui.settings.AppSettings
import com.onemnemo.grapho.ui.theme.GraphoTheme
import com.onemnemo.grapho.ui.theme.readReducedMotion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = AppSettings(applicationContext)
        // Read once per launch: the setting lives in system developer and accessibility options.
        val reducedMotion = readReducedMotion(applicationContext)
        setContent {
            val appearance = settings.appearance
            val dark = appearance.isDark(isSystemInDarkTheme())
            // The activity handles theme changes itself, so the bar icons are
            // restyled here rather than on recreation.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            GraphoTheme(appearance, reducedMotion) {
                val store = remember { NotebookStore(applicationContext) }
                GraphoApp(store, settings)
            }
        }
    }
}
