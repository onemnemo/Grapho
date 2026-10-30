package com.onemnemo.grapho.note

import android.graphics.RectF
import kotlin.math.hypot

/**
 * What the pen does when a stroke looks like a shape. [SUGGEST] leaves the stroke as drawn and offers the clean
 * shape in a chip after it. [SNAP_ON_HOLD] straightens it when the pen rests at the end of the stroke, and is the
 * default. [OFF] never changes a stroke.
 */
enum class ShapeDetection(val key: String) {
    SUGGEST("suggest"),
    SNAP_ON_HOLD("hold"),
    OFF("off"),
    ;

    val suggests: Boolean get() = this == SUGGEST
    val snapsOnHold: Boolean get() = this == SNAP_ON_HOLD

    companion object {
        /** The setting saved under [key], or [SNAP_ON_HOLD] when there is none or it is not known. */
        fun fromKey(key: String?): ShapeDetection = entries.firstOrNull { it.key == key } ?: SNAP_ON_HOLD
    }
}

/** What the core calls a shape it recognised. The [code] is the core's, so the numbers never change. */
enum class ShapeGuess(val code: Int) {
    LINE(1),
    ARROW(2),
    CIRCLE(3),
    ELLIPSE(4),
    TRIANGLE(5),
    RECTANGLE(6),
    POLYGON(7),
    ARC(8),
    ELBOW(9),
    ;

    companion object {
        /** The guess for a core [code], or null for one this version does not know. */
        fun fromCode(code: Int): ShapeGuess? = entries.firstOrNull { it.code == code }
    }
}

/** A shape offered for the stroke just drawn, with the stroke's [bounds] in view pixels to place the offer by. */
class ShapeSuggestion(val guess: ShapeGuess, val bounds: RectF)

/**
 * Whether a stroke whose bounding box measures [widthPx] by [heightPx] is big enough to be offered as a shape. A
 * letter or a tick is far smaller than a shape drawn on purpose, and offering it would interrupt writing.
 */
fun worthSuggesting(widthPx: Float, heightPx: Float, minPx: Float): Boolean = hypot(widthPx, heightPx) >= minPx
