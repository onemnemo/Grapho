package com.onemnemo.grapho.ui.library

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.theme.CoverPalette
import com.onemnemo.grapho.ui.theme.CoverPreset
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** The empty states of the library: nothing at all, an empty folder, no folders, nothing starred or deleted, no match. */
enum class EmptyKind { Library, Folder, Folders, Starred, Deleted, NoMatch }

private val CoverWidth = 84.dp
private const val FAN_DEGREES = 9f
private val FAN_SHIFT = 56.dp
private val TextWidth = 320.dp

/**
 * A picture made of real covers, a title, one calm sentence and at most two actions. The first button is the
 * solid one, because only one action on a surface is the main one.
 */
@Composable
fun LibraryEmpty(
    kind: EmptyKind,
    onPrimary: (() -> Unit)?,
    onSecondary: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val c = Grapho.colors
    Column(
        modifier.fillMaxWidth().padding(horizontal = GraphoSpace.space2xl, vertical = GraphoSpace.space3xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (kind == EmptyKind.NoMatch) SearchCover() else FannedCovers()
        Text(
            stringResource(titleOf(kind)),
            style = Grapho.type.title,
            color = c.textDefault,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = GraphoSpace.space2xl),
        )
        Text(
            stringResource(textOf(kind)),
            style = Grapho.type.body,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = GraphoSpace.spaceSm).widthIn(max = TextWidth),
        )
        Row(
            Modifier.padding(top = GraphoSpace.space2xl),
            horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        ) {
            if (onPrimary != null) {
                val label = if (kind == EmptyKind.Folders) {
                    R.string.library_new_folder
                } else {
                    R.string.library_create_notebook
                }
                GraphoButton(
                    stringResource(label),
                    onClick = onPrimary,
                    iconRes = R.drawable.ic_plus,
                )
            }
            if (onSecondary != null) {
                val label = if (kind == EmptyKind.NoMatch) {
                    R.string.library_search_clear
                } else {
                    R.string.library_import_pdf
                }
                GraphoButton(stringResource(label), onClick = onSecondary, kind = ButtonKind.Secondary)
            }
        }
    }
}

@StringRes
private fun titleOf(kind: EmptyKind): Int = when (kind) {
    EmptyKind.Library -> R.string.library_empty_title
    EmptyKind.Folder -> R.string.library_folder_empty_title
    EmptyKind.Folders -> R.string.library_folders_empty_title
    EmptyKind.Starred -> R.string.library_starred_empty_title
    EmptyKind.Deleted -> R.string.library_deleted_empty_title
    EmptyKind.NoMatch -> R.string.library_nomatch_title
}

@StringRes
private fun textOf(kind: EmptyKind): Int = when (kind) {
    EmptyKind.Library -> R.string.library_empty_text
    EmptyKind.Folder -> R.string.library_folder_empty_text
    EmptyKind.Folders -> R.string.library_folders_empty_text
    EmptyKind.Starred -> R.string.library_starred_empty_text
    EmptyKind.Deleted -> R.string.library_deleted_hint
    EmptyKind.NoMatch -> R.string.library_nomatch_text
}

@Composable
private fun FannedCovers() {
    Box(
        Modifier.size(width = CoverWidth + FAN_SHIFT * 2, height = CoverWidth * 4 / 3 + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fan = listOf(
            Triple(-FAN_DEGREES, -FAN_SHIFT, CoverPreset.Dusk),
            Triple(FAN_DEGREES, FAN_SHIFT, CoverPreset.Biology),
            Triple(0f, 0.dp, CoverPreset.Calculus),
        )
        for ((degrees, shift, preset) in fan) {
            Box(
                Modifier
                    .offset(x = shift)
                    .size(CoverWidth, CoverWidth * 4 / 3)
                    .graphicsLayer { rotationZ = degrees }
                    .shadow(8.dp, CoverShape)
                    .clip(CoverShape),
            ) { CoverArt(CoverPalette.resolve(preset.base.toArgb()), null) }
        }
    }
}

@Composable
private fun SearchCover() {
    val c = Grapho.colors
    Box(Modifier.size(CoverWidth + 32.dp, CoverWidth * 4 / 3 + 16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(CoverWidth, CoverWidth * 4 / 3)
                .graphicsLayer { rotationZ = -4f }
                .shadow(8.dp, CoverShape)
                .clip(CoverShape),
        ) { CoverArt(CoverPalette.resolve(CoverPreset.Plain.base.toArgb()), null) }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(Grapho.metrics.sizeControlLarge)
                .clip(CircleShape)
                .background(c.surfaceOverlay),
            contentAlignment = Alignment.Center,
        ) { GraphoIcon(R.drawable.ic_search, null, c.textMuted, size = Grapho.metrics.sizeIconLarge) }
    }
}
