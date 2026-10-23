package com.onemnemo.grapho.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.note.ToolMode
import com.onemnemo.grapho.ui.components.BusyOverlay
import com.onemnemo.grapho.ui.components.LoadingScreen
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.components.PaperColorDialog
import com.onemnemo.grapho.ui.components.SplashPhase
import com.onemnemo.grapho.ui.components.rememberInlineEditState
import com.onemnemo.grapho.ui.components.rememberSplashPhase
import com.onemnemo.grapho.ui.settings.AppSettings
import com.onemnemo.grapho.ui.settings.GestureAction
import com.onemnemo.grapho.ui.settings.fingerGesture
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** The note screen: the note bar and the page, with the pen wheel and the layers panel on top of the page. */
@Composable
fun NoteScreen(
    file: File,
    notices: Notices,
    settings: AppSettings,
    onSettings: () -> Unit,
    customiseWheel: Boolean,
    onWheelCustomised: () -> Unit,
    onBack: () -> Unit,
    onClosed: (Int?) -> Unit,
    openingTitle: String? = null,
    openingInfinite: Boolean? = null,
) {
    val c = Grapho.colors
    val context = LocalContext.current
    val session = remember(file) { NoteSession(file) }
    val latestOnClosed by rememberUpdatedState(onClosed)
    val barStyle = settings.noteBar
    val barPosition = settings.barPosition
    val geometry = barGeometry(barPosition, barStyle)

    DisposableEffect(session) {
        session.open()
        // Leaving the screen, by any route, saves and closes the note.
        onDispose { session.finish { latestOnClosed(it) } }
    }
    // Once the note is open, its content decides what Back closes first.
    BackHandler(enabled = session.notebook == null, onBack = onBack)
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { session.saveSoon() }

    val saveError = session.saveError
    LaunchedEffect(saveError) {
        if (saveError != null) notices.show(context.getString(saveError), isError = true)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.surfaceRaised)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val notebook = session.notebook
        val splash = rememberSplashPhase(
            loading = (notebook == null || !session.canvasShown) && session.openError == null,
            showAfterMs = NOTE_SPLASH_AFTER_MS,
        )
        // The page builds on the main thread, which can hold the screen still for a while. It starts only after
        // the loading screen has had time to draw, so the person sees it instead of a frozen blank page.
        var mounted by remember { mutableStateOf(false) }
        LaunchedEffect(notebook != null) {
            if (notebook != null) {
                delay(NOTE_MOUNT_AFTER_MS)
                mounted = true
            }
        }
        if (notebook == null || !mounted) {
            val error = session.openError
            Box(
                Modifier.fillMaxSize().padding(geometry.reserve.toPadding()).background(c.surfaceBase),
                contentAlignment = Alignment.Center,
            ) {
                if (error != null) {
                    Text(stringResource(error), style = Grapho.type.body, color = c.textMuted)
                }
            }
            NoteBar(
                style = barStyle,
                title = "",
                untitled = stringResource(R.string.note_untitled),
                onBack = onBack,
                onRename = null,
                titleEdit = rememberInlineEditState(),
                modifier = Modifier.align(barPosition.alignment()),
                position = barPosition,
                maxLength = floatingBarMaxWidthDp(
                    if (barPosition.vertical) maxHeight.value else maxWidth.value,
                    GraphoSpace.sizeBarNoteMax.value,
                ).dp,
            )
        } else {
            NoteContent(
                notebook,
                session,
                notices,
                NoteHooks(settings, onSettings, customiseWheel, onWheelCustomised, onBack),
                maxWidth,
                maxHeight,
                barStyle,
            )
        }
        // Over everything, so the note can draw its first page underneath while the loading screen shows.
        if (splash != SplashPhase.Hidden) {
            LoadingScreen(
                cheering = splash == SplashPhase.Cheering,
                title = openingTitle,
                kind = openingInfinite?.let {
                    stringResource(if (it) R.string.loading_kind_canvas else R.string.loading_kind_notebook)
                },
            )
        }
    }
}

private const val TOOL_SAVE_DEBOUNCE_MS = 300L
private const val NOTE_SPLASH_AFTER_MS = 120L
private const val NOTE_MOUNT_AFTER_MS = 180L

@Composable
private fun NoteContent(
    notebook: Notebook,
    session: NoteSession,
    notices: Notices,
    hooks: NoteHooks,
    windowWidth: Dp,
    windowHeight: Dp,
    barStyle: NoteBarStyle,
) {
    val context = LocalContext.current
    val onBack = hooks.onBack
    val toolSettings = remember { ToolSettingsStore(context) }
    val toolbar = remember { ToolbarState().also { toolSettings.load(it) } }
    // Saved shortly after the last change, so a slider drag does not write every frame.
    LaunchedEffect(toolbar) {
        snapshotFlow { toolbar.settings() }
            .distinctUntilChanged()
            .collectLatest {
                delay(TOOL_SAVE_DEBOUNCE_MS)
                toolSettings.save(toolbar)
            }
    }
    DisposableEffect(toolbar) {
        onDispose { toolSettings.save(toolbar) }
    }
    val ui = remember {
        NoteUiState().also {
            it.fingerDraws = hooks.settings.fingerDraws
            it.shapeDetection = hooks.settings.shapeDetection
        }
    }
    // The defaults can change from Settings while the note is open.
    LaunchedEffect(hooks.settings.fingerDraws) { ui.fingerDraws = hooks.settings.fingerDraws }
    LaunchedEffect(hooks.settings.shapeDetection) { ui.shapeDetection = hooks.settings.shapeDetection }
    val titleEdit = rememberInlineEditState()
    FullScreenEffect(ui.fullScreen)
    val info = session.info

    // Undo, redo and add page change the document, so make sure they get saved.
    fun edited() {
        ui.pasteAt = null
        notebook.markChanged()
        session.onEdited()
    }

    val pdfInserter = rememberPdfInserter(session, { ui.view[0] }, notices, ::edited)
    val imageInserter = rememberImageInserter(session, { ui.view[0] }, notices, ::edited)
    val diagramCleaner = rememberDiagramCleaner(session, { ui.view[0] }, notices, ::edited)
    val imageReplacer = rememberImageReplacer(session, { ui.view[0] }, notices, ::edited)
    val selectionImager = rememberSelectionImager({ ui.view[0] }, notices)
    val exporter = rememberNoteExporter(session, { ui.view[0] }, notices, info?.title.orEmpty())
    val sharer = rememberNoteSharer(session, notices, info?.title.orEmpty())

    // The canvas reports layer changes from now on; this reads the page that is in view when the panel opens.
    LaunchedEffect(ui.layersOpen) {
        if (ui.layersOpen) ui.pageLayers = ui.view[0]?.layers?.current()
    }

    // The paste card belongs to the lasso, so it goes when the tool does.
    LaunchedEffect(toolbar.mode) {
        ui.pasteAt = null
        if (toolbar.mode != ToolMode.SHAPE) ui.shapeOptionsOpen = false
        if (toolbar.mode != ToolMode.TEXT) ui.textOptionsOpen = false
    }

    // The wheel sets the text look on the toolbar; the box being typed into follows it.
    LaunchedEffect(toolbar.text) {
        val open = ui.textEdit ?: return@LaunchedEffect
        if (open.style != toolbar.text) ui.view[0]?.setTextStyle(toolbar.text)
    }

    // Back closes the layers panel, then crop, then the selection, before it leaves the note.
    BackHandler { if (ui.dismissable) ui.dismissTop() else onBack() }

    val actions =
        noteActions(session, ui, titleEdit, pdfInserter, imageInserter, exporter, sharer, hooks.onSettings, ::edited)
    LaunchedEffect(hooks.customiseWheel) {
        if (hooks.customiseWheel) {
            actions.onCustomiseWheel()
            hooks.onWheelCustomised()
        }
    }
    val toggles = NoteToggles(
        fingerDraws = ui.fingerDraws,
        rulerOn = ui.rulerOn,
        protractorOn = ui.protractorOn,
        fullScreen = ui.fullScreen,
        onFingerDraws = {
            ui.fingerDraws = it
            hooks.settings.changeFingerDraws(it)
        },
        onRuler = { ui.rulerOn = it },
        onProtractor = { ui.protractorOn = it },
        onFullScreen = { ui.fullScreen = it },
    )
    val position = hooks.settings.barPosition
    val geometry = barGeometry(position, barStyle)
    val tip = position.tipSide
    val barFit = barFitFor(
        tools = hooks.settings.barTools,
        mode = toolbar.mode,
        position = position,
        style = barStyle,
        wantTitle = hooks.settings.showTitle,
        windowWidth = windowWidth,
        windowHeight = windowHeight,
    )
    val visibility = ui.barVisibility
    val autoHide = hooks.settings.autoHide
    val barHeld = visibility.menuOpen || ui.insertOpen || ui.imageToolOpen || ui.layersOpen || titleEdit.editing
    val barHidden = rememberBarHidden(visibility, autoHide, ui.readMode, barHeld)
    val insertBar = insertOnBar(windowWidth.value)
    val hoverBand = with(LocalDensity.current) { (geometry.travel + GraphoSpace.spaceLg).toPx() }
    SideEffect {
        ui.penHover.active = barHidden && autoHide == AutoHide.Always && !ui.readMode
        ui.penHover.position = position
        ui.penHover.band = hoverBand
        ui.penHover.onNear = visibility::summon
        ui.penHover.onStay = visibility::touched
    }

    val readPosition = if (ui.readMode) {
        ReadProgress.position(ui.readPage, info?.pageIds?.size ?: 0, info?.infinite == true)
    } else {
        null
    }

    Box(Modifier.fillMaxSize().watchPenHover(ui.penHover)) {
        Box(Modifier.fillMaxSize().padding(geometry.reserve.toPadding())) {
            NoteCanvas(notebook, session, notices, toolbar, ui) { fingers ->
                when (fingerGesture(fingers)?.let(hooks.settings::gestureAction)) {
                    GestureAction.Undo -> ui.view[0]?.undo()?.also { edited() }
                    GestureAction.Redo -> ui.view[0]?.redo()?.also { edited() }
                    GestureAction.Off, null -> Unit
                }
            }
            NoteCardLayer(
                ui,
                toolbar,
                diagramCleaner,
                imageReplacer,
                selectionImager,
                geometry.overlay.clearOf(GraphoSpace.spaceSm.value),
            )
            // The wheel is drawn over the canvas and takes touches only on its own parts; the rest reach the canvas.
            // Reading puts it away, since nothing can be drawn.
            if (!ui.readMode) {
                PenWheel(
                    state = toolbar,
                    modifier = Modifier.padding(geometry.overlay.toPadding()),
                    open = ui.wheelOpen,
                    onOpenChange = { ui.wheelOpen = it },
                    customise = ui.wheelCustomise,
                    rulerOn = ui.rulerOn,
                    onRulerChange = { ui.rulerOn = it },
                    notices = notices,
                )
            }
            if (pdfInserter.busy) BusyOverlay(stringResource(R.string.note_inserting_pdf))
            if (imageInserter.busy) BusyOverlay(stringResource(R.string.note_inserting_image))
            if (exporter.busy || sharer.busy) BusyOverlay(stringResource(R.string.note_exporting))
        }
        BarSlide(barHidden, position, geometry.travel, Modifier.align(position.alignment())) {
            NoteBar(
                style = barStyle,
                title = info?.title.orEmpty(),
                untitled = stringResource(R.string.note_untitled),
                onBack = onBack,
                onRename = { name ->
                    session.rename(name)
                    ui.view[0]?.reload()
                },
                titleEdit = titleEdit,
                position = position,
                maxLength = floatingBarMaxWidthDp(
                    if (position.vertical) windowHeight.value else windowWidth.value,
                    GraphoSpace.sizeBarNoteMax.value,
                ).dp,
                showTitle = barFit.showTitle,
                readMode = ui.readMode,
                reading = readPosition,
                onTouched = visibility::touched,
                tools = {
                    if (!ui.readMode) {
                        NoteBarTools(
                            tools = barFit.tools,
                            mode = toolbar.mode,
                            onTool = { pickBarTool(toolbar, ui, it) },
                            imageOpen = ui.imageToolOpen,
                            onImageOpen = { ui.imageToolOpen = it },
                            actions = actions,
                            vertical = position.vertical,
                            tip = tip,
                            onShapesPlaced = { ui.shapesAnchor = it },
                            onTextPlaced = { ui.textAnchor = it },
                        )
                    }
                },
            ) {
                if (ui.readMode) {
                    ReadBarActions(readPosition, onEdit = ui::exitRead, vertical = position.vertical, tip = tip)
                } else {
                    NoteBarActions(
                        info = info,
                        insertOnBar = insertBar,
                        layersOpen = ui.layersOpen,
                        onLayers = {
                            ui.pasteAt = null
                            ui.layersOpen = !ui.layersOpen
                        },
                        insertOpen = ui.insertOpen,
                        onInsertOpen = { ui.insertOpen = it },
                        onUndo = {
                            ui.view[0]?.undo()
                            edited()
                        },
                        onRedo = {
                            ui.view[0]?.redo()
                            edited()
                        },
                        toggles = toggles,
                        actions = actions,
                        vertical = position.vertical,
                        tip = tip,
                        onMoreOpen = { visibility.menuOpen = it },
                        onLayersPlaced = { ui.layersAnchor = it },
                        imageTool = NoteBarTool.Image in barFit.tools,
                    )
                }
            }
        }
        if (barHidden && autoHide == AutoHide.Always) {
            NoteBarHandle(position, shown = true, onSummon = visibility::summon, Modifier.align(position.alignment()))
        }
        val editor = ui.view[0]?.layers
        val layers = ui.pageLayers
        if (ui.layersOpen && editor != null && layers != null) {
            val placement = layersPlacement(windowWidth.value)
            LayerPanel(
                editor = editor,
                state = layers,
                placement = placement,
                onClose = { ui.layersOpen = false },
                modifier = barPanelPlacement(placement, ui.layersAnchor, geometry),
            )
        }
        if (ui.shapeOptionsOpen && !ui.readMode) ShapeOptionsLayer(ui, toolbar, windowWidth, geometry, barFit.tools)
        if (ui.textOptionsOpen && !ui.readMode) TextOptionsLayer(ui, toolbar, windowWidth, geometry, barFit.tools)
    }

    if (ui.showPaperColor) {
        PaperColorDialog(
            initial = info?.background?.color ?: PaperColors.WHITE.argb,
            onConfirm = { argb ->
                ui.showPaperColor = false
                session.setPaperColor(argb)
                ui.view[0]?.reload()
            },
            onDismiss = { ui.showPaperColor = false },
        )
    }
}
