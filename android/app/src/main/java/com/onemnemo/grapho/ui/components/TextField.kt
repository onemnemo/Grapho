package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import com.onemnemo.grapho.ui.theme.GraphoTouch

enum class FieldSize { Small, Default }

/**
 * The one text field, in its stacked layout (see [GraphoInlineTextField] for the inline one). There is no
 * floating label. An optional [label] sits above it, [hint] under it, and an [error] replaces the hint
 * and turns the outline red. A [readOnly] field can be focused and copied but not edited, a disabled one cannot.
 * [trailing] holds one small control such as a clear button. Pass [focusRequester] to focus the field on open.
 */
@Composable
fun GraphoTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    size: FieldSize = FieldSize.Default,
    @DrawableRes leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onDone: (() -> Unit)? = null,
) {
    val c = Grapho.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    // Small is never used at touch density, where it resolves to the default height.
    val small = size == FieldSize.Small && Grapho.metrics != GraphoTouch
    val height = if (small) Grapho.metrics.sizeControlSmall else Grapho.metrics.sizeControlDefault
    val textColor = when {
        !enabled -> c.stateDisabledFg
        readOnly -> c.stateReadonlyFg
        else -> c.textDefault
    }
    val fill = when {
        !enabled -> c.stateDisabledBg
        readOnly -> c.stateReadonlyBg
        hovered && !focused -> c.surfaceControlHover
        else -> c.surfaceControl
    }
    val edge = when {
        !enabled -> c.stateDisabledBorder
        error != null -> c.borderError
        focused -> c.borderFocus
        readOnly -> c.stateReadonlyBorder
        hovered -> c.borderControlHover
        else -> c.borderControl
    }
    val emphasised = enabled && (focused || error != null)
    val borderWidth = if (emphasised) GraphoSpace.borderWidthEmphasis else GraphoSpace.borderWidthHairline
    val help = error ?: hint
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceXs)) {
        if (label != null) {
            Text(label, style = Grapho.type.caption, color = if (enabled) c.textMuted else c.textDisabled)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .semantics {
                    if (label != null) contentDescription = label
                    if (error != null) this.error(error)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            textStyle = Grapho.type.body.copy(color = textColor),
            cursorBrush = SolidColor(c.textDefault),
            interactionSource = source,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = height)
                        .hoverable(source, enabled)
                        .clip(GraphoShapes.surface)
                        .background(fill)
                        .border(
                            width = borderWidth,
                            color = edge,
                            shape = GraphoShapes.surface,
                        )
                        .padding(horizontal = GraphoSpace.spaceMd),
                    horizontalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) GraphoIcon(leadingIcon, null, c.textMuted)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.text.isEmpty() && placeholder != null) {
                            Text(
                                placeholder,
                                style = Grapho.type.body,
                                color = if (enabled) c.textSubtle else c.stateDisabledFg,
                            )
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        if (help != null) {
            Text(
                help,
                style = Grapho.type.caption,
                color = if (error != null) c.stateDangerFg else c.textSubtle,
                modifier = Modifier.padding(horizontal = GraphoSpace.spaceXs),
            )
        }
    }
}
