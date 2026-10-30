package com.onemnemo.grapho.ui.note

import android.graphics.RectF
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.components.floatingCard
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/**
 * The Compare chip under a picture whose background was just removed: hold it to see the original, let go for the
 * cut-out. [onHold] is told when the hold starts and ends, and also if the touch is taken away. Under the chip sits
 * the panel for the cut itself, showing [options] and reporting changes to [onOptions], and the [brush] tools with
 * [onTool] and [onDone]. While a brush is on, drags over the picture paint it; [crop] is the part of its source shown.
 */
@Composable
internal fun PictureCompare(
    rect: State<RectF?>,
    insets: EdgeInsets,
    options: CutoutOptions,
    onOptions: (CutoutOptions) -> Unit,
    brush: CutoutBrush,
    crop: RectF,
    onTool: (CutoutTool) -> Unit,
    onDone: () -> Unit,
    onHold: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val gap = with(density) { GraphoSpace.spaceMd.toPx() }
    val margins = with(density) {
        EdgeInsets(insets.left.dp.toPx(), insets.top.dp.toPx(), insets.right.dp.toPx(), insets.bottom.dp.toPx())
    }
    val label = stringResource(R.string.picture_compare)
    val hint = stringResource(R.string.picture_compare_hint)
    Box(Modifier.fillMaxSize()) {
        if (brush.tool != CutoutTool.Auto) PictureBrushOverlay(brush, rect, crop)
        Column(
            Modifier.placedUnder(rect, gap, margins),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GraphoSpace.spaceSm),
        ) {
            Row(
                Modifier
                    .floatingCard(GraphoShapes.pill)
                    .semantics { contentDescription = hint }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onHold(true)
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onHold(false)
                                }
                            },
                        )
                    }
                    .heightIn(min = Grapho.metrics.sizeControlSmall)
                    .padding(horizontal = GraphoSpace.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GraphoIcon(R.drawable.ic_compare, null, Grapho.colors.textMuted)
                Text(
                    label,
                    style = Grapho.type.label,
                    color = Grapho.colors.textDefault,
                    modifier = Modifier.padding(start = GraphoSpace.spaceSm),
                )
            }
            PictureCutoutPanel(options, onOptions, brush, onTool, onDone)
        }
    }
}
