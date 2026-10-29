package com.onemnemo.grapho.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The cover's outer corner is the item radius, its bound edge the key radius, like a book. */
internal val CoverShape = RoundedCornerShape(
    topStart = GraphoSpace.radiusKey,
    bottomStart = GraphoSpace.radiusKey,
    topEnd = GraphoSpace.radiusItem,
    bottomEnd = GraphoSpace.radiusItem,
)

/** A picked cover shrinks a little and gets a ring with a gap, so it reads without relying on colour. */
private const val PICKED_SCALE = 0.94f

/** Covers the person did not pick step back while choosing. */
private const val DIM_ALPHA = 0.55f

/** Width over height of every cover frame. */
const val COVER_RATIO = 3f / 4f

private val TILE_ELEVATION = 6.dp
private val RingGap = 3.dp
private val RingWidth = 3.dp

/** A notebook, canvas or PDF in the grid: real cover art in a 3:4 frame, its name, and one quiet line. */
@Composable
fun CoverTile(
    entry: NotebookEntry,
    selecting: Boolean,
    selected: Boolean,
    query: String,
    actions: ItemActions,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    val name = entry.displayName()
    val meta = metaLabel(entry)
    val scale by animateFloatAsState(
        if (selected) PICKED_SCALE else 1f,
        tween(GraphoMotion.motionDurationBaseMs, easing = GraphoMotion.motionEasingStandard),
        label = "tileScale",
    )
    val dim by animateFloatAsState(
        if (selecting && !selected) DIM_ALPHA else 1f,
        tween(GraphoMotion.motionDurationBaseMs),
        label = "tileDim",
    )
    val ring = c.selectionIndicator
    Column(
        modifier = modifier
            .pressable(
                role = if (selecting) Role.Checkbox else Role.Button,
                onLongClick = if (selecting) null else onLongClick,
                onClick = onClick,
            )
            .semantics { if (selecting) this.selected = selected },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_RATIO)
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
            Box(
                Modifier
                    .matchParentSize()
                    .shadow(TILE_ELEVATION, CoverShape)
                    .clip(CoverShape),
            ) {
                when (entry.kind) {
                    EntryKind.Notebook -> CoverArt(CoverPalette.resolve(entry.coverColor), name)
                    EntryKind.Pdf -> PdfArt()
                    EntryKind.Canvas -> CanvasArt(entry.background)
                }
            }
            SelectionBadge(
                visible = selecting,
                selected = selected,
                modifier = Modifier.align(Alignment.TopStart).padding(GraphoSpace.spaceMd),
            )
            if (entry.starred) StarMark(Modifier.align(Alignment.TopEnd).padding(GraphoSpace.spaceMd))
            if (!selecting) {
                ItemMoreButton(
                    kind = entry.kind,
                    actions = actions,
                    onMedia = true,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
        Spacer(Modifier.height(GraphoSpace.spaceMd))
        Text(
            highlighted(name, query),
            style = Grapho.type.label,
            color = if (selecting && !selected) c.textSubtle else c.textDefault,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            meta,
            style = Grapho.type.caption,
            color = c.textSubtle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
