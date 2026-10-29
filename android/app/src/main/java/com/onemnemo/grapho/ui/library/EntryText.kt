package com.onemnemo.grapho.ui.library

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.NotebookEntry
import com.onemnemo.grapho.ui.theme.Grapho

/** What the library calls a note that has no title. */
@Composable
fun NotebookEntry.displayName(): String = title.ifBlank { stringResource(R.string.note_untitled) }

fun NotebookEntry.nameIn(context: Context): String = title.ifBlank { context.getString(R.string.note_untitled) }

@Composable
fun kindLabel(kind: EntryKind): String = stringResource(
    when (kind) {
        EntryKind.Notebook -> R.string.library_kind_notebook
        EntryKind.Pdf -> R.string.library_kind_pdf
        EntryKind.Canvas -> R.string.library_kind_canvas
    },
)

/** "12 pages", or "Infinite" for a canvas, which has one page without edges. */
@Composable
fun sizeLabel(entry: NotebookEntry): String = if (entry.infinite) {
    stringResource(R.string.library_size_infinite)
} else {
    LocalContext.current.resources.getQuantityString(R.plurals.library_pages, entry.pages, entry.pages)
}

/** The quiet line under a tile: "Notebook, 12 pages", or just "Canvas". */
@Composable
fun metaLabel(entry: NotebookEntry): String {
    val kind = kindLabel(entry.kind)
    return if (entry.infinite) kind else stringResource(R.string.library_meta, kind, sizeLabel(entry))
}

@Composable
fun agoLabel(ago: Ago): String = when (ago) {
    Ago.JustNow -> stringResource(R.string.library_when_now)
    is Ago.Minutes -> stringResource(R.string.library_when_minutes, ago.n)
    is Ago.Hours -> stringResource(R.string.library_when_hours, ago.n)
    Ago.Yesterday -> stringResource(R.string.library_when_yesterday)
    is Ago.Days -> stringResource(R.string.library_when_days, ago.n)
    is Ago.On -> ago.date
}

/** [text] with the first match of [query] on the match colour, for search results. */
@Composable
fun highlighted(text: String, query: String): AnnotatedString {
    val range = matchRange(text, query) ?: return AnnotatedString(text)
    val match = SpanStyle(background = Grapho.colors.highlightMatch)
    return buildAnnotatedString {
        append(text.substring(0, range.first))
        withStyle(match) { append(text.substring(range.first, range.last + 1)) }
        append(text.substring(range.last + 1))
    }
}
