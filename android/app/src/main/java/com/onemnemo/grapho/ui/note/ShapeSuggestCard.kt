package com.onemnemo.grapho.ui.note

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ShapeGuess
import com.onemnemo.grapho.note.ShapeSuggestion
import com.onemnemo.grapho.ui.components.ButtonKind
import com.onemnemo.grapho.ui.components.ButtonSize
import com.onemnemo.grapho.ui.components.GraphoButton
import com.onemnemo.grapho.ui.components.GraphoIconButton
import com.onemnemo.grapho.ui.theme.Grapho
import com.onemnemo.grapho.ui.theme.GraphoSpace
import kotlinx.coroutines.delay

/** How long the offer stays before it goes by itself. */
internal const val SHAPE_SUGGEST_MS = 5000L

@StringRes
internal fun guessName(guess: ShapeGuess): Int = when (guess) {
    ShapeGuess.LINE -> R.string.shape_guess_line
    ShapeGuess.ARROW -> R.string.shape_guess_arrow
    ShapeGuess.CIRCLE -> R.string.shape_guess_circle
    ShapeGuess.ELLIPSE -> R.string.shape_guess_ellipse
    ShapeGuess.TRIANGLE -> R.string.shape_guess_triangle
    ShapeGuess.RECTANGLE -> R.string.shape_guess_rectangle
    ShapeGuess.POLYGON -> R.string.shape_guess_polygon
    ShapeGuess.ARC -> R.string.shape_guess_arc
    ShapeGuess.ELBOW -> R.string.shape_guess_elbow
}

/**
 * "Did you mean": a small pill under a stroke that looks like a shape, offering the clean shape. Choosing it swaps
 * the stroke for the shape ([onAccept]); the cross, or five seconds, keeps the drawing as it is ([onDismiss]).
 */
@Composable
internal fun ShapeSuggestCard(
    suggestion: ShapeSuggestion,
    insets: EdgeInsets,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(suggestion) {
        delay(SHAPE_SUGGEST_MS)
        onDismiss()
    }
    CardSurface(suggestion.bounds, insets) {
        Text(
            stringResource(R.string.shape_suggest_question),
            style = Grapho.type.label,
            color = Grapho.colors.textMuted,
            modifier = Modifier.padding(horizontal = GraphoSpace.spaceSm),
        )
        GraphoButton(
            text = stringResource(guessName(suggestion.guess)),
            onClick = onAccept,
            kind = ButtonKind.Primary,
            size = ButtonSize.Small,
        )
        GraphoIconButton(R.drawable.ic_close, stringResource(R.string.shape_suggest_keep), onDismiss)
    }
}
