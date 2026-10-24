package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel

internal const val PANEL_W = 360f

/**
 * The wheel customiser: one calm panel with a Tools segment, for which tools the rings show and in what
 * order, and a Pens segment, for the pen list. It opens on Pens when [startOnPens]. Tapping outside closes it.
 */
@Composable
internal fun CustomisePanel(
    state: ToolbarState,
    tools: WheelToolStore,
    pens: PenStore,
    startOnPens: Boolean,
    progress: Float,
    notices: Notices,
    onDone: () -> Unit,
) {
    var onPens by remember { mutableStateOf(startOnPens) }
    val panelLabel = stringResource(R.string.note_customise_wheel)
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDone,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            Modifier
                .padding(12.dp)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * progress
                    scaleX = s
                    scaleY = s
                    this.alpha = progress
                },
        ) {
            Column(
                modifier = Modifier
                    .width(PANEL_W.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GraphoWheel.body)
                    .padding(14.dp)
                    .semantics { contentDescription = panelLabel },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Segment(stringResource(R.string.wheel_tab_tools), !onPens, Modifier.weight(1f)) { onPens = false }
                    Segment(stringResource(R.string.wheel_tab_pens), onPens, Modifier.weight(1f)) { onPens = true }
                }
                Column(
                    modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (onPens) PensTab(state, pens, notices) else ToolsTab(tools)
                }
                PanelButton(stringResource(R.string.action_done), filled = true, modifier = Modifier.fillMaxWidth(), onClick = onDone)
            }
        }
    }
}

@Composable
private fun Segment(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val description = stringResource(R.string.wheel_tab_description, label)
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) GraphoWheel.text else GraphoWheel.selectedFill)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Grapho.type.label, color = if (on) GraphoWheel.body else GraphoWheel.text)
    }
}
