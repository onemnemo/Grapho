package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.Fonts
import com.onemnemo.grapho.note.FormatState
import com.onemnemo.grapho.note.Marks
import com.onemnemo.grapho.note.ParaKinds
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.ui.components.ColorSwatch
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The font size moves this many points for each press of the size buttons. */
private const val SIZE_STEP = 2

/** The widest the palette row under the format bar grows, so a long palette scrolls. */
private val PALETTE_MAX_WIDTH = 320.dp

/** Where the format bar goes, in view pixels. */
internal object FormatBarPlacement {
    /**
     * Puts a bar [barWidth] by [barHeight] centred over a box spanning [left] to [right] and [top] to [bottom], [gap]
     * above it. When there is no room above, it goes [belowGap] under the box instead, and always inside the window
     * [windowWidth] by [windowHeight] less the margins.
     */
    fun above(
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        gap: Float,
        belowGap: Float,
        barWidth: Float,
        barHeight: Float,
        windowWidth: Float,
        windowHeight: Float,
        margins: EdgeInsets,
    ): PillSpot {
        val xLow = min(margins.left, max(0f, windowWidth - barWidth))
        val xHigh = max(xLow, windowWidth - barWidth - margins.right)
        val x = ((left + right) / 2f - barWidth / 2f).coerceIn(xLow, xHigh)
        val yLow = min(margins.top, max(0f, windowHeight - barHeight))
        val yHigh = max(yLow, windowHeight - barHeight - margins.bottom)
        val over = top - gap - barHeight
        val y = (if (over >= yLow) over else bottom + belowGap).coerceIn(yLow, yHigh)
        return PillSpot(x, y)
    }
}

/** Puts the bar over [rect] as [FormatBarPlacement.above] says, a frame at a time, without recomposing. */
private fun Modifier.placedAbove(rect: State<RectF?>, gap: Float, belowGap: Float, margins: EdgeInsets): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val r = rect.value
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (r != null) {
                val spot = FormatBarPlacement.above(
                    r.left,
                    r.right,
                    r.top,
                    r.bottom,
                    gap,
                    belowGap,
                    placeable.width.toFloat(),
                    placeable.height.toFloat(),
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat(),
                    margins,
                )
                placeable.place(spot.x.roundToInt(), spot.y.roundToInt())
            }
        }
    }

/** Makes the look of the open box [style] and remembers it for the next box. */
internal fun applyTextStyle(ui: NoteUiState, toolbar: ToolbarState, style: TextSettings) {
    toolbar.text = style.coerced()
    ui.view[0]?.setTextStyle(toolbar.text)
}

/** Colours the open box with a picked custom colour, keeps it among the recent ones, and remembers it. */
internal fun applyTextColour(ui: NoteUiState, toolbar: ToolbarState, argb: Int) {
    toolbar.addRecent(argb)
    applyTextStyle(ui, toolbar, (ui.textEdit?.style ?: toolbar.text).copy(argb = argb))
}

/** What the format bar's buttons do to the open field. Each is a no-op once the field is gone. */
internal class TextFormatActions(
    val onMark: (Int) -> Unit,
    val onKind: (Int) -> Unit,
    val onIndent: (Int) -> Unit,
    val onHighlight: (Boolean) -> Unit,
    val onSize: (Int) -> Unit,
    val onColour: (Int) -> Unit,
    val onFont: (Int) -> Unit,
)

/** The marker colour the highlight button puts on text. */
internal const val HIGHLIGHT_COLOUR = 0xFFFFF176.toInt()

private enum class BarTab { Text, Paragraph }

private enum class BarPanel { Font, Colour }

private val FONT_NAMES = listOf(
    R.string.text_font_sans,
    R.string.text_font_serif,
    R.string.text_font_mono,
    R.string.text_font_condensed,
    R.string.text_font_rounded,
    R.string.text_font_hand,
)

/**
 * The card over the open text box. It has two tabs so only one short row shows at a time: Text has the font, size,
 * bold, italic, underline, strikethrough, highlight, code and colour, and Paragraph has the headings, quote, lists,
 * indent and alignment. The buttons light up from [format], which says what the selection or cursor looks like, and
 * act on the selection, or on what is typed next at a cursor. The font acts on the whole box when nothing is
 * selected. The card follows [rect], the box in view pixels, [gap] clear of it (and [belowRoom] more when it has to go
 * under the box), and stays inside [margins]. It is drawn in the page's own layer and takes no focus, so the keyboard
 * stays up while it is used. Alignment is for the whole box and goes to [onChange] with the whole new look of the box.
 */
@Composable
internal fun TextFormatBar(
    style: TextSettings,
    format: FormatState,
    rect: State<RectF?>,
    gap: Float,
    belowRoom: Float,
    margins: EdgeInsets,
    palette: List<Int>,
    recent: List<Int>,
    actions: TextFormatActions,
    onChange: (TextSettings) -> Unit,
) {
    var tab by remember { mutableStateOf(BarTab.Text) }
    var panel by remember { mutableStateOf<BarPanel?>(null) }
    val size = format.size?.let { (it * style.size).roundToInt() } ?: style.size
    val colour = format.colour ?: style.argb
    Column(
        Modifier
            .placedAbove(rect, gap, gap + belowRoom, margins)
            .floatingCard(GraphoShapes.surface)
            .padding(horizontal = GraphoSpace.spaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormatChip(stringResource(R.string.text_tab_text), null, tab == BarTab.Text) { tab = BarTab.Text }
            FormatChip(stringResource(R.string.text_tab_paragraph), null, tab == BarTab.Paragraph) {
                tab = BarTab.Paragraph
                panel = null
            }
            Spacer(Modifier.width(GraphoSpace.spaceMd))
            if (tab == BarTab.Text) {
                TextTab(style, format, size, colour, panel, actions) { panel = if (panel == it) null else it }
            } else {
                ParagraphTab(style, format, actions, onChange)
            }
        }
        if (tab == BarTab.Text && panel == BarPanel.Font) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                for ((id, name) in FONT_NAMES.withIndex()) {
                    val family = FontFamily(Typeface.create(Fonts.family(id), Typeface.NORMAL))
                    FormatChip(stringResource(name), null, (format.font ?: format.boxFont) == id, family) {
                        actions.onFont(id)
                    }
                }
            }
        }
        if (tab == BarTab.Text && panel == BarPanel.Colour) {
            Column(Modifier.widthIn(max = PALETTE_MAX_WIDTH), verticalArrangement = Arrangement.Center) {
                ColourRow(
                    current = colour,
                    palette = palette,
                    recent = recent,
                    onPick = { actions.onColour(it) },
                    onCustomColour = { actions.onColour(it) },
                )
            }
        }
    }
}

@Composable
private fun TextTab(
    style: TextSettings,
    format: FormatState,
    size: Int,
    colour: Int,
    panel: BarPanel?,
    actions: TextFormatActions,
    onPanel: (BarPanel) -> Unit,
) {
    GraphoIconButton(
        R.drawable.ic_font,
        stringResource(R.string.text_font),
        onClick = { onPanel(BarPanel.Font) },
        selected = panel == BarPanel.Font,
    )
    GraphoIconButton(
        R.drawable.ic_minus,
        stringResource(R.string.text_smaller),
        onClick = { actions.onSize(-SIZE_STEP) },
        enabled = size > TextSettings.MIN_SIZE,
    )
    Text(
        size.toString(),
        style = Grapho.type.label,
        color = Grapho.colors.textDefault,
        textAlign = TextAlign.Center,
        modifier = Modifier.width(28.dp),
    )
    GraphoIconButton(
        R.drawable.ic_plus,
        stringResource(R.string.text_larger),
        onClick = { actions.onSize(SIZE_STEP) },
        enabled = size < TextSettings.MAX_SIZE,
    )
    MarkIcon(R.drawable.ic_bold, R.string.text_bold, format.marks, Marks.BOLD, actions)
    MarkIcon(R.drawable.ic_italic, R.string.text_italic, format.marks, Marks.ITALIC, actions)
    MarkIcon(R.drawable.ic_underline, R.string.text_underline, format.marks, Marks.UNDERLINE, actions)
    MarkIcon(R.drawable.ic_strike, R.string.text_strike, format.marks, Marks.STRIKE, actions)
    GraphoIconButton(
        R.drawable.ic_highlight,
        stringResource(R.string.text_highlight),
        onClick = { actions.onHighlight(format.highlight == null) },
        selected = format.highlight != null,
    )
    MarkIcon(R.drawable.ic_code, R.string.text_code, format.marks, Marks.CODE, actions)
    ColorSwatch(
        color = Color(colour),
        selected = panel == BarPanel.Colour,
        ringColor = Grapho.colors.selectionIndicator,
        label = stringResource(R.string.text_colour),
        onClick = { onPanel(BarPanel.Colour) },
    )
}

@Composable
private fun ParagraphTab(
    style: TextSettings,
    format: FormatState,
    actions: TextFormatActions,
    onChange: (TextSettings) -> Unit,
) {
    val kinds = listOf(
        Triple(ParaKinds.BODY, "T", R.string.text_para_body),
        Triple(ParaKinds.HEADING1, "H1", R.string.text_heading1),
        Triple(ParaKinds.HEADING2, "H2", R.string.text_heading2),
        Triple(ParaKinds.HEADING3, "H3", R.string.text_heading3),
    )
    for ((kind, label, name) in kinds) {
        FormatChip(label, stringResource(name), format.kind == kind) { actions.onKind(kind) }
    }
    KindIcon(R.drawable.ic_quote, R.string.text_quote, format.kind, ParaKinds.QUOTE, actions)
    KindIcon(R.drawable.ic_bullets, R.string.text_bullets, format.kind, ParaKinds.BULLET, actions)
    KindIcon(R.drawable.ic_numbers, R.string.text_numbers, format.kind, ParaKinds.NUMBERED, actions)
    KindIcon(R.drawable.ic_checklist, R.string.text_checklist, format.kind, ParaKinds.CHECK, actions)
    GraphoIconButton(R.drawable.ic_outdent, stringResource(R.string.text_outdent), onClick = { actions.onIndent(-1) })
    GraphoIconButton(R.drawable.ic_indent, stringResource(R.string.text_indent), onClick = { actions.onIndent(1) })
    GraphoIconButton(
        ALIGN_ICONS.getValue(style.align),
        stringResource(alignName(style.align)),
        onClick = { onChange(style.copy(align = nextAlignment(style.align))) },
    )
}

@Composable
private fun MarkIcon(@DrawableRes icon: Int, @StringRes name: Int, marks: Int, bit: Int, actions: TextFormatActions) {
    GraphoIconButton(icon, stringResource(name), onClick = { actions.onMark(bit) }, selected = marks and bit != 0)
}

@Composable
private fun KindIcon(@DrawableRes icon: Int, @StringRes name: Int, now: Int, kind: Int, actions: TextFormatActions) {
    GraphoIconButton(icon, stringResource(name), onClick = { actions.onKind(kind) }, selected = now == kind)
}

/** A text button for the bar, lit when [on], at least as big as a touch needs. [description] names it, or [label] does. */
@Composable
private fun FormatChip(
    label: String,
    description: String?,
    on: Boolean,
    family: FontFamily? = null,
    onClick: () -> Unit,
) {
    val c = Grapho.colors
    Box(
        Modifier
            .height(Grapho.metrics.sizeHitMin)
            .widthIn(min = Grapho.metrics.sizeHitMin)
            .clip(GraphoShapes.pill)
            .background(if (on) c.selectionFill else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = GraphoSpace.spaceXs)
            .semantics {
                contentDescription = description ?: label
                selected = on
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = Grapho.type.label.copy(fontFamily = family ?: Grapho.type.label.fontFamily),
            color = if (on) c.actionToggleFgOn else c.actionQuietFg,
            textAlign = TextAlign.Center,
        )
    }
}

/** The alignment after [align] when the button is pressed: start, centre, end, and round again. */
internal fun nextAlignment(align: TextAlignment): TextAlignment = when (align) {
    TextAlignment.Start -> TextAlignment.Centre
    TextAlignment.Centre -> TextAlignment.End
    TextAlignment.End -> TextAlignment.Start
}
