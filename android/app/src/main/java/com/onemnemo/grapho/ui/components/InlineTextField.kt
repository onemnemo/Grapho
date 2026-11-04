package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import com.onemnemo.grapho.ui.theme.GraphoTouch

/**
 * Whether an inline field is being edited. The field is plain text until [start] is called (by a tap on it,
 * or by a Rename command elsewhere), so it can never hold focus or a selection while nobody is renaming.
 */
@Stable
class InlineEditState(editing: Boolean = false) {
    var editing by mutableStateOf(editing)
        private set
    internal var selectAll = true
        private set

    /** Starts editing. A Rename command selects the whole name; a tap puts the caret at the end. */
    fun start(selectAll: Boolean = true) {
        this.selectAll = selectAll
        editing = true
    }

    internal fun stop() {
        editing = false
    }
}

@Composable
fun rememberInlineEditState(editing: Boolean = false): InlineEditState = remember { InlineEditState(editing) }

/**
 * The inline layout of the text field, for renaming in place and for a document title. It is a line of text
 * until it is tapped or [state] is started. Enter (or leaving the field) commits through [onCommit], and Escape
 * puts the original [value] back. An empty name is never committed. At touch density a resting bottom edge
 * shows that the text can be edited. For a labelled field in a form, use [GraphoTextField].
 */
@Composable
fun GraphoInlineTextField(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    textStyle: TextStyle = Grapho.type.label,
    size: FieldSize = FieldSize.Default,
    enabled: Boolean = true,
    state: InlineEditState = rememberInlineEditState(),
) {
    val c = Grapho.colors
    val touch = Grapho.metrics == GraphoTouch
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val editing = enabled && state.editing

    // Small resolves to the default height at touch density.
    val height = when {
        touch -> Grapho.metrics.sizeHitMin
        size == FieldSize.Small -> Grapho.metrics.sizeControlSmall
        else -> Grapho.metrics.sizeControlDefault
    }
    val fill = when {
        !enabled -> Color.Transparent
        editing -> c.surfaceControl
        hovered -> c.surfaceControlHover
        else -> Color.Transparent
    }
    val edge = when {
        !enabled -> Modifier

        editing -> Modifier.border(GraphoSpace.borderWidthEmphasis, c.borderFocus, GraphoShapes.item)

        hovered -> Modifier.border(GraphoSpace.borderWidthHairline, c.borderControlHover, GraphoShapes.item)

        touch -> Modifier.drawBehind {
            val width = GraphoSpace.borderWidthHairline.toPx()
            val y = this.size.height - width / 2f
            drawLine(c.borderControl, Offset(0f, y), Offset(this.size.width, y), width)
        }

        else -> Modifier
    }
    val frame: @Composable (@Composable () -> Unit) -> Unit = { inner ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = height)
                .hoverable(source, enabled)
                .clip(GraphoShapes.item)
                .background(fill)
                .then(edge)
                .padding(horizontal = GraphoSpace.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { inner() }
        }
    }
    val color = if (enabled) c.textDefault else c.stateDisabledFg
    if (!editing) {
        Box(
            modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                ) { state.start(selectAll = false) },
        ) {
            frame {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        placeholder,
                        style = textStyle,
                        color = c.textSubtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(value, style = textStyle, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        return
    }
    InlineEditor(value, onCommit, modifier, placeholder, textStyle, color, state, height, frame)
}

@Composable
private fun InlineEditor(
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    textStyle: TextStyle,
    color: Color,
    state: InlineEditState,
    height: Dp,
    frame: @Composable (@Composable () -> Unit) -> Unit,
) {
    val c = Grapho.colors
    val end = TextRange(value.length)
    var draft by remember {
        mutableStateOf(TextFieldValue(value, if (state.selectAll) TextRange(0, value.length) else end))
    }

    var finished by remember { mutableStateOf(false) }

    fun commit() {
        if (finished) return
        finished = true
        val next = draft.text.trim()
        if (next.isNotEmpty() && next != value) onCommit(next)
    }

    Box(modifier.fillMaxWidth()) {
        frame {
            if (draft.text.isEmpty() && placeholder != null) {
                Text(placeholder, style = textStyle, color = c.textSubtle)
            }
            PlatformTextInput(
                value = draft,
                onValueChange = { draft = it },
                textStyle = textStyle.copy(color = color),
                cursorColor = c.textDefault,
                modifier = Modifier.fillMaxWidth().heightIn(min = height),
                autoFocus = true,
                blurOnAction = true,
                onFocusChange = { focused ->
                    if (!focused) {
                        commit()
                        state.stop()
                    }
                },
                onEscape = { draft = TextFieldValue(value) },
            )
        }
    }
}
