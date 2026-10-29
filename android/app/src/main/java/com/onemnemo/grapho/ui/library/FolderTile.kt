package com.onemnemo.grapho.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LibraryFolder
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.FolderColors
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** A picked folder shrinks a little and gets a ring with a gap, like a picked cover. */
private const val PICKED_SCALE = 0.94f

/** Folders the person did not pick step back while choosing. */
private const val DIM_ALPHA = 0.55f

private val RingGap = 3.dp
private val RingWidth = 3.dp
private val TileWidth = 176.dp

/** The name and the count stop short of the dots button. */
private val TextWidth = 108.dp
private const val META_ALPHA = 0.7f

/** A folder in a row of folders: the folder art with its name and what it holds on the front, and its dots. */
@Composable
fun FolderTile(
    folder: LibraryFolder,
    counts: FolderCounts,
    selecting: Boolean,
    selected: Boolean,
    actions: FolderActions,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = Grapho.colors.selectionIndicator
    val meta = folderMeta(counts)
    val scale by animateFloatAsState(
        if (selected) PICKED_SCALE else 1f,
        tween(GraphoMotion.motionDurationBaseMs, easing = GraphoMotion.motionEasingStandard),
        label = "folderScale",
    )
    val dim by animateFloatAsState(
        if (selecting && !selected) DIM_ALPHA else 1f,
        tween(GraphoMotion.motionDurationBaseMs),
        label = "folderDim",
    )
    FolderArt(
        colour = folderColour(folder.id),
        modifier = modifier
            .pressable(
                role = if (selecting) Role.Checkbox else Role.Button,
                onLongClick = if (selecting) null else onLongClick,
                onClick = onClick,
            )
            .semantics { if (selecting) this.selected = selected }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = dim
            }
            .drawBehind {
                if (selected) {
                    val grow = (RingGap + RingWidth / 2).toPx()
                    drawRoundRect(
                        ring,
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(GraphoSpace.radiusItem.toPx() + grow),
                        style = Stroke(RingWidth.toPx()),
                    )
                }
            },
    ) {
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = GraphoSpace.spaceLg, bottom = GraphoSpace.spaceLg),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
        ) {
            Text(
                folder.name,
                style = Grapho.type.label,
                color = FolderColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(TextWidth),
            )
            Text(
                meta,
                style = Grapho.type.caption,
                color = FolderColors.ink.copy(alpha = META_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(TextWidth),
            )
        }
        SelectionBadge(
            visible = selecting,
            selected = selected,
            modifier = Modifier.align(Alignment.TopStart).padding(GraphoSpace.spaceMd),
        )
        if (!selecting) {
            FolderMoreButton(actions = actions, onMedia = true, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

/** The "12 items" or "2 folders" line of a folder, or both, or Empty. */
@Composable
fun folderMeta(counts: FolderCounts): String {
    val resources = LocalContext.current.resources
    val items = resources.getQuantityString(R.plurals.library_folder_items, counts.items, counts.items)
    val folders = resources.getQuantityString(R.plurals.library_folder_subfolders, counts.folders, counts.folders)
    return when {
        counts.items > 0 && counts.folders > 0 -> stringResource(R.string.library_meta, items, folders)
        counts.items > 0 -> items
        counts.folders > 0 -> folders
        else -> stringResource(R.string.library_folder_empty)
    }
}

/**
 * The heading over the folders and a row of their tiles that scrolls sideways. [onSeeAll] adds the link to All
 * folders, which Home has and a folder does not.
 */
@Composable
fun FolderStrip(
    folders: List<LibraryFolder>,
    picks: Picks,
    countsOf: (LibraryFolder) -> FolderCounts,
    actionsFor: (LibraryFolder) -> FolderActions,
    onOpen: (LibraryFolder) -> Unit,
    onPick: (LibraryFolder) -> Unit,
    onSeeAll: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.library_folders),
                style = Grapho.type.heading,
                color = Grapho.colors.textMuted,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            if (onSeeAll != null) {
                GraphoButton(
                    stringResource(R.string.library_see_all),
                    onClick = onSeeAll,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    kind = ButtonKind.Quiet,
                    size = ButtonSize.Small,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
            contentPadding = PaddingValues(vertical = GraphoSpace.spaceSm),
        ) {
            items(folders, key = { it.id }) { folder ->
                FolderTile(
                    folder = folder,
                    counts = countsOf(folder),
                    selecting = picks.selecting,
                    selected = picks.hasKey(folderKey(folder.id)),
                    actions = actionsFor(folder),
                    onClick = { if (picks.selecting) onPick(folder) else onOpen(folder) },
                    onLongClick = { onPick(folder) },
                    modifier = Modifier.width(TileWidth),
                )
            }
        }
    }
}
