package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextEdit
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.note.displayColor
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

/**
 * Where the open text box is in the canvas's pixels: its turned top left corner, its width, how many pixels a page
 * point is, and its turn in degrees.
 */
private data class TextSpot(val x: Float, val y: Float, val width: Float, val scale: Float, val degrees: Float)

/** Follows the open box a frame at a time, so the field over it keeps up with scrolling and zooming. */
@Composable
private fun rememberTextSpot(view: () -> NoteCanvasView?, serial: Int): State<TextSpot?> {
    val spot = remember(serial) { mutableStateOf<TextSpot?>(null) }
    LaunchedEffect(serial) {
        val probe = FloatArray(PROBE_SIZE)
        while (true) {
            withFrameNanos { }
            val found = view()?.textPlacement(probe) == true
            val next = if (found) TextSpot(probe[0], probe[1], probe[2], probe[3], probe[4]) else null
            if (spot.value != next) spot.value = next
        }
    }
    return spot
}

private const val PROBE_SIZE = 5

/** The room the Done pill takes under the box, which the format bar clears when it has to go there. */
private val DONE_ROOM = 56.dp

/**
 * The field a text box is typed into, laid over the box on the page at the page's scale, with a frame round it,
 * handles to move and widen it, and a Done button under it. The canvas hides the box while it is open and
 * writes it to the note when the edit ends: Done, Back, a tap on the page or another tool. [insets] keep Done
 * clear of a floating note bar.
 */
@Composable
internal fun TextEditLayer(ui: NoteUiState, toolbar: ToolbarState, insets: EdgeInsets) {
    val edit = ui.textEdit ?: return
    val spot by rememberTextSpot({ ui.view[0] }, edit.serial)
    val here = spot ?: return
    key(edit.serial) {
        TextEditField(
            edit = edit,
            spot = here,
            paper = ui.view[0]?.paperColor ?: PaperColors.WHITE.argb,
            insets = insets,
            palette = toolbar.palette,
            recent = toolbar.recent,
            onChange = { applyTextStyle(ui, toolbar, it) },
            onCustomColour = { applyTextColour(ui, toolbar, it) },
            onTyped = { ui.view[0]?.textTyped(it) },
            onMove = { dx, dy -> ui.view[0]?.moveText(dx, dy) },
            onStretch = { fromLeft, delta -> ui.view[0]?.stretchText(fromLeft, delta) },
            onDone = { ui.view[0]?.commitText() },
        )
    }
}

@Composable
private fun TextEditField(
    edit: TextEdit,
    spot: TextSpot,
    paper: Int,
    insets: EdgeInsets,
    palette: List<Int>,
    recent: List<Int>,
    onChange: (TextSettings) -> Unit,
    onCustomColour: (Int) -> Unit,
    onTyped: (String) -> Unit,
    onMove: (dx: Float, dy: Float) -> Unit,
    onStretch: (fromLeft: Boolean, delta: Float) -> Unit,
    onDone: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(edit.startText, TextRange(edit.startText.length))) }
    var heightPx by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val density = LocalDensity.current
    val style = edit.style
    val ink = displayColor(style.argb, paper)
    val textStyle = with(density) {
        TextStyle(
            color = Color(ink),
            fontSize = (style.size * spot.scale).toSp(),
            fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (style.underline) TextDecoration.Underline else TextDecoration.None,
            textAlign = when (style.align) {
                TextAlignment.Start -> TextAlign.Start
                TextAlignment.Centre -> TextAlign.Center
                TextAlignment.End -> TextAlign.End
            },
        )
    }
    val label = stringResource(R.string.text_box)
    val ring = Grapho.colors.selectionIndicator
    val slot = with(density) { TEXT_HANDLE_SLOT.roundToPx() }
    Box(Modifier.pinnedBox(spot, slot)) {
        BasicTextField(
            value = value,
            onValueChange = {
                if (it.text.length <= TextSettings.MAX_CHARS) {
                    value = it
                    onTyped(it.text)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(TEXT_HANDLE_SLOT)
                .onSizeChanged { heightPx = it.height }
                .border(GraphoSpace.borderWidthHairline, ring)
                .focusRequester(focus)
                .semantics { contentDescription = label },
            textStyle = textStyle,
            cursorBrush = SolidColor(ring),
        )
        TextHandles(spot.scale, onMove, onStretch)
    }
    val margins = with(density) {
        EdgeInsets(insets.left.dp.toPx(), insets.top.dp.toPx(), insets.right.dp.toPx(), insets.bottom.dp.toPx())
    }
    val gap = with(density) { GraphoSpace.spaceMd.toPx() }
    val under = remember(spot.x, spot.y, spot.width, heightPx) {
        mutableStateOf<RectF?>(RectF(spot.x, spot.y, spot.x + spot.width, spot.y + heightPx))
    }
    val above = remember(spot.x, spot.y, spot.width, heightPx) {
        mutableStateOf<RectF?>(RectF(spot.x, spot.y, spot.x + spot.width, spot.y + heightPx))
    }
    val handleGap = with(density) { (TEXT_HANDLE_SLOT + GraphoSpace.spaceXs).toPx() }
    val doneRoom = with(density) { DONE_ROOM.toPx() }
    TextFormatBar(
        style = style,
        rect = above,
        gap = handleGap,
        belowRoom = doneRoom,
        margins = margins,
        palette = palette,
        recent = recent,
        onChange = onChange,
        onCustomColour = onCustomColour,
    )
    Row(
        Modifier
            .placedUnder(under, gap, margins)
            .floatingCard(GraphoShapes.pill)
            .padding(horizontal = GraphoSpace.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoButton(
            stringResource(R.string.action_done),
            onClick = onDone,
            kind = ButtonKind.Quiet,
            size = ButtonSize.Small,
        )
    }
}

/**
 * Gives the content the width of the text box on the page plus [slot] pixels of room for the handles on each side,
 * and the height its lines need plus the same, and puts the text box's top left corner on [spot], turned about that
 * corner. The layer takes no touches of its own, so the page under it still gets them.
 */
private fun Modifier.pinnedBox(spot: TextSpot, slot: Int): Modifier = layout { measurable, constraints ->
    val wide = spot.width.roundToInt().coerceAtLeast(1) + 2 * slot
    val placeable = measurable.measure(Constraints(wide, wide, 0, Constraints.Infinity))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.placeWithLayer(spot.x.roundToInt() - slot, spot.y.roundToInt() - slot) {
            rotationZ = spot.degrees
            transformOrigin = TransformOrigin(slot / placeable.width.toFloat(), slot / placeable.height.toFloat())
        }
    }
}
