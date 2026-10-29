package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.ui.settings.AppSettings

/**
 * What the note shares with the app around it: the app wide [settings], opening Settings, the request from
 * Settings to start customising the wheel ([customiseWheel], answered with [onWheelCustomised]), and leaving.
 */
internal class NoteHooks(
    val settings: AppSettings,
    val onSettings: () -> Unit,
    val customiseWheel: Boolean,
    val onWheelCustomised: () -> Unit,
    val onBack: () -> Unit,
)
