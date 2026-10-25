package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.PageLayers
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.ColorSwatch
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The quiet bar above a lasso selection (below it when there is no room). Every action weighs the same: icons,
 * with words only for the three picture actions, and Delete set apart by space, not a rule. Delete is one tap
 * because Undo brings it back. Colour swaps the bar for the person's palette, and is left out when only images are
 * selected ([canColour]). Crop and the two picture clean-ups are there when exactly one image is selected
 * ([canCrop]), and More adds Cut, Order, moving to a layer, and Flip. Thickness, shown with Colour, swaps the bar
 * for a slider that starts at [thickness], and [onThickness] gets the width when it is released. [bounds] is
 * the selection in view pixels, which are this composable's pixels because the canvas fills the same box.
 */
@Composable
internal fun SelectionMenuCard(
    bounds: RectF,
    insets: EdgeInsets,
    canColour: Boolean,
    canCrop: Boolean,
    palette: List<Int>,
    recent: List<Int>,
    customStart: Int,
    onColour: (Int) -> Unit,
    onCustomColour: (Int) -> Unit,
    onCrop: () -> Unit,
    onRemoveBackground: () -> Unit,
    onConvertToInk: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    layers: PageLayers?,
    thickness: Float,
    onThickness: (Float) -> Unit,
    more: SelectionMoreActions,
    onDelete: () -> Unit,
) {
    var colouring by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var thickening by remember { mutableStateOf(false) }
    if (picking) {
        LassoColourPicker(
            initial = customStart,
            recent = recent,
            onApply = {
                picking = false
                colouring = false
                onCustomColour(it)
            },
            onDismiss = { picking = false },
        )
    }
    CardSurface(bounds, insets) {
        if (colouring) {
            ColourPage(
                palette = palette,
                onBack = { colouring = false },
                onCustom = { picking = true },
                onPick = {
                    colouring = false
                    onColour(it)
                },
            )
        } else if (thickening) {
            ThicknessPage(current = thickness, onBack = { thickening = false }, onApply = onThickness)
        } else {
            if (canColour) {
                BarAction(R.drawable.ic_palette, stringResource(R.string.lasso_colour), { colouring = true })
                BarAction(R.drawable.ic_sliders, stringResource(R.string.lasso_thickness), { thickening = true })
            }
            if (canCrop) {
                BarWords(R.drawable.ic_crop, stringResource(R.string.lasso_crop), onCrop)
                BarWords(R.drawable.ic_cutout, stringResource(R.string.lasso_remove_background), onRemoveBackground)
                BarWords(R.drawable.ic_to_ink, stringResource(R.string.lasso_convert_to_ink), onConvertToInk)
            }
            BarAction(R.drawable.ic_copy, stringResource(R.string.lasso_copy), onCopy)
            BarAction(R.drawable.ic_duplicate, stringResource(R.string.lasso_duplicate), onDuplicate)
            SelectionMoreMenu(canReplace = canCrop, layers = layers, actions = more)
            Spacer(Modifier.width(GraphoSpace.spaceMd))
            BarAction(R.drawable.ic_trash, stringResource(R.string.lasso_delete), onDelete, danger = true)
        }
    }
}

@Composable
private fun ColourPage(palette: List<Int>, onBack: () -> Unit, onCustom: () -> Unit, onPick: (Int) -> Unit) {
    val c = Grapho.colors
    GraphoIconButton(R.drawable.ic_chevron_left, stringResource(R.string.action_back), onBack)
    // First, so a long palette never scrolls it out of reach.
    CustomColourSwatch(onCustom)
    Row(Modifier.widthIn(max = PALETTE_MAX_WIDTH).horizontalScroll(rememberScrollState())) {
        val label = stringResource(R.string.lasso_colour)
        for (argb in palette) {
            ColorSwatch(
                color = Color(argb),
                selected = false,
                ringColor = c.selectionIndicator,
                label = label,
                onClick = { onPick(argb) },
            )
        }
    }
}

private val PALETTE_MAX_WIDTH = 320.dp

@Composable
private fun BarAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit, danger: Boolean = false) {
    val c = Grapho.colors
    GraphoIconButton(icon, label, onClick, tint = if (danger) c.stateDangerFg else c.actionQuietFg)
}

@Composable
private fun BarWords(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    GraphoButton(label, onClick, kind = ButtonKind.Quiet, size = ButtonSize.Small, iconRes = icon)
}
