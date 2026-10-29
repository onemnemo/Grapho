package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.PageBackground
import com.onemnemo.grapho.ui.NewNotebookSpec
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSheet
import com.onemnemo.grapho.ui.components.GraphoTabs
import com.onemnemo.grapho.ui.components.GraphoTextField
import com.onemnemo.grapho.ui.components.PaperColorDialog
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.theme.CanvasPalette
import com.onemnemo.grapho.ui.theme.CoverPreset
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Everything the canvas sheet edits: a background colour, a pattern and how far apart its marks are. */
data class CanvasDraft(
    val name: String,
    val background: Int,
    val pattern: Int,
    val spacingMm: Float,
    /** The folder a new canvas goes in, or null for the top level. */
    val folderId: String? = null,
) {
    fun toSpec(): NewNotebookSpec = NewNotebookSpec(
        title = name,
        infinite = true,
        pattern = pattern,
        paper = 0,
        landscape = false,
        coverColor = CoverPreset.Plain.base.toArgb(),
        paperColor = background,
        spacingMm = spacingMm,
    )

    fun paper(): PageBackground = PageBackground(pattern, spacingMm, background)

    companion object {
        /** A new canvas starts on the Dot grid preset. */
        fun fresh(folderId: String? = null) = CanvasDraft(
            name = "",
            background = CanvasPalette.presets[1].background.toArgb(),
            pattern = CanvasPalette.presets[1].pattern,
            spacingMm = SPACING_TIERS[1],
            folderId = folderId,
        )

        fun of(name: String, paper: PageBackground) =
            CanvasDraft(name, paper.color, paper.pattern, nearestSpacing(paper.spacingMm))
    }
}

private enum class CanvasTab { Presets, Background, Pattern }

private const val PRESET_COLUMNS = 4
private val PreviewWidth = 132.dp
private val SpacingNames =
    listOf(R.string.canvas_spacing_close, R.string.canvas_spacing_medium, R.string.canvas_spacing_wide)

/**
 * New canvas and Edit canvas: one sheet. A canvas is a background and a pattern, drawn live as you choose. New
 * also picks the Location when there are folders to choose from in [targets].
 */
@Composable
fun CanvasSheet(
    mode: SheetMode,
    initial: CanvasDraft,
    onSave: (CanvasDraft) -> Unit,
    onDismiss: () -> Unit,
    targets: List<MoveTarget> = emptyList(),
) {
    val untitled = stringResource(R.string.canvas_untitled)
    val startName = initial.name.ifBlank { untitled }
    var draft by remember { mutableStateOf(initial) }
    var name by remember { mutableStateOf(TextFieldValue(startName, TextRange(0, startName.length))) }
    var tab by remember { mutableStateOf(CanvasTab.Presets) }
    var customColour by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val changed = draft != initial || name.text != startName
    val save = { onSave(draft.copy(name = name.text.trim().ifEmpty { untitled })) }

    if (customColour) {
        PaperColorDialog(
            initial = draft.background,
            onConfirm = {
                draft = draft.copy(background = it)
                customColour = false
            },
            onDismiss = { customColour = false },
        )
    }

    val isNew = mode == SheetMode.New
    GraphoSheet(
        title = stringResource(if (isNew) R.string.library_new_canvas else R.string.library_edit_canvas),
        onDismiss = onDismiss,
        dismissOnScrim = !changed,
        footer = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(if (isNew) R.string.library_create_canvas else R.string.library_save),
                onClick = save,
                size = ButtonSize.Large,
            )
        },
    ) {
        SheetTop(
            preview = {
                Box(
                    Modifier
                        .width(PreviewWidth)
                        .aspectRatio(COVER_RATIO)
                        .shadow(8.dp, GraphoShapes.surface)
                        .clip(GraphoShapes.surface),
                ) { CanvasArt(draft.paper()) }
            },
            fields = {
                GraphoTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.new_title),
                    focusRequester = focus,
                    onDone = save,
                )
            },
        )
        if (isNew && targets.size > 1) {
            LocationRow(targets, draft.folderId) { draft = draft.copy(folderId = it) }
        }
        GraphoTabs(
            tabs = listOf(
                CanvasTab.Presets to stringResource(R.string.library_tab_presets),
                CanvasTab.Background to stringResource(R.string.new_background),
                CanvasTab.Pattern to stringResource(R.string.canvas_tab_pattern),
            ),
            selected = tab,
            onSelect = { tab = it },
        )
        when (tab) {
            CanvasTab.Presets -> CanvasPresets(draft) { draft = it }

            CanvasTab.Background -> BackgroundPane(draft.background, { draft = draft.copy(background = it) }) {
                customColour = true
            }

            CanvasTab.Pattern -> PatternPane(draft) { draft = it }
        }
    }
}

@Composable
private fun CanvasPresets(draft: CanvasDraft, onChange: (CanvasDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
        for (row in CanvasPalette.presets.withIndex().chunked(PRESET_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
                for ((index, preset) in row) {
                    val bg = preset.background.toArgb()
                    PresetTile(
                        label = stringResource(canvasPresetName(index)),
                        selected = bg == draft.background && preset.pattern == draft.pattern,
                        onClick = { onChange(draft.copy(background = bg, pattern = preset.pattern)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        val thumb = RoundedCornerShape(GraphoSpace.radiusKey)
                        Box(Modifier.width(56.dp).aspectRatio(COVER_RATIO).clip(thumb)) {
                            CanvasArt(PageBackground(preset.pattern, SPACING_TIERS[1], bg), sketch = false)
                        }
                    }
                }
                repeat(PRESET_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BackgroundPane(background: Int, onPick: (Int) -> Unit, onCustom: () -> Unit) {
    val colours = CanvasPalette.lightSwatches + CanvasPalette.darkSwatches
    val onList = colours.any { it.toArgb() == background }
    SwatchGrid(
        colors = colours,
        isSelected = { it.toArgb() == background },
        onPick = { onPick(it.toArgb()) },
        custom = if (onList) null else Color(background),
        onCustom = onCustom,
    )
}

@Composable
private fun PatternPane(draft: CanvasDraft, onChange: (CanvasDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg)) {
        PatternRow(draft.pattern, draft.background) { onChange(draft.copy(pattern = it)) }
        FieldLabel(stringResource(R.string.canvas_spacing))
        SegmentedControl(
            SPACING_TIERS.mapIndexed { i, mm -> mm to stringResource(SpacingNames[i]) },
            draft.spacingMm,
            { onChange(draft.copy(spacingMm = it)) },
            disabled = if (draft.pattern == 0) SPACING_TIERS.toSet() else emptySet(),
        )
    }
}
