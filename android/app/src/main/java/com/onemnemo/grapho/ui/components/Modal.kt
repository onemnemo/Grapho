package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.onemnemo.grapho.ui.theme.Grapho

/**
 * The window and scrim behind a Sheet or Dialog. The platform dim is turned off so the scrim is the token one.
 * Back and a tap on the scrim ([dismissOnScrim]) both call [onDismiss].
 */
@Composable
internal fun ModalLayer(
    onDismiss: () -> Unit,
    dismissOnScrim: Boolean,
    content: @Composable BoxScope.(width: Dp, height: Dp) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Grapho.colors.scrimDefault)
                .pointerInput(dismissOnScrim) {
                    detectTapGestures { if (dismissOnScrim) onDismiss() }
                },
        ) {
            content(maxWidth, maxHeight)
        }
    }
}
