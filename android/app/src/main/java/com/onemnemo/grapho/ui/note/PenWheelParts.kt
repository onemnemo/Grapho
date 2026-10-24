package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.GraphoWheel

/** A round button with a 44dp touch target centred at ([cx], [cy]) dp in the window. */
@Composable
internal fun WheelButton(
    cx: Float,
    cy: Float,
    label: String,
    onClick: () -> Unit,
    size: Float = 44f,
    active: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(x = (cx - size / 2f).dp, y = (cy - size / 2f).dp)
            .size(size.dp)
            .clip(CircleShape)
            .background(if (active) GraphoWheel.plate else Color.Transparent)
            .pressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
        content = content,
    )
}
