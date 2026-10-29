package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.PaperChrome
import com.onemnemo.grapho.ui.components.PaperColorDialog
import com.onemnemo.grapho.ui.components.PaperColorRow
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val Templates = listOf(
    0 to R.string.paper_blank,
    1 to R.string.paper_lined,
    2 to R.string.paper_grid,
    3 to R.string.paper_dotted,
)

/** Blank, lined, grid and dotted as small pages in the chosen paper colour. */
@Composable
fun PatternRow(pattern: Int, paperColor: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd)) {
        for ((kind, name) in Templates) {
            PaperThumbnail(stringResource(name), kind, paperColor, selected = pattern == kind, onClick = {
                onPick(kind)
            })
        }
    }
}

/** The paper colour presets and a custom one. The custom picker is a dialog over the sheet. */
@Composable
fun PaperColourField(paperColor: Int, onPick: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    if (custom) {
        PaperColorDialog(
            initial = paperColor,
            onConfirm = {
                onPick(it)
                custom = false
            },
            onDismiss = { custom = false },
        )
    }
    PaperColorRow(selected = paperColor, onPick = onPick, onCustom = { custom = true })
}

/** A small page showing the pattern, with its name below. */
@Composable
private fun PaperThumbnail(name: String, pattern: Int, paper: Int, selected: Boolean, onClick: () -> Unit) {
    val c = Grapho.colors
    Column(
        modifier = Modifier.pressable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 84.dp)
                .clip(GraphoShapes.item)
                .background(Color(paper))
                .border(
                    width = if (selected) GraphoSpace.borderWidthEmphasis else GraphoSpace.borderWidthHairline,
                    color = if (selected) c.selectionIndicator else c.borderSubtle,
                    shape = GraphoShapes.item,
                ),
        ) {
            Canvas(Modifier.size(width = 64.dp, height = 84.dp)) {
                drawPaperPattern(pattern, Color(PaperChrome.pattern(paper)), 9.dp.toPx(), 1.dp.toPx())
            }
        }
        Text(name, style = Grapho.type.caption, color = if (selected) c.textDefault else c.textMuted)
    }
}
