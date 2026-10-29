package com.onemnemo.grapho.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.floatingSurface
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoShapes
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlin.math.roundToInt

private val RailWidth = 28.dp
private val LetterMax = 24.dp
private val BubbleSize = 72.dp

/** Letters with no group behind them are dimmed. */
private const val EMPTY_LETTER_ALPHA = 0.45f

/**
 * A to Z down the edge. Press and slide to jump between groups; a bubble names the letter under the finger.
 * [available] are the letters that have a group, and [active] the letter of the group at the top of the page.
 */
@Composable
fun AlphabetRail(available: Set<String>, active: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Grapho.colors
    val density = LocalDensity.current
    var touch by remember { mutableStateOf<Pair<String, Float>?>(null) }
    val label = stringResource(R.string.library_jump_to_letter)
    Box(modifier.fillMaxHeight().heightIn(max = LetterMax * RAIL_LETTERS.size).width(RailWidth)) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(RailWidth)
                .semantics { contentDescription = label }
                .pointerInput(available) {
                    awaitEachGesture {
                        fun pick(y: Float) {
                            val index = (y / size.height * RAIL_LETTERS.size).toInt()
                            val letter = nearestLetter(index, available) ?: return
                            touch = letter to y.coerceIn(0f, size.height.toFloat())
                            onSelect(letter)
                        }
                        val down = awaitFirstDown()
                        pick(down.position.y)
                        down.consume()
                        drag(down.id) { move ->
                            pick(move.position.y)
                            move.consume()
                        }
                        touch = null
                    }
                },
        ) {
            for (letter in RAIL_LETTERS) {
                val on = (touch?.first ?: active) == letter
                Box(
                    Modifier
                        .weight(1f)
                        .width(RailWidth)
                        .clip(GraphoShapes.item)
                        .background(if (on) c.selectionFill else Color.Transparent)
                        .alpha(if (letter in available) 1f else EMPTY_LETTER_ALPHA),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        letter,
                        style = Grapho.type.micro,
                        color = if (on) c.textDefault else c.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        touch?.let { (letter, y) ->
            val gap = with(density) { (RailWidth + GraphoSpace.spaceLg).roundToPx() }
            val half = with(density) { (BubbleSize / 2).roundToPx() }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-gap, y.roundToInt() - half) }
                    .size(BubbleSize)
                    .floatingSurface(GraphoShapes.pill),
                contentAlignment = Alignment.Center,
            ) { Text(letter, style = Grapho.type.display, color = c.textDefault) }
        }
    }
}
