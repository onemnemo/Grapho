package com.onemnemo.grapho.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.PopupProperties
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.Notices
import kotlinx.coroutines.delay

// Geometry in dp. The docked disc is centred on the left edge of the canvas; DOCK_TOP is the room above it
// for the size bubble.
internal const val DISC_R = 170f
internal const val INNER_R = 116f
internal const val CORE_R = 58f
internal const val RIM_R = 143f
internal const val DOCK_TOP = 36f
internal const val DISC_CY = DOCK_TOP + DISC_R
internal const val POPUP_R = 150f
internal const val TAB_DEFAULT_Y = 330f
private const val PILL_MS = 1200L

/** The pen panel, colour picker and name pill are popup windows with this: no focus, and no touch outside their bounds. */
internal val OverlayProperties = PopupProperties(
    focusable = false,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    excludeFromSystemGesture = true,
    clippingEnabled = false,
)

private enum class Layer { None, Pens, Picker, Colours, Customise }

/**
 * The pop-up wheel for the canvas. It is in one of these states: closed (just
 * the tab on the left edge), docked (half disc on the left edge), compact (a
 * small circle anywhere), the pen panel beside it, or the colour picker. The
 * picker replaces the wheel while it is up and hands back to it. It only
 * reads and writes [state], so it always agrees with the canvas.
 *
 * The wheel, its handle and the closed tab are children of one box over the canvas, all placed
 * in that box's px, so a carry has a single frame to live in. The box itself takes no touches:
 * only the wheel's own parts do, and the rest of the area reaches the canvas underneath, which
 * keeps its live ink. The pen panel, colour picker and name pill never move, and are popup windows.
 */
@Composable
fun PenWheel(
    state: ToolbarState,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    customise: CustomiseRequest? = null,
    rulerOn: Boolean = false,
    onRulerChange: (Boolean) -> Unit = {},
    notices: Notices,
) {
    var layer by remember(open) { mutableStateOf(Layer.None) }
    var sizeMode by remember(open) { mutableStateOf(false) }
    // The colour in the manager that is picked, as set and place, and whether the picker is editing it.
    var pickedSlot by remember(open) { mutableStateOf<Pair<Int, Int>?>(null) }
    var editing by remember(open) { mutableStateOf(false) }
    // Which segment the customiser opens on.
    var customiseOnPens by remember(open) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val placementStore = remember { WheelPlacementStore(context) }
    val saved = remember { placementStore.load() }
    // Where the wheel lives: docked on [edge] with its disc centre [along] dp along it, or the free circle at floatX, floatY.
    var docked by rememberSaveable { mutableStateOf(saved.docked) }
    var edge by rememberSaveable { mutableStateOf(saved.edge) }
    var along by rememberSaveable { mutableStateOf(saved.along) }
    // Centre of the pop-up circle.
    var floatX by rememberSaveable { mutableStateOf(saved.floatX) }
    var floatY by rememberSaveable { mutableStateOf(saved.floatY) }
    // Settings can dock the wheel on another edge while the note is behind it.
    val settingsRevision = remember { WheelPlacementStore.revision }
    val latestRevision = WheelPlacementStore.revision
    LaunchedEffect(latestRevision) {
        if (latestRevision != settingsRevision) {
            val next = placementStore.load()
            docked = next.docked
            edge = next.edge
            along = next.along
        }
    }
    var pillText by remember { mutableStateOf("") }
    var pillTick by remember { mutableIntStateOf(0) }
    var pillOn by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf(IntSize.Zero) }
    // The host box, whose px every part of the wheel and every finger on it is measured in.
    val hostCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val book = remember(state) { ColourBook(state, ColourStore(context)) }
    val rim = remember { SpinState(RIM_STEP) }
    // The full circle turns in even slots, so its step follows the number of colours.
    val ring = remember { SpinState(stepOf(true, state.rim.size)) }
    val toolStore = remember { WheelToolStore(context) }
    // The group open on the tool ring, which then shows that group's tools.
    var openGroup by remember(open) { mutableStateOf<String?>(null) }
    val toolSlots = toolStore.slots.toList()
    val toolList = remember(toolSlots, openGroup) { ringTools(toolSlots, openGroup) }
    // The rings turn in steps that follow the number of tools, so they start over when it changes.
    val tools = remember(toolList.size, openGroup) { newToolSpin(toolList.size) }
    val toolsFull = remember(toolList.size, openGroup) { newToolRing(toolList.size) }
    val haptic = LocalHapticFeedback.current
    val store = remember { PenStore(context) }
    ring.step = stepOf(true, state.rim.size)
    val origin = remember { FloatArray(2) }
    val density = LocalDensity.current.density
    val hostW = host.width / density
    val hostH = host.height / density
    // A landing is committed the moment the wheel is let go, so the wheel's own place is there at once.
    val motion = remember {
        WheelMotion(
            scope = scope,
            start = saved,
            onTick = { haptic.tick() },
            onPlace = { p ->
                docked = p.docked
                edge = p.edge
                along = p.along
                floatX = p.floatX
                floatY = p.floatY
                placementStore.save(p)
            },
        )
    }
    val moving = motion.active

    // Read here, in composition, so a change of placement from outside a carry reaches the motion.
    val placement = WheelPlacement(docked, edge, along, floatX, floatY)
    SideEffect { motion.sync(density, host.width.toFloat(), host.height.toFloat(), placement) }
    LaunchedEffect(Unit) { state.wheelPinned = placementStore.loadPinned() }
    // The page ignores touches while the wheel is carried.
    SideEffect { state.wheelCarrying = motion.active }
    DisposableEffect(Unit) { onDispose { state.wheelCarrying = false } }
    LaunchedEffect(open) { if (!open) motion.reset() }

    BackHandler(enabled = open) {
        if (layer == Layer.Picker && editing) {
            editing = false
            layer = Layer.Colours
        } else if (layer != Layer.None) {
            layer = Layer.None
        } else if (openGroup != null) {
            openGroup = null
        } else {
            onOpenChange(false)
        }
    }
    // Choosing a tool or switching size mode puts the pen panel away.
    LaunchedEffect(state.mode, sizeMode) {
        if (layer == Layer.Pens) layer = Layer.None
    }
    // Only the pens, Shapes, Text and the lasso have controls to open.
    LaunchedEffect(state.mode) {
        if (!hasEditView(state.mode)) sizeMode = false
    }
    LaunchedEffect(customise) {
        if (customise != null) {
            customiseOnPens = false
            layer = Layer.Customise
        }
    }
    LaunchedEffect(pillTick) {
        if (pillTick > 0) {
            pillOn = true
            delay(PILL_MS)
            pillOn = false
        }
    }

    // No pointer input here: a touch that hits none of the wheel's parts falls through to the canvas.
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { host = it }
            .onGloballyPositioned { hostCoords[0] = it },
    ) {
        val alongLen = if (edge.vertical) hostH else hostW
        val tabMin = TAB_H / 2f
        val tabMax = maxOf(tabMin, alongLen - TAB_H / 2f)
        // Keep the whole disc on screen when there is room for it.
        val discAlong = clampAlong(along, alongLen)
        val discCentreAt = edgePoint(edge, discAlong, hostW, hostH)
        // The circle stays on screen, with room above it for the size bubble.
        val cx = clampCircleX(floatX, hostW)
        val cy = clampCircleY(floatY, hostH)

        val wheelUp = open && layer != Layer.Picker && layer != Layer.Colours && layer != Layer.Customise
        val showPens = open && layer == Layer.Pens && !moving
        val onPens = { layer = if (layer == Layer.Pens) Layer.None else Layer.Pens }
        val closePens = { if (layer == Layer.Pens) layer = Layer.None }

        Presence(visible = !open) { p ->
            WheelTab(
                edge = edge,
                hostW = hostW,
                hostH = hostH,
                centre = { along.coerceIn(tabMin, tabMax) },
                color = Color(currentSetting(state).color),
                progress = p,
                host = hostCoords,
                onOpen = {
                    docked = true
                    placementStore.save(WheelPlacement(docked, edge, along, floatX, floatY))
                    onOpenChange(true)
                },
                onDragStart = { origin[0] = along.coerceIn(tabMin, tabMax) },
                onDragBy = { d ->
                    along = (origin[0] + if (edge.vertical) d.y else d.x).coerceIn(tabMin, tabMax)
                },
                onDragEnd = { placementStore.save(WheelPlacement(docked, edge, along, floatX, floatY)) },
            )
        }

        Presence(visible = wheelUp) { p ->
            WheelBody(
                motion = motion,
                host = hostCoords,
                state = state,
                sizeMode = sizeMode,
                rulerOn = rulerOn,
                onSizeModeChange = { sizeMode = it },
                rim = rim,
                ring = ring,
                tools = tools,
                toolsFull = toolsFull,
                toolList = toolList,
                pinned = state.wheelPinned,
                progress = p,
                // A tap on the grip of the free circle sends it back to the edge it was last docked on.
                onDock = {
                    if (!docked) {
                        docked = true
                        placementStore.save(WheelPlacement(docked, edge, along, floatX, floatY))
                    }
                },
                onTogglePin = {
                    state.wheelPinned = !state.wheelPinned
                    placementStore.savePinned(state.wheelPinned)
                },
                onNib = onPens,
                onPicker = { layer = Layer.Picker },
                onToolAction = { action ->
                    when (action) {
                        is WheelAction.OpenGroup -> openGroup = action.id
                        WheelAction.Back -> openGroup = null
                        is WheelAction.Mode -> {
                            // The tool in use opens its own controls when chosen again: sizes and opacity for the
                            // pens, the options of Shapes, Text and the lasso.
                            if (state.mode == action.mode && hasEditView(action.mode)) {
                                sizeMode = !sizeMode
                            } else {
                                toggleTool(state, action.mode)
                                // Shapes, Text and the lasso show their options as soon as they are chosen.
                                sizeMode = hasToolOptions(state.mode)
                            }
                            openGroup = null
                        }
                        WheelAction.Ruler -> onRulerChange(!rulerOn)
                    }
                },
                onTouch = closePens,
                onCarryStart = { finger ->
                    layer = Layer.None
                    if (motion.begin(finger)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onCarryMove = motion::move,
                // A cancel lands the wheel like a release.
                onCarryEnd = motion::end,
            )
        }

        Presence(visible = showPens) { p ->
            // Beside the wheel, on the page side of it.
            val h = penPanelHeight(store.pens.size, hostH)
            val onRight = hostW - (cx + POPUP_R) >= cx - POPUP_R
            val at = if (!docked) {
                Offset(if (onRight) cx + POPUP_R + 8f else cx - POPUP_R - 8f - PEN_PANEL_W, cy - h / 2f)
            } else when (edge) {
                DockEdge.Left -> Offset(DISC_R + 8f, discCentreAt.y - h / 2f)
                DockEdge.Right -> Offset(hostW - DISC_R - 8f - PEN_PANEL_W, discCentreAt.y - h / 2f)
                DockEdge.Top -> Offset(discCentreAt.x - PEN_PANEL_W / 2f, DISC_R + 8f)
                DockEdge.Bottom -> Offset(discCentreAt.x - PEN_PANEL_W / 2f, hostH - DISC_R - 8f - h)
            }
            val px = at.x
            val py = at.y
            PenPanel(
                state = state,
                store = store,
                progress = p,
                x = px,
                y = py,
                height = h,
                onPick = { preset ->
                    applyPreset(state, preset)
                    pillText = preset.name
                    pillTick++
                    layer = Layer.None
                },
                onEdit = {
                    customiseOnPens = true
                    layer = Layer.Customise
                },
                notices = notices,
            )
        }

        Presence(visible = open && layer == Layer.Picker && !moving) { p ->
            ColorPickerPanel(
                state = state,
                book = book,
                editSlot = if (editing) pickedSlot else null,
                progress = p,
                onAdded = { index ->
                    rim.snapTo(-index * RIM_STEP)
                    ring.snapTo(-index * stepOf(true, state.rim.size))
                },
                onManage = { layer = Layer.Colours },
                onDone = {
                    layer = if (editing) Layer.Colours else Layer.None
                    editing = false
                },
            )
        }

        Presence(visible = open && layer == Layer.Colours && !moving) { p ->
            ColourManagerPanel(
                state = state,
                book = book,
                progress = p,
                notices = notices,
                picked = pickedSlot,
                onPick = { pickedSlot = it },
                onEdit = {
                    editing = true
                    layer = Layer.Picker
                },
                onDone = {
                    pickedSlot = null
                    layer = Layer.None
                },
            )
        }

        Presence(visible = open && layer == Layer.Customise && !moving) { p ->
            CustomisePanel(
                state = state,
                tools = toolStore,
                pens = store,
                startOnPens = customiseOnPens,
                progress = p,
                notices = notices,
                onDone = { layer = Layer.None },
            )
        }

        Presence(visible = pillOn && open && !moving) { p ->
            // Beside the wheel like the pen panel, lined up with its disc.
            // The point beside the wheel, then where across the pill it sits.
            val (at, align) = if (!docked) {
                Offset(cx + POPUP_R + 8f, cy) to Offset(0f, 0.5f)
            } else when (edge) {
                DockEdge.Left -> Offset(DISC_R + 8f, discCentreAt.y) to Offset(0f, 0.5f)
                DockEdge.Right -> Offset(hostW - DISC_R - 8f, discCentreAt.y) to Offset(1f, 0.5f)
                DockEdge.Top -> Offset(discCentreAt.x, DISC_R + 8f) to Offset(0.5f, 0f)
                DockEdge.Bottom -> Offset(discCentreAt.x, hostH - DISC_R - 8f) to Offset(0.5f, 1f)
            }
            NamePill(text = pillText, x = at.x, y = at.y, alignX = align.x, alignY = align.y, progress = p)
        }
    }
}

/** Keeps [content] composed while it animates in and out, with the progress from 0 to 1. */
@Composable
internal fun Presence(visible: Boolean, content: @Composable (progress: Float) -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(if (visible) 1f else 0f, tween(if (visible) 180 else 150))
    }
    if (visible || progress.value > 0f) content(progress.value)
}

/** Fades and scales a popup's content in and out around [origin]. */
internal fun Modifier.popIn(progress: Float, from: Float, origin: TransformOrigin): Modifier =
    graphicsLayer {
        val s = from + (1f - from) * progress
        scaleX = s
        scaleY = s
        alpha = progress
        transformOrigin = origin
    }
