package com.onemnemo.grapho.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoMotion
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** A bottom sheet never covers the whole window, so the scrim above it stays a way out. */
private const val BOTTOM_SHEET_MAX_HEIGHT = 0.92f

private val SHEET_ELEVATION = 24.dp

/** A centred sheet grows from this scale while it fades in. */
private const val SHEET_START_SCALE = 0.96f

/** A fixed size for a centred sheet that lays out its own body, such as a list beside a page. */
class SheetFrame(val width: Dp, val height: Dp)

/**
 * A task that needs focus and room: a header with the [title] and a close button, a scrolling body, and an
 * optional [footer] of buttons that stays put. The footer's main action is a [ButtonSize.Large] button. The
 * sheet is pinned to the bottom of a compact window and centred on anything wider, see [sheetPlacement]. A
 * [wide] sheet takes the wide token width. Set [dismissOnScrim] to false once the person has changed
 * something, so a stray tap cannot throw it away. A [frame] gives a centred sheet that size, shrunk to fit the
 * window, and leaves the body's padding and scrolling to its content.
 */
@Composable
fun GraphoSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable RowScope.() -> Unit)? = null,
    dismissOnScrim: Boolean = true,
    wide: Boolean = false,
    frame: SheetFrame? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Grapho.colors
    val calm = motionCalm()
    val enter = motionTween<Float>(GraphoMotion.motionDurationPopMs, motionEnterEasing())
    val progress = remember { Animatable(if (calm) 1f else 0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, enter) }
    ModalLayer(onDismiss, dismissOnScrim) { windowWidth, windowHeight ->
        val bottom = sheetPlacement(windowWidth.value) == SheetPlacement.Bottom
        val shape = if (bottom) {
            RoundedCornerShape(topStart = GraphoSpace.radiusContainer, topEnd = GraphoSpace.radiusContainer)
        } else {
            GraphoShapes.container
        }
        val placed = if (bottom) {
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = windowHeight * BOTTOM_SHEET_MAX_HEIGHT)
        } else {
            val preferred = frame?.width
                ?: if (wide) GraphoSpace.sizeSheetWidthWide else GraphoSpace.sizeSheetWidthDefault
            val room = windowHeight - GraphoSpace.spaceLg * 2
            Modifier
                .align(Alignment.Center)
                .width(
                    centredSheetWidthDp(windowWidth.value, preferred.value, GraphoSpace.spaceLg.value).dp,
                )
                .then(
                    if (frame == null) Modifier.heightIn(max = room) else Modifier.height(minOf(frame.height, room)),
                )
        }
        Surface(
            modifier = modifier
                .then(placed)
                .imePadding()
                .graphicsLayer {
                    val left = 1f - progress.value
                    alpha = progress.value
                    if (bottom) {
                        translationY = size.height * left
                    } else {
                        val scale = 1f - (1f - SHEET_START_SCALE) * left
                        scaleX = scale
                        scaleY = scale
                    }
                }
                // A tap on the sheet must not fall through to the scrim and close it.
                .pointerInput(Unit) { detectTapGestures { } },
            shape = shape,
            color = c.surfaceOverlay,
            shadowElevation = SHEET_ELEVATION,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Grapho.metrics.sizeSheetHeader)
                        .padding(start = GraphoSpace.space2xl, end = GraphoSpace.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = Grapho.type.title,
                        color = c.textDefault,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    GraphoIconButton(R.drawable.ic_close, stringResource(R.string.action_close), onDismiss)
                }
                Column(
                    modifier = if (frame == null) {
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = GraphoSpace.space2xl, vertical = GraphoSpace.spaceSm)
                    } else {
                        Modifier.weight(1f)
                    },
                    verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
                    content = content,
                )
                if (footer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(GraphoSpace.space2xl),
                        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = footer,
                    )
                }
            }
        }
    }
}
