package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.hypot

private const val SHADOW_STEPS = 4
private const val HINT_ALPHA = 0.10f

/** Where a face that is not the wheel's shape at rest is kept, far off the host, so it can never be hit. */
private const val AWAY_PX = -100_000f

private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

/**
 * The wheel as children of the host box, which is also the frame everything is placed in: both
 * shapes stay composed while the wheel is up, the disc and the circle, each at offset zero and
 * moved by layer translation to [WheelMotion.centre], with the handle riding along. They crossfade
 * through [WheelMotion.shape]. A carry therefore never recomposes or lays out. [progress] fades the
 * whole group in and out.
 */
@Composable
internal fun WheelBody(
    motion: WheelMotion,
    host: Array<LayoutCoordinates?>,
    state: ToolbarState,
    sizeMode: Boolean,
    rulerOn: Boolean,
    onSizeModeChange: (Boolean) -> Unit,
    rim: SpinState,
    ring: SpinState,
    tools: SpinState,
    toolsFull: SpinState,
    toolList: List<WheelTool>,
    pinned: Boolean,
    progress: Float,
    onDock: () -> Unit,
    onTogglePin: () -> Unit,
    onNib: () -> Unit,
    onPicker: () -> Unit,
    onToolAction: (WheelAction) -> Unit,
    onTouch: () -> Unit,
    onCarryStart: (Offset) -> Unit,
    onCarryMove: (Offset) -> Unit,
    onCarryEnd: () -> Unit,
) {
    val from = if (motion.docked) 0.85f else 0.3f
    Layout(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = from + (1f - from) * progress
                scaleX = s
                scaleY = s
                alpha = progress
                if (progress < 1f && size.width > 0f && size.height > 0f) {
                    val c = motion.centre
                    transformOrigin = TransformOrigin(c.x / size.width, c.y / size.height)
                }
            },
        content = {
            DropHint(motion)
            // The disc follows the placement's edge, which a landing commits the moment it is let go.
            WheelPiece(
                circle = false,
                geo = DOCKED_GEOS.getValue(motion.edge),
                motion = motion,
                host = host,
                state = state,
                sizeMode = sizeMode,
                rulerOn = rulerOn,
                rim = rim,
                tools = tools,
                toolList = toolList,
                onSizeModeChange = onSizeModeChange,
                onNib = onNib,
                onPicker = onPicker,
                onToolAction = onToolAction,
                onTouch = onTouch,
                onCarryStart = onCarryStart,
                onCarryMove = onCarryMove,
                onCarryEnd = onCarryEnd,
            )
            WheelPiece(
                circle = true,
                geo = POPUP_GEO,
                motion = motion,
                host = host,
                state = state,
                sizeMode = sizeMode,
                rulerOn = rulerOn,
                rim = ring,
                tools = toolsFull,
                toolList = toolList,
                onSizeModeChange = onSizeModeChange,
                onNib = onNib,
                onPicker = onPicker,
                onToolAction = onToolAction,
                onTouch = onTouch,
                onCarryStart = onCarryStart,
                onCarryMove = onCarryMove,
                onCarryEnd = onCarryEnd,
            )
            WheelHandle(
                motion = motion,
                host = host,
                pinned = pinned,
                onToggle = onTogglePin,
                onGripTap = onDock,
                onCarryStart = onCarryStart,
                onCarryMove = onCarryMove,
                onCarryEnd = onCarryEnd,
            )
        },
    ) { measurables, constraints ->
        // Every part is measured at its own size, however small the host, and sits at the origin.
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}

/** A quiet ghost of the half disc on the edge a free circle would dock on if let go now. */
@Composable
private fun DropHint(motion: WheelMotion) {
    val edge = motion.hintEdge ?: return
    val density = LocalDensity.current.density
    val ink = GraphoWheel.body
    Box(
        Modifier
            .size((2f * DISC_R).dp)
            .graphicsLayer {
                val c = motion.hintCentre(edge)
                translationX = c.x - DISC_R * density
                translationY = c.y - DISC_R * density
            }
            // A whole circle on the edge's point: the host clips off the half outside it.
            .drawBehind { drawCircle(ink.copy(alpha = HINT_ALPHA * motion.hintFade), DISC_R * density) },
    )
}

/**
 * One shape of the wheel. It is placed so its geo centre sits on the wheel's centre, and is
 * out of reach unless it is the shape at rest or the wheel is moving. Its touch handlers only
 * answer while it is the shape at rest. The carry handler answers whenever no finger has the wheel
 * yet, and is keyed on nothing that changes during a carry, since it must outlive the whole carry.
 */
@Composable
private fun WheelPiece(
    circle: Boolean,
    geo: Geo,
    motion: WheelMotion,
    host: Array<LayoutCoordinates?>,
    state: ToolbarState,
    sizeMode: Boolean,
    rulerOn: Boolean,
    rim: SpinState,
    tools: SpinState,
    toolList: List<WheelTool>,
    onSizeModeChange: (Boolean) -> Unit,
    onNib: () -> Unit,
    onPicker: () -> Unit,
    onToolAction: (WheelAction) -> Unit,
    onTouch: () -> Unit,
    onCarryStart: (Offset) -> Unit,
    onCarryMove: (Offset) -> Unit,
    onCarryEnd: () -> Unit,
) {
    val density = LocalDensity.current.density
    val wheelLabel = stringResource(R.string.settings_pen_wheel)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sizeNow by rememberUpdatedState(sizeMode)
    val latestAction by rememberUpdatedState(onToolAction)
    val latestSizeMode by rememberUpdatedState(onSizeModeChange)
    val stateNow by rememberUpdatedState(state)
    val toolsNow by rememberUpdatedState(toolList)
    // Leaves the open tool's controls: the eraser goes back to the pen, the pen's controls fold away.
    val closeEdit = {
        if (stateNow.mode == ToolMode.ERASER) toggleTool(stateNow, ToolMode.ERASER) else latestSizeMode(false)
    }
    val latestTouch by rememberUpdatedState(onTouch)
    val geoNow by rememberUpdatedState(geo)
    SideEffect {
        rim.onDetent = { haptic.tick() }
        tools.onDetent = { haptic.tick() }
    }
    val atRest = { circle != motion.docked && !motion.active }

    val place = Modifier
        .graphicsLayer {
            val m = motion.shape
            val s = smooth(m)
            val faded = if (circle) s else 1f - s
            val c = motion.centre
            val away = faded <= 0f && !motion.active
            translationX = if (away) AWAY_PX else c.x - geo.cx * density
            translationY = c.y - geo.cy * density
            alpha = faded
            val grown = if (circle) mix(DOCKED_SCALE, 1f, m) * (1f + CARRY_SCALE * motion.lift) else 1f
            scaleX = grown
            scaleY = grown
            transformOrigin = TransformOrigin(geo.cx / geo.width, geo.cy / geo.height)
        }
        .drawBehind {
            // Soft shadow of the lifted circle, a little below it, scaled with the circle's layer.
            val lift = motion.lift
            if (circle && lift > 0f) {
                val under = Offset(geo.cx * density, (geo.cy + 6f * lift) * density)
                for (i in SHADOW_STEPS - 1 downTo 0) {
                    drawCircle(Color.Black.copy(alpha = 0.04f * lift), (POPUP_R + 3f * i) * density, under)
                }
            }
        }
        .semantics { if (circle != motion.docked) contentDescription = wheelLabel }

    val gestures = Modifier
        .pointerInput(Unit) {
            // Watches every press first without consuming it, to tell the panel something else was touched.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!atRest()) return@awaitEachGesture
                val g = geoNow
                val dx = down.position.x / density - g.cx
                val dy = down.position.y / density - g.cy
                if (hypot(dx, dy) > g.core) latestTouch()
            }
        }
        .carryGesture(
            host = host,
            enabled = { !motion.held },
            onWheel = { p ->
                val g = geoNow
                hypot(p.x / density - g.cx, p.y / density - g.cy) <= g.r
            },
            onStart = onCarryStart,
            onMove = onCarryMove,
            onEnd = onCarryEnd,
        )
        .pointerInput(geo, rim, tools, toolList) {
            rimGestures(
                geo, state, rim, tools, toolList.size, scope, haptic, { sizeNow }, atRest,
                stemAt = { stemAngle(geoNow, tools, toolsNow, stateNow) },
                onStem = closeEdit,
            ) { i ->
                toolList.getOrNull(i)?.let { latestAction(it.action) }
            }
        }

    WheelFace(
        geo = geo,
        state = state,
        sizeMode = sizeMode,
        rim = rim,
        tools = tools,
        toolList = toolList,
        modifier = place,
        gestures = gestures,
        rulerOn = rulerOn,
        onSizeModeChange = onSizeModeChange,
        onCloseEdit = closeEdit,
        onNib = onNib,
        onPicker = onPicker,
    )
}
