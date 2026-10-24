package com.onemnemo.grapho.ui.note

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.PenGlyph
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.GraphoWheel
import kotlin.math.abs
import kotlin.math.roundToInt

internal val RAINBOW = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF22C55E),
    Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFD946EF), Color(0xFFEF4444),
)

/**
 * The wheel's circle, a little wide of the drawn edge. Clipping the touch handlers to it lets a touch
 * in the corners of the face's box reach the page, and the wide edge keeps the clip from thinning
 * the drawn rim.
 */
private class WheelHit(private val geo: Geo) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val u = density.density
        return Outline.Generic(Path().apply { addOval(Rect(Offset(geo.cx * u, geo.cy * u), (geo.r + 2f) * u)) })
    }
}

/**
 * What the wheel shows: the disc, the colour rim or the sizes, the core with its nib, the tool ring
 * and the fixed rim buttons, laid out by [geo] in a box of the geo's size. The touch handlers in
 * [gestures] sit on the circle only, in the box's own px.
 */
@Composable
internal fun WheelFace(
    geo: Geo,
    state: ToolbarState,
    sizeMode: Boolean,
    rim: SpinState,
    tools: SpinState,
    toolList: List<WheelTool>,
    modifier: Modifier = Modifier,
    gestures: Modifier = Modifier,
    rulerOn: Boolean = false,
    onSizeModeChange: (Boolean) -> Unit = {},
    onCloseEdit: () -> Unit = {},
    onNib: () -> Unit = {},
    onPicker: () -> Unit = {},
) {
    val e = geo.edge
    val full = geo.full
    val ground = GraphoWheel.body
    val inner = GraphoWheel.well
    val density = LocalDensity.current.density
    val setting = currentSetting(state)
    val number = sizeNumber(state)
    val alpha = state.alpha
    val palette = state.palette
    val slots = state.rim
    val inPalette = palette.contains(setting.color)
    val erasing = state.mode == ToolMode.ERASER
    val editing = isEditing(state, sizeMode)
    val options = optionsOpen(state, sizeMode)
    val hubColour = if (options) optionsColour(state) else null
    // 0 at rest, 1 once the open tool's plate has grown out to the rim.
    val grown = animateFloatAsState(if (editing) 1f else 0f, EDIT_SPRING, label = "wheelEdit")
    val openAt = stemAngle(geo, tools, toolList, state)
    val scope = rememberCoroutineScope()
    val latestTools by rememberUpdatedState(toolList)
    val latestState by rememberUpdatedState(state)
    // Opening the controls turns the ring to put the open tool at the page-facing centre, where its plate grows.
    LaunchedEffect(editing) {
        if (!editing) return@LaunchedEffect
        val open = latestTools.indexOfFirst { isModeTool(latestState, it.action) }
        if (open >= 0) tools.glideTo(scope, centredOffset(geo.full, open, latestTools.size, tools.offset))
    }
    val coreTap: () -> Unit = {
        if (hubColour != null) {
            onPicker()
        } else if (state.mode in BACK_TO_PEN_MODES) {
            state.mode = state.drawMode
        } else {
            onNib()
        }
    }
    val hit = remember(geo) { WheelHit(geo) }
    val penLabel = stringResource(R.string.wheel_tool_pen)
    Box(modifier.size(width = geo.width.dp, height = geo.height.dp)) {
        Box(Modifier.fillMaxSize().clip(hit).then(gestures)) {
            Canvas(Modifier.fillMaxSize()) {
                val u = density
                val c = Offset(geo.cx * u, geo.cy * u)
                fun at(r: Float, deg: Float): Offset {
                    val p = polar(r, e.toScreen(deg))
                    return Offset(c.x + p.x * u, c.y + p.y * u)
                }

                // Body, tool well and hub, each with its own edge ring.
                val edge = 2f * u
                drawCircle(ground, geo.r * u, c)
                drawCircle(GraphoWheel.ring, (geo.r - 1f) * u, c, style = Stroke(width = edge))
                drawCircle(inner, geo.inner * u, c)
                drawCircle(GraphoWheel.ring, (geo.inner + 1f) * u, c, style = Stroke(width = edge))
                drawCircle(GraphoWheel.hub, geo.core * u, c)
                drawCircle(GraphoWheel.hubEdge, (geo.core + 1f) * u, c, style = Stroke(width = edge))

                if (erasing) {
                    drawEraserSizes(geo, c, u, state.eraserPt)
                } else if (options) {
                    // The tool's sizes are dots on the rim; its choices are buttons laid over the lower rim.
                    dotRow(state)?.let { drawDotRow(geo, c, u, it) }
                } else if (sizeMode) {
                    for (n in DOT_NUMBERS) {
                        drawCircle(GraphoWheel.dot, dotRadius(n) * u, at(geo.rimR, sizeAngle(n)))
                    }
                    val selected = DOT_NUMBERS.minByOrNull { abs(it - number) } ?: 1f
                    val sp = at(geo.rimR, sizeAngle(selected))
                    drawCircle(GraphoWheel.selectedIndicator, dotRadius(selected) * u, sp)
                    drawCircle(GraphoWheel.selectedIndicator, (dotRadius(selected) + 5f) * u, sp, style = Stroke(width = 2f * u))
                    drawOpacityBand(c, geo.rimR, u, Color(setting.color), alpha, e)
                } else {
                    val n = slots.size
                    val step = stepOf(geo, n)
                    for (i in 0 until n) {
                        val a = wrapDeg(i * step + rim.offset, n * step)
                        val fade = swatchFade(geo, a, step)
                        if (fade <= 0f) continue
                        val argb = slots[i]
                        if (argb == RIM_STRIP) {
                            // A short radial line in the slot between two sets.
                            val inside = at(geo.rimR - 9f, a)
                            val outside = at(geo.rimR + 9f, a)
                            drawLine(GraphoWheel.hubEdge.copy(alpha = fade), inside, outside, strokeWidth = 2f * u)
                            continue
                        }
                        val p = at(geo.rimR, a)
                        drawCircle(Color(argb).copy(alpha = fade), geo.swatchR * u, p)
                        val edgeInk = GraphoWheel.edge.copy(alpha = GraphoWheel.edge.alpha * fade)
                        drawCircle(edgeInk, geo.swatchR * u, p, style = Stroke(width = 1f * u))
                        if (argb == setting.color) {
                            val ring = Stroke(width = 2f * u)
                            drawCircle(GraphoWheel.swatchRing.copy(alpha = fade), (geo.swatchR + 4f) * u, p, style = ring)
                        }
                    }
                }
                if (grown.value > 0.002f) drawStem(geo, c, u, openAt, grown.value)
                drawScroll(geo, c, u, tools, toolList.size, 1f - (grown.value * 1.5f).coerceIn(0f, 1f))
            }

            // Core: the nib, pointing into the page. Tap for the pen panel, or to leave eraser and fill.
            val coreModifier = Modifier.pressable(scaleOnPress = false) { coreTap() }
            Box(
                Modifier
                    .offset(x = (geo.cx - geo.core).dp, y = (geo.cy - geo.core).dp)
                    .size((geo.core * 2f).dp)
                    .clip(CircleShape)
                    .then(coreModifier)
                    .semantics { contentDescription = penLabel },
            )
            val nibCentre = polar(26f, e.face)
            if (hubColour != null) {
                // Shapes and Text show their colour in the hub, and tapping it opens the picker.
                Box(
                    Modifier
                        .offset(
                            x = (if (full) geo.cx - 16f else geo.cx + nibCentre.x - 16f).dp,
                            y = (if (full) geo.cy - 16f else geo.cy + nibCentre.y - 16f).dp,
                        )
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(hubColour)
                        .border(2.dp, GraphoWheel.swatchRing, CircleShape),
                )
            } else {
                PenGlyph(
                    kind = nibKind(state),
                    color = tipColor(state),
                    outline = GraphoWheel.textMuted,
                    modifier = Modifier
                        .offset(
                            x = (if (full) geo.cx - 12f else geo.cx + nibCentre.x - 12f).dp,
                            y = (if (full) geo.cy - 33f else geo.cy + nibCentre.y - 33f).dp,
                        )
                        .size(width = 24.dp, height = 66.dp)
                        .graphicsLayer { rotationZ = if (full) 50f else 90f + e.face },
                )
            }

            // Tool ring: it turns like the rim and keeps to the right of the wheel. While the open tool's
            // controls are up, the others fold into it and the tool itself rides on its grown plate.
            for (i in toolList.indices) {
                val a = toolAngle(full, i, tools.offset, toolList.size)
                val fade = toolFade(full, a)
                if (fade <= 0f) continue
                val tool = toolList[i]
                val toolLabel = tool.label()
                val open = isModeTool(state, tool.action)
                val active = isActive(state, rulerOn, tool.action)
                Box(
                    Modifier
                        .offset {
                            val p = polar(geo.toolR, e.toScreen(foldedAngle(a, openAt, open, grown.value)))
                            IntOffset(((geo.cx + p.x - 22f) * density).roundToInt(), ((geo.cy + p.y - 22f) * density).roundToInt())
                        }
                        .size(44.dp)
                        .graphicsLayer {
                            val gone = collapsed(open, grown.value)
                            this.alpha = fade * (1f - gone)
                            scaleX = 1f - 0.3f * gone
                            scaleY = 1f - 0.3f * gone
                        }
                        .clip(CircleShape)
                        .drawBehind { if (active && grown.value < 0.002f) drawCircle(GraphoWheel.plate) }
                        .semantics { contentDescription = toolLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    tool.icon()
                }
            }

            // Fixed buttons on the rim. The eraser has its own two instead.
            if (erasing) {
                EraserModeButtons(geo, state)
            } else if (options) {
                ToolOptionButtons(geo, state)
            } else if (!sizeMode) {
                val sizeButton = polar(geo.rimR, e.toScreen(geo.sizeButtonAngle))
                val sizeAt = Offset(geo.cx + sizeButton.x, geo.cy + sizeButton.y)
                WheelButton(sizeAt.x, sizeAt.y, stringResource(R.string.wheel_size), onClick = { onSizeModeChange(true) }) {
                    Canvas(Modifier.size(26.dp)) {
                        drawCircle(GraphoWheel.glyphRing, 13f.dp.toPx(), style = Stroke(width = 1.5f.dp.toPx()))
                        drawCircle(Color(setting.color), dotRadius(number.toFloat()).dp.toPx())
                    }
                }
                val rainbow = polar(geo.rimR, e.toScreen(geo.rainbowAngle))
                WheelButton(geo.cx + rainbow.x, geo.cy + rainbow.y, stringResource(R.string.colour_custom), onClick = onPicker) {
                    Canvas(Modifier.size(26.dp)) {
                        drawCircle(Brush.sweepGradient(RAINBOW, center), radius = 12f.dp.toPx())
                        if (!inPalette) {
                            drawCircle(GraphoWheel.swatchRing, 16f.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }
        }

        if (erasing) {
            val size = eraserNumber(state)
            StemReadout(geo, openAt, grown, size.toString(), stringResource(R.string.wheel_eraser_size, size), onCloseEdit)
        } else if (options) {
            val label = stringResource(optionsLabel(state.mode))
            StemReadout(geo, openAt, grown, optionsReadout(state), label, onCloseEdit)
        } else if (sizeMode) {
            StemReadout(geo, openAt, grown, number.toString(), stringResource(R.string.wheel_size_back, number), onCloseEdit)
        }
    }
}
