package com.onemnemo.grapho.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.GraphoSwitchRow
import com.onemnemo.grapho.ui.components.OptionTile
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.note.AutoHide
import com.onemnemo.grapho.ui.note.BarPosition
import com.onemnemo.grapho.ui.note.NoteBarStyle
import com.onemnemo.grapho.ui.theme.GraphoSpace

@StringRes
private fun barName(style: NoteBarStyle): Int = when (style) {
    NoteBarStyle.Floating -> R.string.note_bar_floating
    NoteBarStyle.Line -> R.string.note_bar_line
}

@StringRes
private fun positionName(position: BarPosition): Int = when (position) {
    BarPosition.Top -> R.string.bar_position_top
    BarPosition.Bottom -> R.string.bar_position_bottom
    BarPosition.Left -> R.string.bar_position_left
    BarPosition.Right -> R.string.bar_position_right
}

@StringRes
private fun autoHideName(autoHide: AutoHide): Int = when (autoHide) {
    AutoHide.Off -> R.string.auto_hide_off
    AutoHide.WhileWriting -> R.string.auto_hide_while_writing
    AutoHide.Always -> R.string.auto_hide_always
}

/** Four tiles in a row when each can keep its width, two by two when it cannot. */
private val POSITION_ROW_MIN = GraphoSpace.sizeTileMin * 4

/**
 * The note bar: its style, the edge it sits on, when it hides, whether it shows the note's title, and the tools on it. The tools open their own view
 * ([onEditTools]).
 */
@Composable
internal fun GeneralPage(settings: AppSettings, onEditTools: () -> Unit) {
    SettingsPage {
        SettingsGroup(R.string.settings_note_bar) {
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            ) {
                for (style in NoteBarStyle.entries) {
                    OptionTile(
                        label = stringResource(barName(style)),
                        selected = settings.noteBar == style,
                        onClick = { settings.changeNoteBar(style) },
                        modifier = Modifier.weight(1f),
                    ) { BarPreview(style) }
                }
            }
        }
        SettingsGroup(R.string.settings_bar_position) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val perRow = if (maxWidth >= POSITION_ROW_MIN) BarPosition.entries.size else 2
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm)) {
                    for (row in BarPosition.entries.chunked(perRow)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
                        ) {
                            for (position in row) {
                                OptionTile(
                                    label = stringResource(positionName(position)),
                                    selected = settings.barPosition == position,
                                    onClick = { settings.changeBarPosition(position) },
                                    modifier = Modifier.weight(1f),
                                ) { BarPreview(settings.noteBar, position = position) }
                            }
                        }
                    }
                }
            }
        }
        SettingsGroup(R.string.settings_bar_behaviour) {
            FieldRow(stringResource(R.string.settings_auto_hide)) {
                SegmentedControl(
                    options = AutoHide.entries.map { it to stringResource(autoHideName(it)) },
                    selected = settings.autoHide,
                    onSelect = settings::changeAutoHide,
                    size = SegmentSize.Small,
                )
            }
            GraphoSwitchRow(
                text = stringResource(R.string.settings_show_title),
                checked = settings.showTitle,
                onCheckedChange = settings::changeShowTitle,
                modifier = Modifier.padding(horizontal = GraphoSpace.spaceMd),
            )
            FieldRow(
                stringResource(R.string.settings_bar_tools),
                value = stringResource(R.string.settings_bar_tools_count, settings.barTools.size),
                onClick = onEditTools,
            )
        }
    }
}
