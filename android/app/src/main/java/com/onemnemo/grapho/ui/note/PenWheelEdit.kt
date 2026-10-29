package com.onemnemo.grapho.ui.note

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.hypot
import kotlin.math.roundToInt

/** How the edit view opens and closes: a little spring, so the plate settles rather than stops. */
internal val EDIT_SPRING = spring<Float>(dampingRatio = 0.82f, stiffness = 380f)

/** The radius of the grown plate in dp on the docked wheel; the full circle draws it a little thinner. */
private const val STEM_R = 21f
private const val STEM_OVERSHOOT = 1.06f

/** How close a touch has to be to the plate's centre line to hit it, in dp. */
private const val STEM_HIT = 24f

/** How far the other tools fold toward the open one, as a share of the way. */
private const val FOLD = 0.8f
private const val SCROLL_SPAN = 78f
private const val SCROLL_WIDTH = 3f

/** True while the pen's own controls or the eraser's are open on the rim. */
internal fun isEditing(state: ToolbarState, sizeMode: Boolean): Boolean = sizeMode || state.mode == ToolMode.ERASER

/** The pen and the highlighter open sizes and opacity; Shapes, Text and the lasso open their options. */
internal fun hasEditView(mode: ToolMode): Boolean = mode == ToolMode.PEN || mode == ToolMode.HIGHLIGHTER || hasToolOptions(mode)

/** The angle on the tool ring of the tool in use, in the left-docked layout. It is zero when that tool is not on the ring. */
internal fun stemAngle(geo: Geo, tools: SpinState, toolList: List<WheelTool>, state: ToolbarState): Float {
    val i = toolList.indexOfFirst { isModeTool(state, it.action) }
    return if (i < 0) 0f else toolAngle(geo.full, i, tools.offset, toolList.size)
}

/**
 * Where the tool ring has to rest to put place [index] of [count] at the page-facing centre, for the edit view:
 * the nearest turn of the full circle, or the one stop of the docked ring.
 */
internal fun centredOffset(full: Boolean, index: Int, count: Int, offset: Float): Float =
    if (full) {
        val step = 360f / count
        -index * step + 360f * ((offset + index * step) / 360f).roundToInt()
    } else {
        -index * TOOL_STEP
    }

/** Where a tool at [angle] sits while the edit view is [grown]: the other tools fold toward the open one at [openAt]. */
internal fun foldedAngle(angle: Float, openAt: Float, open: Boolean, grown: Float): Float =
    if (open) angle else angle + normDeg(openAt - angle) * grown * FOLD

/** How far a tool that is not open has shrunk and faded away, from 0 to 1. */
internal fun collapsed(open: Boolean, grown: Float): Float = if (open) 0f else (grown * 1.5f).coerceIn(0f, 1f)

/**
 * The plate of the open tool grown out to the rim, as a capsule along the tool's angle. [grown] is 0 at rest and
 * 1 once open; it overshoots a little while it springs.
 */
internal fun DrawScope.drawStem(geo: Geo, c: Offset, u: Float, angle: Float, grown: Float) {
    val r = STEM_R * (if (geo.full) geo.r / DISC_R else 1f)
    val near = geo.toolR
    val far = geo.toolR + (geo.rimR - geo.toolR) * grown.coerceIn(0f, STEM_OVERSHOOT)
    val capsule = Path().apply {
        moveTo(near, -r)
        lineTo(far, -r)
        arcTo(Rect(Offset(far - r, -r), Size(2f * r, 2f * r)), -90f, 180f, false)
        lineTo(near, r)
        arcTo(Rect(Offset(near - r, -r), Size(2f * r, 2f * r)), 90f, 180f, false)
        close()
    }
    withTransform({
        translate(c.x, c.y)
        rotate(geo.edge.toScreen(angle), Offset.Zero)
        scale(u, u, Offset.Zero)
    }) {
        drawPath(capsule, GraphoWheel.plate)
        drawPath(capsule, GraphoWheel.bandEdge, style = Stroke(width = 1.5f))
    }
}

/** True when ([dx], [dy]) dp from the wheel's centre is on the grown plate that runs out along [angle]. */
internal fun isOnStem(geo: Geo, angle: Float, dx: Float, dy: Float): Boolean {
    val a = polar(geo.toolR, geo.edge.toScreen(angle))
    val b = polar(geo.rimR, geo.edge.toScreen(angle))
    val vx = b.x - a.x
    val vy = b.y - a.y
    val along = (((dx - a.x) * vx + (dy - a.y) * vy) / (vx * vx + vy * vy)).coerceIn(0f, 1f)
    return hypot(dx - (a.x + vx * along), dy - (a.y + vy * along)) <= STEM_HIT
}

/** The size number at the end of the grown plate, fading in as the plate reaches the rim. */
@Composable
internal fun StemReadout(geo: Geo, angle: Float, grown: State<Float>, text: String, description: String, onClose: () -> Unit) {
    val p = polar(geo.rimR, geo.edge.toScreen(angle))
    Box(
        Modifier
            .offset(x = (geo.cx + p.x - 20f).dp, y = (geo.cy + p.y - 9f).dp)
            .size(width = 40.dp, height = 18.dp)
            .graphicsLayer { alpha = ((grown.value - 0.5f) * 2f).coerceIn(0f, 1f) }
            .semantics {
                contentDescription = description
                onClick { onClose(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Grapho.type.label.copy(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = GraphoWheel.text,
            maxLines = 1,
        )
    }
}

/**
 * The marker on the tool band's edge for a docked ring with more tools than fit: a track for the whole list and
 * a thumb for the tools in view. [fade] takes it away while the edit view is open.
 */
internal fun DrawScope.drawScroll(geo: Geo, c: Offset, u: Float, tools: SpinState, count: Int, fade: Float) {
    if (geo.full || fade <= 0f || count < 2 || count * TOOL_STEP <= 2f * TOOL_REACH - 20f) return
    val r = (geo.inner - 5f) * u
    val last = (count - 1).toFloat()
    val lo = ((-TOOL_REACH - tools.offset) / TOOL_STEP).coerceIn(0f, last)
    val hi = ((TOOL_REACH - tools.offset) / TOOL_STEP).coerceIn(0f, last)
    fun arc(from: Float, to: Float, color: Color) {
        drawArc(
            color = color.copy(alpha = fade),
            startAngle = geo.edge.toScreen(from),
            sweepAngle = geo.edge.mirror * (to - from),
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(2f * r, 2f * r),
            style = Stroke(width = SCROLL_WIDTH * u, cap = StrokeCap.Round),
        )
    }
    val from = -SCROLL_SPAN + 2f * SCROLL_SPAN * lo / last
    val to = -SCROLL_SPAN + 2f * SCROLL_SPAN * hi / last
    arc(-SCROLL_SPAN, SCROLL_SPAN, GraphoWheel.scrollTrack)
    arc(from, maxOf(to, from + 1f), GraphoWheel.dot)
}
