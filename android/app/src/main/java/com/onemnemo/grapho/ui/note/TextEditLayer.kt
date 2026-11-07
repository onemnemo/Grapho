package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.FormatState
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.note.RichText
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

/**
 * Follows the open box a frame at a time, so the field over it keeps up with scrolling and zooming. Read the state
 * only while laying out or drawing, never while composing, so a scroll does not recompose the field and its bar.
 */
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
    key(edit.serial) {
        val spot = rememberTextSpot({ ui.view[0] }, edit.serial)
        val placed by remember { derivedStateOf { spot.value != null } }
        if (placed) {
            TextEditField(
                edit = edit,
                spot = spot,
                paper = ui.view[0]?.paperColor ?: PaperColors.WHITE.argb,
                insets = insets,
                palette = toolbar.palette,
                recent = toolbar.recent,
                onChange = { applyTextStyle(ui, toolbar, it) },
                onAddRecent = { toolbar.addRecent(it) },
                onTyped = { ui.view[0]?.textTyped(it) },
                onFormatting = { ui.view[0]?.textFormatting(it) },
                onBaseline = { ui.view[0]?.textBaseline(it) },
                onFit = { ui.view[0]?.fitTextWidth() },
                onMove = { dx, dy -> ui.view[0]?.moveText(dx, dy) },
                onStretch = { fromLeft, delta -> ui.view[0]?.stretchText(fromLeft, delta) },
                onScale = { left, top, dx, dy -> ui.view[0]?.scaleText(left, top, dx, dy) },
                onDone = { ui.view[0]?.commitText() },
            )
        }
    }
}

@Composable
private fun TextEditField(
    edit: TextEdit,
    spot: State<TextSpot?>,
    paper: Int,
    insets: EdgeInsets,
    palette: List<Int>,
    recent: List<Int>,
    onChange: (TextSettings) -> Unit,
    onAddRecent: (Int) -> Unit,
    onTyped: (String) -> Unit,
    onFormatting: ((() -> RichText)?) -> Unit,
    onBaseline: (RichText) -> Unit,
    onFit: () -> Unit,
    onMove: (dx: Float, dy: Float) -> Unit,
    onStretch: (fromLeft: Boolean, delta: Float) -> Unit,
    onScale: (fromLeft: Boolean, fromTop: Boolean, dx: Float, dy: Float) -> Unit,
    onDone: () -> Unit,
) {
    val density = LocalDensity.current
    val style = edit.style
    val ink = displayColor(style.argb, paper)
    val label = stringResource(R.string.text_box)
    val ring = Grapho.colors.selectionIndicator
    val accent = ring.toArgb()
    val slot = with(density) { TEXT_HANDLE_SLOT.roundToPx() }
    val typed by rememberUpdatedState(onTyped)
    val fit by rememberUpdatedState(onFit)
    val format = remember { mutableStateOf(FormatState()) }
    val current by rememberUpdatedState(style)
    val change by rememberUpdatedState(onChange)
    val move by rememberUpdatedState(onMove)
    val field = remember { arrayOfNulls<TextBoxInput>(1) }
    val heightPx = remember { mutableIntStateOf(0) }
    // Zooming changes the font size on the field itself, so the field and its bar are not composed again.
    LaunchedEffect(Unit) {
        snapshotFlow { spot.value }.collect { at -> if (at != null) field[0]?.follow(at.scale, at.degrees) }
    }
    val box = remember {
        derivedStateOf<RectF?>(structuralEqualityPolicy()) {
            spot.value?.let { RectF(it.x, it.y, it.x + it.width, it.y + heightPx.intValue) }
        }
    }
    Box(Modifier.pinnedBox(spot, slot)) {
        AndroidView(
            factory = { context ->
                TextBoxInput(
                    context,
                    edit.startText,
                    edit.startRich,
                    style.size.toFloat(),
                    paper,
                    edit.marks,
                ).also { input ->
                    field[0] = input
                    input.contentDescription = label
                    input.onTyped = { typed(it) }
                    input.onFormat = { format.value = it }
                    input.onBigInsert = { fit() }
                    onFormatting { input.snapshot() }
                    onBaseline(input.snapshot())
                    input.onDrag = { dx, dy -> move(dx, dy) }
                    spot.value?.let { input.follow(it.scale, it.degrees) }
                    input.applyLook(style, ink, accent)
                    input.focusAndShowKeyboard()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(TEXT_HANDLE_SLOT)
                .onSizeChanged { heightPx.intValue = it.height }
                .border(GraphoSpace.borderWidthHairline, ring),
            update = { it.applyLook(style, ink, accent) },
            onRelease = {
                val last = it.snapshot()
                onFormatting { last }
                it.release()
                field[0] = null
            },
        )
        TextHandles({ spot.value?.scale ?: 1f }, onMove, onStretch, onScale)
    }
    val margins = with(density) {
        EdgeInsets(insets.left.dp.toPx(), insets.top.dp.toPx(), insets.right.dp.toPx(), insets.bottom.dp.toPx())
    }
    val gap = with(density) { GraphoSpace.spaceMd.toPx() }
    val handleGap = with(density) { (TEXT_HANDLE_SLOT + GraphoSpace.spaceXs).toPx() }
    val doneRoom = with(density) { DONE_ROOM.toPx() }
    val actions = remember {
        TextFormatActions(
            onMark = { field[0]?.toggleMark(it) },
            onKind = { field[0]?.setKind(it) },
            onIndent = { field[0]?.indent(it) },
            onHighlight = { on -> field[0]?.setHighlight(if (on) HIGHLIGHT_COLOUR else null) },
            onSize = { delta ->
                val input = field[0]
                val box = current.size
                val now = format.value.size?.let { (it * box).roundToInt() } ?: box
                val next = (now + delta).coerceIn(TextSettings.MIN_SIZE, TextSettings.MAX_SIZE)
                if (input == null || input.isBlank) change(current.copy(size = next)) else input.setSize(next.toFloat())
            },
            onColour = { argb ->
                onAddRecent(argb)
                val input = field[0]
                if (input == null || input.isBlank) change(current.copy(argb = argb)) else input.setColour(argb)
            },
        )
    }
    TextFormatBar(
        style = style,
        format = format.value,
        rect = box,
        gap = handleGap,
        belowRoom = doneRoom,
        margins = margins,
        palette = palette,
        recent = recent,
        actions = actions,
        onChange = onChange,
    )
    Row(
        Modifier
            .placedUnder(box, gap, margins)
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
 * corner. [spot] is read here, while laying out, so following the page only moves the box. The layer takes no
 * touches of its own, so the page under it still gets them.
 */
private fun Modifier.pinnedBox(spot: State<TextSpot?>, slot: Int): Modifier = layout { measurable, constraints ->
    val at = spot.value
    val wide = (at?.width ?: 1f).roundToInt().coerceAtLeast(1) + 2 * slot
    val placeable = measurable.measure(Constraints(wide, wide, 0, Constraints.Infinity))
    layout(constraints.maxWidth, constraints.maxHeight) {
        if (at == null) return@layout
        placeable.placeWithLayer(at.x.roundToInt() - slot, at.y.roundToInt() - slot) {
            rotationZ = at.degrees
            transformOrigin = TransformOrigin(slot / placeable.width.toFloat(), slot / placeable.height.toFloat())
        }
    }
}
