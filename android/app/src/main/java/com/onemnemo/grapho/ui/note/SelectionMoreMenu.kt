package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.PageLayers
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.components.GraphoMenu
import com.onemnemo.grapho.ui.components.GraphoMenuDivider
import com.onemnemo.grapho.ui.components.GraphoMenuItem

/** What the selection's More menu can do. */
internal class SelectionMoreActions(
    val onCut: () -> Unit,
    val onReplaceImage: () -> Unit,
    val onBringToFront: () -> Unit,
    val onSendToBack: () -> Unit,
    val onMoveToLayer: (Int) -> Unit,
    val onMoveToNewLayer: () -> Unit,
    val onFlipHorizontally: () -> Unit,
    val onFlipVertically: () -> Unit,
    val onCopyAsImage: () -> Unit,
    val onExportSelection: () -> Unit,
)

/** Which level of the menu shows. */
private enum class MorePage { Main, Order, Layers }

/**
 * The More button of the selection bar and the menu under it: Cut, Replace image (only for one picture), then
 * Order and Move to layer, which open a second level in the same menu, Flip, and Copy as image and Export
 * selection. [layers] are the layers of the page; hidden and locked ones are left out because nothing can be
 * put on them. Each final choice closes the menu first.
 */
@Composable
internal fun SelectionMoreMenu(canReplace: Boolean, layers: PageLayers?, actions: SelectionMoreActions) {
    var open by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(MorePage.Main) }
    fun choose(action: () -> Unit) {
        open = false
        page = MorePage.Main
        action()
    }

    Box {
        GraphoIconButton(R.drawable.ic_more_horizontal, stringResource(R.string.lasso_more), onClick = { open = true })
        GraphoMenu(
            expanded = open,
            onDismiss = {
                open = false
                page = MorePage.Main
            },
        ) {
            when (page) {
                MorePage.Main -> MainPage(canReplace, actions, ::choose) { page = it }

                MorePage.Order -> {
                    GraphoMenuItem(
                        stringResource(R.string.action_back),
                        icon = R.drawable.ic_chevron_left,
                        onClick = { page = MorePage.Main },
                    )
                    GraphoMenuDivider()
                    GraphoMenuItem(
                        stringResource(R.string.lasso_bring_to_front),
                        icon = R.drawable.ic_bring_front,
                        onClick = { choose(actions.onBringToFront) },
                    )
                    GraphoMenuItem(
                        stringResource(R.string.lasso_send_to_back),
                        icon = R.drawable.ic_send_back,
                        onClick = { choose(actions.onSendToBack) },
                    )
                }

                MorePage.Layers -> LayerChoices(layers, actions, ::choose) { page = MorePage.Main }
            }
        }
    }
}

@Composable
private fun MainPage(
    canReplace: Boolean,
    actions: SelectionMoreActions,
    choose: (() -> Unit) -> Unit,
    show: (MorePage) -> Unit,
) {
    GraphoMenuItem(
        stringResource(R.string.lasso_cut),
        icon = R.drawable.ic_cut,
        onClick = { choose(actions.onCut) },
    )
    if (canReplace) {
        GraphoMenuItem(
            stringResource(R.string.lasso_replace_image),
            icon = R.drawable.ic_replace,
            onClick = { choose(actions.onReplaceImage) },
        )
    }
    GraphoMenuDivider()
    GraphoMenuItem(
        stringResource(R.string.lasso_order),
        icon = R.drawable.ic_bring_front,
        onClick = { show(MorePage.Order) },
    )
    GraphoMenuItem(
        stringResource(R.string.lasso_move_to_layer),
        icon = R.drawable.ic_layers,
        onClick = { show(MorePage.Layers) },
    )
    GraphoMenuItem(
        stringResource(R.string.lasso_flip_horizontally),
        icon = R.drawable.ic_flip_h,
        onClick = { choose(actions.onFlipHorizontally) },
    )
    GraphoMenuItem(
        stringResource(R.string.lasso_flip_vertically),
        icon = R.drawable.ic_flip_v,
        onClick = { choose(actions.onFlipVertically) },
    )
    GraphoMenuDivider()
    GraphoMenuItem(
        stringResource(R.string.lasso_copy_as_image),
        icon = R.drawable.ic_image,
        onClick = { choose(actions.onCopyAsImage) },
    )
    GraphoMenuItem(
        stringResource(R.string.lasso_export_selection),
        icon = R.drawable.ic_share,
        onClick = { choose(actions.onExportSelection) },
    )
}

@Composable
private fun LayerChoices(
    layers: PageLayers?,
    actions: SelectionMoreActions,
    choose: (() -> Unit) -> Unit,
    back: () -> Unit,
) {
    GraphoMenuItem(stringResource(R.string.action_back), icon = R.drawable.ic_chevron_left, onClick = back)
    GraphoMenuDivider()
    if (layers != null) {
        // Top first, like the layer panel; the layer the selection is on is no target.
        for (index in layers.layers.indices.reversed()) {
            val layer = layers.layers[index]
            if (index == layers.active || layer.locked || !layer.visible) continue
            GraphoMenuItem(layer.name, onClick = { choose { actions.onMoveToLayer(index) } })
        }
    }
    GraphoMenuItem(
        stringResource(R.string.lasso_move_to_new_layer),
        icon = R.drawable.ic_plus,
        onClick = { choose(actions.onMoveToNewLayer) },
    )
}
