package com.onemnemo.grapho.ui.note

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.PenGlyph
import com.onemnemo.grapho.ui.components.PenKind
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoContent
import com.onemnemo.grapho.ui.theme.GraphoWheel
import com.onemnemo.grapho.ui.theme.PenColors
import kotlin.math.PI
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

internal const val PEN_PANEL_W = 260f
private const val CARD_H = 56f
private const val CARD_GAP = 6f
private const val PANEL_PAD = 8f
private const val SAVE_H = 44f
private const val LINE_H = 40f
private const val HEADER_H = 34f

/** One saved pen. Choosing it sets the tool, its colour, its width, its opacity and its line style. */
internal class PenPreset(
    val name: String,
    val kind: PenKind,
    val color: Color,
    val widthPt: Float,
    val alpha: Int,
    val line: LineStyle = LineStyle.SOLID,
) {
    val mode: ToolMode get() = if (kind == PenKind.Highlighter) ToolMode.HIGHLIGHTER else ToolMode.PEN
}

/** The person's list of pens, kept in shared preferences as a JSON array. The first run starts with the built-in pens. */
internal class PenStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val pens = mutableStateListOf<PenPreset>()

    init {
        val stored = prefs.getString(KEY, null)?.let { parse(it) }
        if (stored != null) {
            pens.addAll(stored)
        } else {
            pens.addAll(defaults())
            save()
        }
    }

    fun add(preset: PenPreset) {
        pens.add(preset)
        save()
    }

    fun replace(index: Int, preset: PenPreset) {
        if (index !in pens.indices) return
        pens[index] = preset
        save()
    }

    /** Takes the pen at [index] out of the list and returns it, or null when there is none. */
    fun removeAt(index: Int): PenPreset? {
        val gone = pens.getOrNull(index) ?: return null
        setAll(pens.toList().withoutAt(index))
        return gone
    }

    /** Puts a deleted [preset] back at [index], or at the end when the list has become shorter. */
    fun restore(index: Int, preset: PenPreset) {
        setAll(pens.toList().restoredAt(index, preset))
    }

    private fun setAll(next: List<PenPreset>) {
        pens.clear()
        pens.addAll(next)
        save()
    }

    /** Moves the pen at [index] by [delta] places and returns where it ended up. */
    fun move(index: Int, delta: Int): Int {
        val to = index + delta
        if (index !in pens.indices || to !in pens.indices) return index
        pens.add(to, pens.removeAt(index))
        save()
        return to
    }

    private fun save() {
        val array = JSONArray()
        for (p in pens) {
            array.put(
                JSONObject()
                    .put("name", p.name)
                    .put("kind", if (p.kind == PenKind.Highlighter) "highlighter" else "pen")
                    .put("color", p.color.toArgb())
                    .put("width", p.widthPt.toDouble())
                    .put("alpha", p.alpha)
                    .put("line", p.line.key),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun parse(text: String): List<PenPreset>? = try {
        val array = JSONArray(text)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            PenPreset(
                name = o.getString("name"),
                kind = if (o.getString("kind") == "highlighter") PenKind.Highlighter else PenKind.Pen,
                color = Color(o.getInt("color")),
                widthPt = o.getDouble("width").toFloat(),
                alpha = o.getInt("alpha").coerceIn(ALPHA_MIN, ALPHA_MAX),
                line = LineStyle.fromKey(o.optString("line", null)),
            )
        }
    } catch (e: Exception) {
        null
    }

    private fun defaults(): List<PenPreset> {
        val hl = PenColors.HIGHLIGHT_ALPHA
        return listOf(
            PenPreset(appContext.getString(R.string.pen_fountain), PenKind.Pen, PenColors.pens[0].second, 1.6f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.pen_ballpoint), PenKind.Pen, PenColors.pens[1].second, 0.8f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.pen_pencil), PenKind.Pen, PenColors.pens[6].second, 1.0f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.pen_brush), PenKind.Pen, PenColors.pens[0].second, 5f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.pen_marker), PenKind.Pen, PenColors.pens[4].second, 4f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.wheel_tool_highlighter), PenKind.Highlighter, PenColors.highlighters[0].second, 12f, hl),
            PenPreset(appContext.getString(R.string.pen_red), PenKind.Pen, PenColors.pens[2].second, 1.4f, ALPHA_MAX),
            PenPreset(appContext.getString(R.string.pen_green_highlighter), PenKind.Highlighter, PenColors.highlighters[1].second, 14f, hl),
            PenPreset(appContext.getString(R.string.pen_pink_highlighter), PenKind.Highlighter, PenColors.highlighters[2].second, 18f, hl),
        )
    }

    private companion object {
        const val PREFS = "grapho_pens"
        const val KEY = "pens"
    }
}

internal fun applyPreset(state: ToolbarState, preset: PenPreset) {
    val setting = PenSetting(preset.color.toArgb() or 0xFF000000.toInt(), preset.widthPt, preset.line)
    state.mode = preset.mode
    if (preset.mode == ToolMode.HIGHLIGHTER) {
        state.highlighter = setting
        state.highlighterAlpha = preset.alpha
    } else {
        state.pen = setting
        state.penAlpha = preset.alpha
    }
}

/** The tool as it is now, as a new pen named after its kind and the number of that kind. */
private fun currentAsPreset(context: Context, state: ToolbarState, count: Int): PenPreset {
    val kind = nibKind(state)
    val setting = currentSetting(state)
    val name = context.getString(
        if (kind == PenKind.Highlighter) R.string.pen_new_highlighter else R.string.pen_new_pen,
        count + 1,
    )
    return PenPreset(name, kind, Color(setting.color), setting.widthPt, state.alpha, setting.line)
}

private fun isCurrent(state: ToolbarState, preset: PenPreset): Boolean {
    val setting = currentSetting(state)
    return preset.mode == (if (state.mode == ToolMode.HIGHLIGHTER) ToolMode.HIGHLIGHTER else ToolMode.PEN) &&
        (preset.color.toArgb() or 0xFF000000.toInt()) == setting.color &&
        sameSize(preset.widthPt, setting.widthPt) &&
        preset.line == setting.line
}

/** The panel's height in dp for [count] pens, kept within [hostH]. */
internal fun penPanelHeight(count: Int, hostH: Float): Float {
    val content = 2f * PANEL_PAD + HEADER_H + LINE_H + count * (CARD_H + CARD_GAP) + SAVE_H
    return content.coerceAtMost(maxOf(hostH - 16f, 120f))
}

/**
 * The pen selector: a dark panel beside the wheel with a card for each saved pen, showing a sample stroke in
 * its colour, width and opacity. Tap a card to use that pen; press and hold it for a Remove action, which
 * deletes it at once and offers Undo. The header's Edit opens the wheel customiser on the pens. The last row
 * saves the pen in use as a new card. The panel's window is placed at ([x], [y]) dp.
 */
@Composable
internal fun PenPanel(
    state: ToolbarState,
    store: PenStore,
    progress: Float,
    x: Float,
    y: Float,
    height: Float,
    onPick: (PenPreset) -> Unit,
    onEdit: () -> Unit,
    notices: Notices,
) {
    val density = LocalDensity.current.density
    var removing by remember { mutableStateOf<PenPreset?>(null) }
    val context = LocalContext.current
    val pensLabel = stringResource(R.string.wheel_tab_pens)
    val editPensLabel = stringResource(R.string.wheel_edit_pens)
    val saveLabel = stringResource(R.string.wheel_save_pen)

    Popup(
        popupPositionProvider = anchoredAt(density, x, y),
        properties = OverlayProperties,
    ) {
        Column(
            Modifier
                .popIn(progress, 0.9f, TransformOrigin.Center)
                .width(PEN_PANEL_W.dp)
                .heightIn(max = height.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GraphoWheel.body)
                .padding(PANEL_PAD.dp)
                .semantics { contentDescription = pensLabel },
        ) {
            Row(
                Modifier.fillMaxWidth().height(HEADER_H.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(pensLabel, style = Grapho.type.label, color = GraphoWheel.text, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .pressable(scaleOnPress = false, onClick = onEdit)
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = editPensLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.action_edit), style = Grapho.type.label, color = GraphoWheel.text)
                }
            }
            LineStyleChoice(
                current = currentSetting(state).line,
                modifier = Modifier.height(LINE_H.dp).padding(horizontal = 2.dp, vertical = 4.dp),
            ) { applyLine(state, it) }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CARD_GAP.dp),
            ) {
                for (preset in store.pens.toList()) {
                    PenCard(
                        preset = preset,
                        current = isCurrent(state, preset),
                        removing = removing === preset,
                        onTap = {
                            if (removing != null) removing = null else onPick(preset)
                        },
                        onHold = { removing = preset },
                        onRemove = {
                            removing = null
                            deletePen(context, store, store.pens.indexOf(preset), notices)
                        },
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = CARD_GAP.dp)
                    .fillMaxWidth()
                    .height(SAVE_H.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .pressable(scaleOnPress = false) {
                        removing = null
                        store.add(currentAsPreset(context, state, store.pens.count { it.kind == nibKind(state) }))
                    }
                    .semantics { contentDescription = saveLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.wheel_save_pen_button), style = Grapho.type.label, color = GraphoWheel.text)
            }
        }
    }
}

@Composable
private fun PenCard(
    preset: PenPreset,
    current: Boolean,
    removing: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val removeLabel = stringResource(R.string.wheel_remove_named, preset.name)
    val ink = preset.color.copy(alpha = preset.alpha / 255f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(CARD_H.dp)
            .clip(shape)
            .background(if (current) GraphoWheel.selectedFill else GraphoWheel.well)
            .then(if (current) Modifier.border(1.5f.dp, GraphoWheel.textMuted, shape) else Modifier)
            .pressable(scaleOnPress = false, onLongClick = onHold, onClick = onTap)
            .semantics { contentDescription = preset.name },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(CARD_H.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                PenGlyph(
                    kind = preset.kind,
                    color = ink,
                    outline = GraphoWheel.textMuted,
                    modifier = Modifier
                        .size(width = 12.dp, height = 33.dp)
                        .graphicsLayer { rotationZ = 90f },
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(preset.name, style = Grapho.type.label, color = GraphoWheel.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(GraphoContent.contentPaperWhite),
                ) {
                    Squiggle(ink, preset.widthPt, preset.line)
                }
            }
        }
        if (removing) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Grapho.colors.stateDangerSolid)
                    .pressable(scaleOnPress = false, onClick = onRemove)
                    .padding(horizontal = 14.dp)
                    .semantics { contentDescription = removeLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.action_remove), style = Grapho.type.label, color = Grapho.colors.stateDangerOnSolid)
            }
        }
    }
}

/** A short wave in the pen's colour, as wide as the pen writes (scaled to fit the card), dashed or dotted to match. */
@Composable
private fun Squiggle(color: Color, widthPt: Float, line: LineStyle) {
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        val stroke = (widthPt * 1.5f).coerceIn(1.5f, 8f).dp.toPx()
        val pad = stroke / 2f + 4.dp.toPx()
        val amp = ((size.height - stroke) / 2f * 0.7f).coerceAtLeast(0f)
        val path = Path()
        val steps = 32
        for (s in 0..steps) {
            val t = s / steps.toFloat()
            val px = pad + t * (size.width - 2f * pad)
            val py = size.height / 2f + sin(t * 2f * 2f * PI.toFloat()) * amp
            if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path,
            color,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = linePathEffect(line, stroke),
            ),
        )
    }
}
