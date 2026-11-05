package com.onemnemo.grapho.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.floatingSurface
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Quiet while nothing is picked, so it is clear the actions need a choice first. */
private const val IDLE_ALPHA = 0.35f

/**
 * The floating bar at the bottom while choosing. Move to is the one solid action and opens the list of places.
 * Star and Export are icons. Export writes one notebook as it is, and any other choice as one library package.
 * Delete is red and acts at once, with Undo in its toast.
 */
@Composable
fun SelectionBar(
    visible: Boolean,
    count: Int,
    canExport: Boolean,
    canStar: Boolean,
    allStarred: Boolean,
    moveTargets: () -> List<MoveTarget>,
    onExport: () -> Unit,
    onMoveTo: (String?) -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.library_selection_actions)
    var moveOpen by remember { mutableStateOf(false) }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter =
            slideInVertically(tween(GraphoMotion.motionDurationSlowMs, easing = GraphoMotion.motionEasingEnter)) {
                it
            } +
                fadeIn(tween(GraphoMotion.motionDurationFastMs)),
        exit =
            slideOutVertically(tween(GraphoMotion.motionDurationExitMs, easing = GraphoMotion.motionEasingExit)) {
                it
            } +
                fadeOut(tween(GraphoMotion.motionDurationFastMs)),
    ) {
        Row(
            modifier = Modifier
                .floatingSurface(GraphoShapes.pill)
                .pointerInput(Unit) { detectTapGestures { } }
                .graphicsLayer { alpha = if (count == 0) IDLE_ALPHA else 1f }
                .semantics { contentDescription = label }
                .padding(GraphoSpace.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                GraphoButton(
                    stringResource(R.string.library_move_to),
                    onClick = { moveOpen = true },
                    iconRes = R.drawable.ic_folder_move,
                    enabled = count > 0,
                )
                GraphoMenu(expanded = moveOpen, onDismiss = { moveOpen = false }) {
                    MoveTargetItems(moveTargets()) { id ->
                        moveOpen = false
                        onMoveTo(id)
                    }
                }
            }
            GraphoIconButton(
                if (allStarred) R.drawable.ic_star_filled else R.drawable.ic_star,
                stringResource(if (allStarred) R.string.library_unstar else R.string.library_star),
                onStar,
                enabled = canStar,
            )
            if (canExport) {
                GraphoIconButton(R.drawable.ic_file_up, stringResource(R.string.note_export), onExport)
            }
            GraphoButton(
                stringResource(R.string.library_delete),
                onClick = onDelete,
                kind = ButtonKind.Danger,
                iconRes = R.drawable.ic_trash,
                enabled = count > 0,
            )
        }
    }
}
