package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.ui.components.InlineEditState

/** What the note menus and the insert menu can do. The screen supplies each one. */
internal class NoteActions(
    val onRename: () -> Unit,
    val onPaper: (Int) -> Unit,
    val onPaperColor: (Int) -> Unit,
    val onCustomPaperColor: () -> Unit,
    val onCustomiseWheel: () -> Unit,
    val onAddBlankPage: () -> Unit,
    val onInsertPdf: () -> Unit,
    val onInsertPhoto: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onPasteImage: () -> Unit,
    val canPasteImage: () -> Boolean,
    val onExport: () -> Unit,
    val onShare: () -> Unit,
    val onReadMode: () -> Unit,
    val onSettings: () -> Unit,
)

/** The note's switches, which sit in the More menu: whether a finger draws, the two guides and full screen. */
internal class NoteToggles(
    val fingerDraws: Boolean,
    val rulerOn: Boolean,
    val protractorOn: Boolean,
    val fullScreen: Boolean,
    val onFingerDraws: (Boolean) -> Unit,
    val onRuler: (Boolean) -> Unit,
    val onProtractor: (Boolean) -> Unit,
    val onFullScreen: (Boolean) -> Unit,
)

/** Wires the note's menus to the session, the canvas and the insert and export flows. */
internal fun noteActions(
    session: NoteSession,
    ui: NoteUiState,
    titleEdit: InlineEditState,
    pdf: PdfInserter,
    images: ImageInserter,
    exporter: NoteExporter,
    sharer: NoteSharer,
    onSettings: () -> Unit,
    edited: () -> Unit,
) = NoteActions(
    onRename = { titleEdit.start() },
    onPaper = { pattern ->
        session.setPaper(pattern)
        ui.view[0]?.reload()
    },
    onPaperColor = { argb ->
        session.setPaperColor(argb)
        ui.view[0]?.reload()
    },
    onCustomPaperColor = { ui.showPaperColor = true },
    onCustomiseWheel = {
        ui.wheelOpen = true
        ui.wheelCustomise = CustomiseRequest()
    },
    onAddBlankPage = {
        if (ui.view[0]?.insertBlankPage() == true) edited()
    },
    onInsertPdf = pdf::pick,
    onInsertPhoto = images::pickPhoto,
    onTakePhoto = images::takePhoto,
    onPasteImage = images::pasteImage,
    canPasteImage = images::canPaste,
    onExport = exporter::open,
    onShare = sharer::share,
    onReadMode = ui::enterRead,
    onSettings = onSettings,
)
