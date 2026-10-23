package com.onemnemo.grapho.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.onemnemo.grapho.ui.theme.Grapho

@Composable
fun GraphoIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Grapho.metrics.sizeIconDefault,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}
