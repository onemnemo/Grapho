package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.GraphoIcon
import com.onemnemo.grapho.ui.theme.GraphoContent
import com.onemnemo.grapho.ui.theme.GraphoShapes

private val MarkSize = 24.dp
private val StarSize = 14.dp

/** The small star on the corner of a starred cover. It sits over art, so it takes the media colours. */
@Composable
fun StarMark(modifier: Modifier = Modifier) {
    Box(
        modifier.size(MarkSize).clip(GraphoShapes.pill).background(GraphoContent.mediaGlass),
        contentAlignment = Alignment.Center,
    ) {
        GraphoIcon(
            R.drawable.ic_star_filled,
            stringResource(R.string.library_starred),
            GraphoContent.mediaText,
            size = StarSize,
        )
    }
}
