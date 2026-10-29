package com.onemnemo.grapho.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.FieldRow
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem

/** The name of a place: the folder's, or Library for the top level. */
@Composable
fun MoveTarget.displayName(): String = folder?.name ?: stringResource(R.string.library_title)

@StringRes
private fun blockLabel(block: MoveBlock): Int = when (block) {
    MoveBlock.Here -> R.string.library_move_here
    MoveBlock.Self -> R.string.library_move_self
    MoveBlock.Inside -> R.string.library_move_inside
}

/**
 * The places as menu rows, folders indented under the folder they are in. A place that can't be picked says why on
 * its right. [selected] marks the place that is chosen now, for a menu that sets a location.
 */
@Composable
fun MoveTargetItems(
    targets: List<MoveTarget>,
    markSelected: Boolean = false,
    selected: String? = null,
    onPick: (String?) -> Unit,
) {
    for (target in targets) {
        GraphoMenuItem(
            text = INDENT.repeat((target.depth - 1).coerceAtLeast(0)) + target.displayName(),
            onClick = { onPick(target.id) },
            enabled = target.enabled,
            checked = markSelected && target.id == selected,
            icon = R.drawable.ic_folder,
            value = target.block?.let { stringResource(blockLabel(it)) },
        )
    }
}

/** The page of a menu that picks where to move to, with a row at the top to go back to the menu. */
@Composable
fun MovePage(targets: List<MoveTarget>, onBack: () -> Unit, onPick: (String?) -> Unit) {
    GraphoMenuItem(stringResource(R.string.library_move_to), icon = R.drawable.ic_chevron_left, onClick = onBack)
    GraphoMenuDivider()
    MoveTargetItems(targets, onPick = onPick)
}

/** The Location row of a sheet. It shows where the new thing will go and opens a menu of folders to change it. */
@Composable
fun LocationRow(targets: List<MoveTarget>, selected: String?, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = targets.firstOrNull { it.id == selected } ?: targets.firstOrNull()
    Box {
        FieldRow(
            label = stringResource(R.string.library_location),
            value = current?.displayName(),
            onClick = { open = true },
        )
        GraphoMenu(expanded = open, onDismiss = { open = false }) {
            MoveTargetItems(targets, markSelected = true, selected = selected) { id ->
                open = false
                onPick(id)
            }
        }
    }
}

/** An em space, which indents a folder name by one level inside a menu row. */
private const val INDENT = " "
