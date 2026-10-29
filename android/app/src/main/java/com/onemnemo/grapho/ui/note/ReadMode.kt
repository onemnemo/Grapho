package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.TooltipSide
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Where the reader is: the one-based [page] in view out of [count]. */
data class ReadPosition(val page: Int, val count: Int)

/** Pure rules for the position shown in the read mode bar. */
internal object ReadProgress {
    /**
     * The position for the zero-based [current] page of a note with [count] pages, or null when there is nothing
     * to count: an infinite canvas or an empty note. A page outside the note is held to its first or last.
     */
    fun position(current: Int, count: Int, infinite: Boolean): ReadPosition? {
        if (infinite || count <= 0) return null
        return ReadPosition(current.coerceIn(0, count - 1) + 1, count)
    }
}

/** The title and, under it, where the reader is, as the read mode bar shows them. */
@Composable
internal fun ReadTitle(title: String, position: ReadPosition?, modifier: Modifier = Modifier) {
    val c = Grapho.colors
    Column(modifier.padding(horizontal = GraphoSpace.spaceSm)) {
        Text(title, style = Grapho.type.label, color = c.textDefault, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (position != null) {
            Text(
                stringResource(R.string.note_read_page, position.page, position.count),
                style = Grapho.type.micro,
                color = c.textSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * What sits on the far end of the read mode bar: the page count, then the Edit button that leaves read mode. A
 * [vertical] bar is too narrow for either, so it holds Edit as an icon, its tip opening on [tip].
 */
@Composable
internal fun ReadBarActions(
    position: ReadPosition?,
    onEdit: () -> Unit,
    vertical: Boolean = false,
    tip: TooltipSide? = TooltipSide.Below,
) {
    if (vertical) {
        GraphoIconButton(R.drawable.ic_edit, stringResource(R.string.action_edit), onEdit, tooltipSide = tip)
        return
    }
    if (position != null) {
        Text(
            stringResource(R.string.note_read_count, position.page, position.count),
            style = Grapho.type.label,
            color = Grapho.colors.textDefault,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceSm),
        )
    }
    GraphoButton(
        stringResource(R.string.action_edit),
        onClick = onEdit,
        kind = ButtonKind.Primary,
        size = ButtonSize.Small,
        iconRes = R.drawable.ic_edit,
    )
}
