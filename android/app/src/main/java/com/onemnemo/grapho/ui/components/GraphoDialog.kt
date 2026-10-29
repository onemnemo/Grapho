package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

private val DIALOG_ELEVATION = 24.dp

/**
 * A short decision that must be answered: a title, a sentence or one field, and buttons. Anything with a
 * list, a grid or steps is a [GraphoSheet]. Put the main action last in [actions], as a [ButtonSize.Large]
 * button. Back and a tap outside both mean Cancel, so [onDismiss] must leave things as they were.
 */
@Composable
fun GraphoDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = Grapho.colors
    ModalLayer(onDismiss, dismissOnScrim = true) { windowWidth, windowHeight ->
        val width = centredSheetWidthDp(windowWidth.value, GraphoSpace.sizeDialogWidth.value, GraphoSpace.spaceLg.value)
        Surface(
            modifier = modifier
                .align(Alignment.Center)
                .width(width.dp)
                .heightIn(max = windowHeight - GraphoSpace.spaceLg * 2)
                .pointerInput(Unit) { detectTapGestures { } },
            shape = GraphoShapes.container,
            color = c.surfaceOverlay,
            shadowElevation = DIALOG_ELEVATION,
        ) {
            Column(
                modifier = Modifier.padding(GraphoSpace.space2xl),
                verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceLg),
            ) {
                Text(title, style = Grapho.type.title, color = c.textDefault)
                if (content != null) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
                        content = content,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
