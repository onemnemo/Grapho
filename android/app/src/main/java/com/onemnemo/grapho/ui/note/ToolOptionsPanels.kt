package com.onemnemo.grapho.ui.note

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.SegmentSize
import com.onemnemo.grapho.ui.components.SegmentedControl

/**
 * The Eraser's options card: whether it takes whole strokes or only the part it passes over, and how big it is.
 * Both apply at once.
 */
@Composable
internal fun BoxScope.EraserOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    ToolOptionsLayer(
        ui,
        NoteBarTool.Eraser,
        stringResource(R.string.wheel_tool_eraser),
        windowWidth,
        geometry,
        barTools,
    ) {
        SegmentedControl(
            options = listOf(
                false to stringResource(R.string.wheel_eraser_whole),
                true to stringResource(R.string.wheel_eraser_part),
            ),
            selected = toolbar.eraserArea,
            onSelect = { toolbar.eraserArea = it },
            size = SegmentSize.Small,
        )
        ValueSlider(
            label = stringResource(R.string.wheel_size),
            value = toolbar.eraserPt,
            range = ERASER_MIN_PT..ERASER_MAX_PT,
            valueText = stringResource(R.string.pen_width_pt, toolbar.eraserPt),
            onValue = { toolbar.eraserPt = it },
        )
    }
}

/** The Lasso's options card: whether it picks with a freehand loop or a dragged rectangle. */
@Composable
internal fun BoxScope.LassoOptionsLayer(
    ui: NoteUiState,
    toolbar: ToolbarState,
    windowWidth: Dp,
    geometry: BarGeometry,
    barTools: List<NoteBarTool>,
) {
    ToolOptionsLayer(
        ui,
        NoteBarTool.Lasso,
        stringResource(R.string.wheel_tool_lasso),
        windowWidth,
        geometry,
        barTools,
    ) {
        SegmentedControl(
            options = listOf(
                false to stringResource(R.string.wheel_lasso_freehand),
                true to stringResource(R.string.wheel_lasso_rectangle),
            ),
            selected = toolbar.lassoRect,
            onSelect = { toolbar.lassoRect = it },
            size = SegmentSize.Small,
        )
    }
}
