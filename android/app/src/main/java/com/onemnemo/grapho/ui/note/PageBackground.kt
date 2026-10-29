package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoPopover
import com.onemnemo.grapho.ui.components.PaperColorRow
import com.onemnemo.grapho.ui.library.PatternRow
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * Page background as a popover from the menu row that opens it: the pattern as four small pages and the paper
 * colour as a row of dots. Choices apply as they are touched, so there is no Done. A custom colour closes the
 * popover and opens its own dialog.
 */
@Composable
internal fun PageBackgroundPopover(
    expanded: Boolean,
    onDismiss: () -> Unit,
    pattern: Int,
    paper: Int,
    onPattern: (Int) -> Unit,
    onPaper: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    GraphoPopover(expanded = expanded, onDismiss = onDismiss) {
        val c = Grapho.colors
        Text(
            stringResource(R.string.canvas_tab_pattern),
            style = Grapho.type.label,
            color = c.textMuted,
            modifier = Modifier.padding(bottom = GraphoSpace.spaceSm),
        )
        PatternRow(pattern = pattern, paperColor = paper, onPick = onPattern)
        Text(
            stringResource(R.string.note_paper_colour),
            style = Grapho.type.label,
            color = c.textMuted,
            modifier = Modifier.padding(top = GraphoSpace.spaceLg, bottom = GraphoSpace.spaceXs),
        )
        PaperColorRow(selected = paper, onPick = onPaper, onCustom = {
            onDismiss()
            onCustom()
        })
    }
}
