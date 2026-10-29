package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ShapeDetection
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.GraphoSwitchRow
import com.onemnemo.grapho.ui.components.OptionTile
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.note.DockEdge
import com.onemnemo.grapho.ui.theme.GraphoSpace

@StringRes
private fun detectionName(mode: ShapeDetection): Int = when (mode) {
    ShapeDetection.SUGGEST -> R.string.shape_detection_suggest
    ShapeDetection.SNAP_ON_HOLD -> R.string.shape_detection_hold
    ShapeDetection.OFF -> R.string.shape_detection_off
}

/** The edges the wheel can be set to dock on from here. */
private val WHEEL_EDGES = listOf(DockEdge.Left, DockEdge.Right, DockEdge.Bottom)

private fun edgeName(edge: DockEdge): Int = when (edge) {
    DockEdge.Left -> R.string.bar_position_left
    DockEdge.Right -> R.string.bar_position_right
    DockEdge.Bottom -> R.string.bar_position_bottom
    DockEdge.Top -> R.string.bar_position_top
}

/**
 * What writing starts with, and what a stroke that looks like a shape turns into. The wheel can only be customised on a page, so its row shows only while a note is
 * open behind Settings ([onCustomiseWheel] is not null).
 */
@Composable
internal fun PenPage(settings: AppSettings, onCustomiseWheel: (() -> Unit)?) {
    SettingsPage {
        SettingsGroup(R.string.settings_writing) {
            GraphoSwitchRow(
                text = stringResource(R.string.note_draw_with_finger),
                checked = settings.fingerDraws,
                onCheckedChange = settings::changeFingerDraws,
                hint = stringResource(R.string.settings_finger_hint),
                modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
            )
        }
        SettingsGroup(R.string.settings_shape_detection) {
            SegmentedControl(
                ShapeDetection.entries.map { it to stringResource(detectionName(it)) },
                settings.shapeDetection,
                settings::changeShapeDetection,
            )
        }
        SettingsGroup(R.string.settings_pen_wheel) {
            var edge by remember { mutableStateOf(settings.wheelEdge()) }
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            ) {
                for (e in WHEEL_EDGES) {
                    OptionTile(
                        label = stringResource(edgeName(e)),
                        selected = edge == e,
                        onClick = {
                            edge = e
                            settings.changeWheelEdge(e)
                        },
                        modifier = Modifier.weight(1f),
                    ) { WheelPreview(e) }
                }
            }
            if (onCustomiseWheel != null) {
                FieldRow(stringResource(R.string.note_customise_wheel), onClick = onCustomiseWheel)
            }
        }
    }
}
