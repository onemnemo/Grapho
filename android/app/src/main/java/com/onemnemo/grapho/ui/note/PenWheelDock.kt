package com.onemnemo.grapho.ui.note

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.PenGlyph
import com.onemnemo.grapho.ui.components.PenKind
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs

/** Degrees between two colours on the docked rim. */
internal const val RIM_STEP = 17f

/** Degrees between tools on the tool ring; tools further than this from the right-hand arc fade out. */
internal const val TOOL_STEP = 38f
internal const val TOOL_REACH = 100f
internal val DOT_NUMBERS = listOf(1f, 15f, 29f, 44f, 58f, 72f, 87f, 100f)

/**
 * The wheel's measurements in dp. The docked wheel is a half disc whose centre
 * is on the [edge] of its box; every angle here is in the left-docked layout and
 * [DockEdge.toScreen] turns it to the real edge. The pop-up is the same wheel as a full circle,
 * a little smaller, with its fixed buttons at the top and the bottom.
 */
internal class Geo(val full: Boolean, val edge: DockEdge = DockEdge.Left) {
    val r = if (full) POPUP_R else DISC_R
    private val k = r / DISC_R
    val core = CORE_R * k
    val toolR = 87f * k
    val rimR = RIM_R * k
    val swatchR = 13f * k
    private val docked = dockedSize(edge)
    private val dockedAt = dockedCentre(edge)
    val width = if (full) 2f * r else docked.width
    val height = if (full) 2f * r + DOCK_TOP else docked.height
    val cx = if (full) r else dockedAt.x
    val cy = if (full) DOCK_TOP + r else dockedAt.y
    val sizeButtonAngle = if (full) -90f else -80f
    val rainbowAngle = if (full) 90f else 80f
    val inner = INNER_R * k
}

internal val DOCKED_GEOS = DockEdge.values().associateWith { Geo(false, it) }
internal val POPUP_GEO = Geo(true)

/** What choosing a place on the tool ring does: pick a tool, switch the ruler, or open or close a group. */
internal sealed interface WheelAction {
    class Mode(val mode: ToolMode) : WheelAction

    /** Shows or hides the ruler on the page. It is not a tool of its own: the pen in use stays. */
    object Ruler : WheelAction

    /** Opens the group [id], whose tools do [actions]. */
    class OpenGroup(val id: String, val actions: List<WheelAction>) : WheelAction

    /** Closes the open group. */
    object Back : WheelAction
}

/** One place on the tool ring. More tools go here. The [id] is what the saved selection stores, so it never changes. */
internal class WheelTool(
    val id: String,
    val action: WheelAction,
    @StringRes private val labelRes: Int = 0,
    private val name: String? = null,
    val icon: @Composable () -> Unit,
) {
    /** A group shows the person's own name for it; every other tool a string resource. */
    @Composable
    fun label(): String = name ?: stringResource(labelRes)
}

/** Every tool the wheel can show, in the default order. */
internal val TOOLS = listOf(
    WheelTool("pen", WheelAction.Mode(ToolMode.PEN), R.string.wheel_tool_pen) { ToolNib(PenKind.Pen) },
    WheelTool("highlighter", WheelAction.Mode(ToolMode.HIGHLIGHTER), R.string.wheel_tool_highlighter) { ToolNib(PenKind.Highlighter) },
    WheelTool("eraser", WheelAction.Mode(ToolMode.ERASER), R.string.wheel_tool_eraser) { GraphoIcon(R.drawable.ic_eraser, null, GraphoWheel.icon, size = 22.dp) },
    WheelTool("fill", WheelAction.Mode(ToolMode.FILL), R.string.wheel_tool_fill) { GraphoIcon(R.drawable.ic_paint_bucket, null, GraphoWheel.icon, size = 22.dp) },
    WheelTool("lasso", WheelAction.Mode(ToolMode.LASSO), R.string.wheel_tool_lasso) { GraphoIcon(R.drawable.ic_lasso, null, GraphoWheel.icon, size = 22.dp) },
    WheelTool("shape", WheelAction.Mode(ToolMode.SHAPE), R.string.wheel_tool_shapes) { GraphoIcon(R.drawable.ic_shapes, null, GraphoWheel.icon, size = 22.dp) },
    WheelTool("text", WheelAction.Mode(ToolMode.TEXT), R.string.wheel_tool_text) { GraphoIcon(R.drawable.ic_text, null, GraphoWheel.icon, size = 22.dp) },
    WheelTool("ruler", WheelAction.Ruler, R.string.note_ruler) { GraphoIcon(R.drawable.ic_ruler, null, GraphoWheel.icon, size = 22.dp) },
)

/**
 * The docked tool ring for [count] tools, resting with the middle of the list at the right of the wheel but
 * never further than two places in, so a long list starts at its first tools. It does not wrap.
 */
internal fun newToolSpin(count: Int): SpinState {
    val last = maxOf(count - 1, 0)
    return SpinState(TOOL_STEP, min = -last * TOOL_STEP, max = 0f, start = -minOf(last / 2, 2) * TOOL_STEP)
}

/** The full circle spreads [count] tools evenly round the whole ring, and turning it wraps. */
internal fun newToolRing(count: Int): SpinState = SpinState(if (count > 0) 360f / count else TOOL_STEP)

internal fun toolAngle(full: Boolean, i: Int, offset: Float, count: Int): Float =
    if (full) wrapDeg(i * (360f / count) + offset, 360f) else i * TOOL_STEP + offset

internal fun toolFade(full: Boolean, angle: Float): Float =
    if (full) 1f else ((TOOL_REACH - abs(angle)) / 20f).coerceIn(0f, 1f)

@Composable
private fun ToolNib(kind: PenKind) {
    PenGlyph(
        kind = kind,
        color = GraphoWheel.textMuted,
        outline = GraphoWheel.text,
        modifier = Modifier
            .size(width = 11.dp, height = 30.dp)
            .graphicsLayer { rotationZ = 45f },
    )
}
