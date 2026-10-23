package com.onemnemo.grapho.ui.components

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

private const val NANOS_PER_MS = 1_000_000L

/** Short messages at the bottom of the screen, at most two at a time. See [ToastQueue] for who makes room. */
@Stable
class Notices {
    private var queue by mutableStateOf(ToastQueue())
    private var counter = 0L

    val visible: List<Notice> get() = queue.visible

    /** Shows [text]. Give [actionLabel] and [onAction] for a toast with one action, such as Undo. */
    fun show(text: String, isError: Boolean = false, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        queue = queue.offer(Notice(text, isError, counter++, actionLabel, onAction))
    }

    fun dismiss(id: Long) {
        queue = queue.remove(id)
    }
}

@Composable
fun NoticeHost(notices: Notices, modifier: Modifier = Modifier) {
    val shown = notices.visible
    if (shown.isEmpty()) return
    Column(
        modifier = modifier.padding(GraphoSpace.spaceLg),
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (notice in shown) key(notice.id) { ToastSlot(notice, notices) }
    }
}

/**
 * One toast and its clock. The clock stops while the toast has focus or a pointer on it, and starts again with
 * what is left, so reading or reaching the action is never cut short. Leaving fades out before it is removed.
 */
@Composable
private fun ToastSlot(notice: Notice, notices: Notices) {
    val context = LocalContext.current
    val total = remember(notice.id) { readingTimeMs(context, notice) }
    var held by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var remaining by remember { mutableLongStateOf(total) }
    val calm = motionCalm()
    val fade = remember { Animatable(if (calm) 1f else 0f) }
    val enter = motionTween<Float>(GraphoMotion.motionDurationBaseMs)
    val exit = motionTween<Float>(GraphoMotion.motionDurationExitMs, GraphoMotion.motionEasingExit)
    LaunchedEffect(Unit) { fade.animateTo(1f, enter) }
    LaunchedEffect(leaving) {
        if (!leaving) return@LaunchedEffect
        fade.animateTo(0f, exit)
        notices.dismiss(notice.id)
    }
    LaunchedEffect(held, leaving) {
        if (held || leaving) return@LaunchedEffect
        val began = System.nanoTime()
        try {
            delay(remaining)
            leaving = true
        } finally {
            remaining = (remaining - (System.nanoTime() - began) / NANOS_PER_MS).coerceAtLeast(0L)
        }
    }
    GraphoToast(
        text = notice.text,
        onDismiss = { leaving = true },
        modifier = Modifier.graphicsLayer { alpha = fade.value },
        isError = notice.isError,
        actionLabel = notice.actionLabel,
        onAction = notice.onAction?.let { action ->
            {
                action()
                leaving = true
            }
        },
        durationMs = if (notice.onAction != null) total.toInt() else 0,
        onHold = { held = it },
    )
}

private fun readingTimeMs(context: Context, notice: Notice): Long {
    val manager = context.getSystemService(AccessibilityManager::class.java)
        ?: return notice.durationMs.toLong()
    val content = AccessibilityManager.FLAG_CONTENT_TEXT or
        if (notice.onAction != null) AccessibilityManager.FLAG_CONTENT_CONTROLS else 0
    return toastReadingMs(
        tokenMs = notice.durationMs,
        systemMs = manager.getRecommendedTimeoutMillis(notice.durationMs, content),
        screenReaderMs = if (manager.isTouchExplorationEnabled) GraphoMotion.feedbackHoldScreenReaderMs else 0,
    )
}
