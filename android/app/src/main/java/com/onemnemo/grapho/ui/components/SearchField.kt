package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The one search field: a pill with a search icon, the text, and a clear button once there is text. It filters
 * as you type, so [onSearch] only marks the moment the person is done, such as the keyboard's search key.
 */
@Composable
fun GraphoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    onSearch: (() -> Unit)? = null,
) {
    val c = Grapho.colors
    val source = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val hovered by source.collectIsHoveredAsState()
    val fill = if (focused || hovered) c.surfaceControlHover else c.surfaceControl
    val edge = if (focused) c.borderControlHover else c.borderControl
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Grapho.metrics.sizeControlDefault)
            .hoverable(source)
            .clip(GraphoShapes.pill)
            .background(fill)
            .border(GraphoSpace.borderWidthHairline, edge, GraphoShapes.pill)
            .padding(start = GraphoSpace.spaceLg, end = GraphoSpace.spaceXs),
        horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphoIcon(R.drawable.ic_search, null, if (focused) c.textDefault else c.textMuted)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = Grapho.type.body, color = c.textSubtle, maxLines = 1)
            PlatformTextInput(
                value = value,
                onValueChange = onValueChange,
                textStyle = Grapho.type.body.copy(color = c.textDefault),
                cursorColor = c.textDefault,
                modifier = Modifier.fillMaxWidth().heightIn(min = Grapho.metrics.sizeControlDefault),
                imeAction = ImeAction.Search,
                autoFocus = autoFocus,
                description = placeholder,
                onImeAction = onSearch,
                onFocusChange = { focused = it },
            )
        }
        if (value.isNotEmpty()) {
            GraphoIconButton(
                R.drawable.ic_close,
                stringResource(R.string.library_search_clear),
                onClick = { onValueChange("") },
                size = Grapho.metrics.sizeControlDefault,
            )
        }
    }
}
