package com.onemnemo.grapho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace

/** Covers its parent while a long task runs, and takes every touch so nothing changes underneath. */
@Composable
fun BusyOverlay(text: String, modifier: Modifier = Modifier) {
    val c = Grapho.colors
    Box(
        modifier
            .fillMaxSize()
            .background(c.scrimDefault)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = GraphoShapes.surface, color = c.surfaceOverlay, shadowElevation = 8.dp) {
            Row(Modifier.padding(GraphoSpace.spaceLg), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    Modifier.size(Grapho.metrics.sizeIconDefault),
                    color = c.progressFill,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(GraphoSpace.spaceMd))
                Text(text, style = Grapho.type.label, color = c.textDefault)
            }
        }
    }
}
