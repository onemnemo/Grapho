package com.onemnemo.grapho.ui.note

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.pressable
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoWheel

/** A quiet icon button for the customiser's rows: a 36dp touch target around a 20dp icon, no background. */
@Composable
internal fun IconAction(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .pressable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        GraphoIcon(icon, null, GraphoWheel.textMuted, size = 20.dp)
    }
}

/**
 * The question that replaces an item's buttons before it is deleted, such as "Delete Pencil?", with a
 * Cancel and a red Delete. [modifier] gives the row its height and background.
 */
@Composable
internal fun ConfirmRow(message: String, modifier: Modifier, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "$message ${stringResource(R.string.delete_permanent_short)}",
            style = Grapho.type.label,
            color = GraphoWheel.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ConfirmPill(
            stringResource(R.string.action_cancel),
            GraphoWheel.selectedFill,
            GraphoWheel.text,
            stringResource(R.string.wheel_cancel_delete),
            onCancel,
        )
        ConfirmPill(
            stringResource(R.string.action_delete),
            Grapho.colors.stateDangerSolid,
            Grapho.colors.stateDangerOnSolid,
            stringResource(R.string.wheel_confirm_delete),
            onConfirm,
        )
    }
}

@Composable
private fun ConfirmPill(text: String, fill: Color, ink: Color, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .pressable(scaleOnPress = false, onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Grapho.type.caption, color = ink, maxLines = 1)
    }
}
