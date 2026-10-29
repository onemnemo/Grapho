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
import com.onemnemo.grapho.note.PaperColors
import com.onemnemo.grapho.ui.NewNotebookSpec
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSheet
import com.onemnemo.grapho.ui.components.GraphoTabs
import com.onemnemo.grapho.ui.components.GraphoTextField
import com.onemnemo.grapho.ui.components.PaperColorDialog
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.spacingFor
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.CoverPreset
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Everything the notebook sheet edits. [paper] is 0 A4, 1 A5, 2 Letter, and is fixed once the notebook exists. */
data class NotebookDraft(
    val name: String,
    val coverColor: Int,
    val pattern: Int,
    val paper: Int,
    val landscape: Boolean,
    val paperColor: Int,
    val spacingMm: Float,
    /** The folder a new notebook goes in, or null for the top level. */
    val folderId: String? = null,
) {
    fun toSpec(): NewNotebookSpec = NewNotebookSpec(
        title = name,
        infinite = false,
        pattern = pattern,
        paper = paper,
        landscape = landscape,
        coverColor = coverColor,
        paperColor = paperColor,
        spacingMm = spacingMm,
    )

    fun background(): PageBackground = PageBackground(pattern, spacingMm, paperColor)

    companion object {
        fun fresh(folderId: String? = null) = NotebookDraft(
            name = "",
            coverColor = CoverPreset.Calculus.base.toArgb(),
            pattern = 0,
            paper = 0,
            landscape = false,
            paperColor = PaperColors.WHITE.argb,
            spacingMm = spacingFor(0),
            folderId = folderId,
        )
    }
}

private enum class LookTab { Presets, Colour, Paper }

private val PageSizes = listOf(0 to R.string.new_size_a4, 1 to R.string.new_size_a5, 2 to R.string.new_size_letter)
private val Orientations = listOf(false to R.string.new_portrait, true to R.string.new_landscape)

private const val PRESET_COLUMNS = 4
private val PreviewWidth = 132.dp

/**
 * New notebook and Edit notebook: one sheet with a live preview. Both pick a cover from presets or a colour, and
 * the paper. New also picks the page size and, when there are folders to choose from in [targets], the
 * Location the notebook is made in.
 */
@Composable
fun NotebookSheet(
    mode: SheetMode,
    initial: NotebookDraft,
    onSave: (NotebookDraft) -> Unit,
    onDismiss: () -> Unit,
    targets: List<MoveTarget> = emptyList(),
) {
    val untitled = stringResource(R.string.note_untitled)
    val startName = initial.name.ifBlank { untitled }
    var draft by remember { mutableStateOf(initial) }
    var name by remember { mutableStateOf(TextFieldValue(startName, TextRange(0, startName.length))) }
    var tab by remember { mutableStateOf(LookTab.Presets) }
    var customCover by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val changed = draft != initial || name.text != startName
    val save = { onSave(draft.copy(name = name.text.trim().ifEmpty { untitled })) }

    if (customCover) {
        PaperColorDialog(
            initial = draft.coverColor,
            onConfirm = {
                draft = draft.copy(coverColor = CoverPalette.solid(Color(it)))
                customCover = false
            },
            onDismiss = { customCover = false },
        )
    }

    val isNew = mode == SheetMode.New
    GraphoSheet(
        title = stringResource(if (isNew) R.string.library_new_notebook else R.string.library_edit_notebook),
        onDismiss = onDismiss,
        dismissOnScrim = !changed,
        footer = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(if (isNew) R.string.library_create_notebook else R.string.library_save),
                onClick = save,
                size = ButtonSize.Large,
            )
        },
    ) {
        SheetTop(
            preview = {
                val shape = RoundedCornerShape(
                    topStart = GraphoSpace.radiusKey,
                    bottomStart = GraphoSpace.radiusKey,
                    topEnd = GraphoSpace.radiusItem,
                    bottomEnd = GraphoSpace.radiusItem,
                )
                Box(Modifier.width(PreviewWidth).aspectRatio(COVER_RATIO).shadow(8.dp, shape).clip(shape)) {
                    CoverArt(CoverPalette.resolve(draft.coverColor), name.text.trim().ifEmpty { untitled })
                }
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
                LookTab.Presets to stringResource(R.string.library_tab_presets),
                LookTab.Colour to stringResource(R.string.library_tab_colour),
                LookTab.Paper to stringResource(R.string.new_paper),
            ),
            selected = tab,
            onSelect = { tab = it },
        )
        when (tab) {
            LookTab.Presets -> PresetsPane(draft.coverColor) { draft = draft.copy(coverColor = it.base.toArgb()) }

            LookTab.Colour -> ColourPane(draft.coverColor, onPick = { draft = draft.copy(coverColor = it) }) {
                customCover = true
            }

            LookTab.Paper -> PaperPane(draft, isNew) { draft = it }
        }
    }
}

@Composable
private fun PresetsPane(coverColor: Int, onPick: (CoverPreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
        for (row in CoverPreset.entries.chunked(PRESET_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
                for (preset in row) {
                    PresetTile(
                        label = stringResource(presetName(preset)),
                        selected = preset.base.toArgb() == coverColor,
                        onClick = { onPick(preset) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(Modifier.width(48.dp).aspectRatio(COVER_RATIO).clip(CoverShape)) {
                            CoverArt(CoverPalette.resolve(preset.base.toArgb()), null)
                        }
                    }
                }
                repeat(PRESET_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ColourPane(coverColor: Int, onPick: (Int) -> Unit, onCustom: () -> Unit) {
    val onList = CoverPalette.swatches.any { CoverPalette.solid(it) == coverColor }
    SwatchGrid(
        colors = CoverPalette.swatches,
        isSelected = { CoverPalette.solid(it) == coverColor },
        onPick = { onPick(CoverPalette.solid(it)) },
        custom = if (onList) null else Color(coverColor),
        onCustom = onCustom,
    )
}

@Composable
private fun PaperPane(draft: NotebookDraft, isNew: Boolean, onChange: (NotebookDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg)) {
        FieldLabel(stringResource(R.string.new_paper))
        PatternRow(draft.pattern, draft.paperColor) { onChange(draft.copy(pattern = it, spacingMm = spacingFor(it))) }
        FieldLabel(stringResource(R.string.new_paper_colour))
        PaperColourField(draft.paperColor) { onChange(draft.copy(paperColor = it)) }
        if (isNew) {
            FieldLabel(stringResource(R.string.new_page_size))
            SegmentedControl(
                PageSizes.map { (value, label) -> value to stringResource(label) },
                draft.paper,
                { onChange(draft.copy(paper = it)) },
            )
            FieldLabel(stringResource(R.string.new_orientation))
            SegmentedControl(
                Orientations.map { (value, label) -> value to stringResource(label) },
                draft.landscape,
                { onChange(draft.copy(landscape = it)) },
            )
        }
    }
}
