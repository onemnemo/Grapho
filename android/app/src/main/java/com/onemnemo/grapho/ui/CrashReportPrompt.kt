package com.onemnemo.grapho.ui

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoDialog
import com.onemnemo.grapho.ui.theme.Grapho

/** Reads and clears the report a crash left, which the app's root supplies. */
class CrashReportSource(val pending: () -> String?, val clear: () -> Unit)

/** Asks once after a crash whether to share the saved report. Dismissing it deletes the report. */
@Composable
fun CrashReportPrompt(source: CrashReportSource) {
    val context = LocalContext.current
    var report by remember { mutableStateOf(source.pending()) }
    val text = report ?: return
    val close = {
        source.clear()
        report = null
    }
    GraphoDialog(
        title = stringResource(R.string.crash_title),
        onDismiss = close,
        actions = {
            GraphoButton(stringResource(R.string.action_dismiss), onClick = close, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(R.string.crash_share),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_subject))
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                    close()
                },
                size = ButtonSize.Large,
            )
        },
    ) {
        Text(stringResource(R.string.crash_text), style = Grapho.type.body, color = Grapho.colors.textMuted)
    }
}
