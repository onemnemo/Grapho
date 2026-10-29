package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoInlineTextField
import com.onemnemo.grapho.ui.components.InlineEditState
import com.onemnemo.grapho.ui.components.LocalPopupSide
import com.onemnemo.grapho.ui.components.floatingSurface
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val TITLE_MAX_WIDTH = 200.dp

/** How far the floating bar sits off the edge it is against. */
internal val FLOATING_BAR_GAP = GraphoSpace.spaceMd

/**
 * The note's bar: back, the title (tap it to rename in place, hidden when [showTitle] is off), the [tools] in the
 * middle and whatever [actions] adds on the far end. It floats over the page as a pill, or runs the full length of
 * its edge as a flat strip with a hairline against the page, where the tools sit in the middle of the strip. At the
 * top or bottom it lies along the edge; at the left or right it is a column, with no title. [onRename] is null
 * while the note is still opening. [maxLength] caps the floating pill along its edge. In [readMode] the title is
 * plain text with the [reading] position under it, and the caller fills [actions] with the count and Edit.
 * [onTouched] hears every touch or hover on the bar without taking it, so an auto-hide bar knows it is in use.
 */
@Composable
fun NoteBar(
    style: NoteBarStyle,
    title: String,
    untitled: String,
    onBack: () -> Unit,
    onRename: ((String) -> Unit)?,
    titleEdit: InlineEditState,
    modifier: Modifier = Modifier,
    position: BarPosition = BarPosition.Top,
    maxLength: Dp = GraphoSpace.sizeBarNoteMax,
    showTitle: Boolean = true,
    reading: ReadPosition? = null,
    readMode: Boolean = false,
    onTouched: () -> Unit = {},
    tools: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    val vertical = position.vertical
    val lead: @Composable (Modifier) -> Unit = { titleModifier ->
        GraphoIconButton(
            R.drawable.ic_chevron_left,
            stringResource(R.string.action_back),
            onBack,
            tooltipSide = position.tipSide,
        )
        if (!vertical && readMode) {
            ReadTitle(title.ifBlank { untitled }, reading, titleModifier.widthIn(max = TITLE_MAX_WIDTH))
        } else if (!vertical && showTitle) {
            Box(titleModifier.widthIn(max = TITLE_MAX_WIDTH)) {
                if (onRename != null) {
                    GraphoInlineTextField(
                        value = title,
                        onCommit = onRename,
                        placeholder = untitled,
                        textStyle = Grapho.type.label,
                        state = titleEdit,
                    )
                }
            }
        }
    }
    val watched = modifier.watchTouches(onTouched)
    val thickness = Grapho.metrics.sizeBarNote
    CompositionLocalProvider(LocalPopupSide provides position.popupSide) {
        if (vertical) {
            VerticalBar(style, position, thickness, maxLength, watched, lead, tools, actions)
        } else {
            HorizontalBar(style, position, thickness, maxLength, watched, lead, tools, actions)
        }
    }
}

@Composable
private fun HorizontalBar(
    style: NoteBarStyle,
    position: BarPosition,
    thickness: Dp,
    maxLength: Dp,
    modifier: Modifier,
    lead: @Composable (Modifier) -> Unit,
    tools: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    val line = style == NoteBarStyle.Line
    val content: @Composable RowScope.() -> Unit = {
        Row(
            modifier = Modifier.weight(1f, fill = line),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            verticalAlignment = Alignment.CenterVertically,
        ) { lead(Modifier.weight(1f, fill = false)) }
        tools()
        if (line) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        } else {
            actions()
        }
    }
    if (!line) {
        Row(
            modifier = modifier
                .padding(
                    top = if (position == BarPosition.Top) FLOATING_BAR_GAP else 0.dp,
                    bottom = if (position == BarPosition.Bottom) FLOATING_BAR_GAP else 0.dp,
                )
                .widthIn(max = maxLength)
                .height(thickness)
                .floatingSurface(GraphoShapes.pill)
                // The pill's padding must not let a tap through to the page and clear a selection.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = GraphoSpace.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(thickness)
                .background(Grapho.colors.surfaceRaised)
                .hairlineAgainstPage(position)
                .padding(horizontal = GraphoSpace.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun VerticalBar(
    style: NoteBarStyle,
    position: BarPosition,
    thickness: Dp,
    maxLength: Dp,
    modifier: Modifier,
    lead: @Composable (Modifier) -> Unit,
    tools: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    val line = style == NoteBarStyle.Line
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.weight(1f, fill = line),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { lead(Modifier) }
        tools()
        if (line) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { actions() }
        } else {
            actions()
        }
    }
    if (!line) {
        Column(
            modifier = modifier
                .absolutePadding(
                    left = if (position == BarPosition.Left) FLOATING_BAR_GAP else 0.dp,
                    right = if (position == BarPosition.Right) FLOATING_BAR_GAP else 0.dp,
                )
                .heightIn(max = maxLength)
                .width(thickness)
                .floatingSurface(GraphoShapes.pill)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(vertical = GraphoSpace.spaceXs),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(thickness)
                .background(Grapho.colors.surfaceRaised)
                .hairlineAgainstPage(position)
                .padding(vertical = GraphoSpace.spaceXs),
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceNudge),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/** The quiet line on the flat bar's side that faces the page. */
@Composable
private fun Modifier.hairlineAgainstPage(position: BarPosition): Modifier {
    val edge = Grapho.colors.borderSubtle
    val hairline = GraphoSpace.borderWidthHairline
    return drawBehind {
        val w = hairline.toPx()
        when (position) {
            BarPosition.Top -> drawRect(edge, Offset(0f, size.height - w), Size(size.width, w))
            BarPosition.Bottom -> drawRect(edge, Offset.Zero, Size(size.width, w))
            BarPosition.Left -> drawRect(edge, Offset(size.width - w, 0f), Size(w, size.height))
            BarPosition.Right -> drawRect(edge, Offset.Zero, Size(w, size.height))
        }
    }
}

/** Reports every pointer event on the bar without consuming any, as the bar is the first to see them. */
@Composable
private fun Modifier.watchTouches(onTouched: () -> Unit): Modifier {
    val latest by rememberUpdatedState(onTouched)
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                latest()
            }
        }
    }
}
