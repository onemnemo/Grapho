package com.onemnemo.grapho.ui.note

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ExportPlan
import com.onemnemo.grapho.note.PageChoice
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoSheet
import com.onemnemo.grapho.ui.components.GraphoSwitchRow
import com.onemnemo.grapho.ui.components.GraphoTextField
import com.onemnemo.grapho.ui.components.SegmentedControl
import com.onemnemo.grapho.ui.theme.Grapho

enum class ExportFormat { Notebook, Pdf, Images }

/** What the person chose: the format and the zero-based [pages] (a single page for an infinite canvas). */
class ExportChoice(val format: ExportFormat, val pages: List<Int>, val backgrounds: Boolean)

private enum class PageMode { All, Current, Range }

/** The export sheet: format, pages and, for PDF and images, whether pages keep their backgrounds. */
@Composable
fun ExportSheet(
    pageCount: Int,
    currentPage: Int,
    infinite: Boolean,
    onConfirm: (ExportChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Grapho.colors
    var format by remember { mutableStateOf(ExportFormat.Pdf) }
    var mode by remember { mutableStateOf(PageMode.All) }
    var range by remember { mutableStateOf(TextFieldValue("")) }
    var backgrounds by remember { mutableStateOf(true) }

    val isNotebook = format == ExportFormat.Notebook
    val choice = when (mode) {
        PageMode.All -> PageChoice.All
        PageMode.Current -> PageChoice.Current
        PageMode.Range -> PageChoice.Range(range.text)
    }
    val parsed = ExportPlan.pages(choice, pageCount, currentPage)
    val pages = when {
        isNotebook -> emptyList()
        infinite -> listOf(0)
        else -> parsed
    }
    val rangeInvalid = !isNotebook && !infinite && mode == PageMode.Range && range.text.isNotBlank() && parsed == null

    GraphoSheet(
        title = stringResource(R.string.export_title),
        onDismiss = onDismiss,
        footer = {
            GraphoButton(stringResource(R.string.action_cancel), onClick = onDismiss, kind = ButtonKind.Quiet)
            GraphoButton(
                stringResource(R.string.export_confirm),
                onClick = { pages?.let { onConfirm(ExportChoice(format, it, backgrounds)) } },
                size = ButtonSize.Large,
                enabled = pages != null,
            )
        },
    ) {
        Heading(R.string.export_format)
        SegmentedControl(
            options = listOf(
                ExportFormat.Pdf to stringResource(R.string.export_format_pdf),
                ExportFormat.Images to stringResource(R.string.export_format_images),
                ExportFormat.Notebook to stringResource(R.string.export_format_notebook),
            ),
            selected = format,
            onSelect = { format = it },
        )
        Text(
            stringResource(
                when (format) {
                    ExportFormat.Pdf -> R.string.export_pdf_note
                    ExportFormat.Images -> R.string.export_images_note
                    ExportFormat.Notebook -> R.string.export_notebook_note
                },
            ),
            style = Grapho.type.caption,
            color = c.textMuted,
        )
        if (!isNotebook) {
            Heading(R.string.export_pages)
            if (infinite) {
                Text(
                    stringResource(R.string.export_infinite_note),
                    style = Grapho.type.body,
                    color = c.textDefault,
                )
            } else {
                SegmentedControl(
                    options = listOf(
                        PageMode.All to stringResource(R.string.export_pages_all),
                        PageMode.Current to stringResource(R.string.export_pages_current),
                        PageMode.Range to stringResource(R.string.export_pages_range),
                    ),
                    selected = mode,
                    onSelect = { mode = it },
                )
                if (mode == PageMode.Range) {
                    GraphoTextField(
                        value = range,
                        onValueChange = { range = it },
                        placeholder = stringResource(R.string.export_range_hint),
                        error = if (rangeInvalid) stringResource(R.string.export_range_error, pageCount) else null,
                    )
                }
            }
            GraphoSwitchRow(
                text = stringResource(R.string.export_backgrounds),
                checked = backgrounds,
                onCheckedChange = { backgrounds = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Heading(@StringRes text: Int) {
    Text(stringResource(text), style = Grapho.type.caption, color = Grapho.colors.textMuted)
}
