package com.onemnemo.grapho.note

/** What the Shapes tool draws. The [code] is what the core's shape drawing takes, so the numbers never change. */
enum class ShapeKind(val code: Int, val key: String) {
    BOX(0, "box"),
    OVAL(1, "oval"),
    POLYGON(2, "polygon"),
    STAR(3, "star"),
    LINE(4, "line"),
    ;

    /** Whether this kind is a line, which takes a path and line ends. */
    val isLine: Boolean get() = this == LINE

    companion object {
        fun fromKey(key: String?): ShapeKind = entries.firstOrNull { it.key == key } ?: BOX
    }
}

/** How a line runs between its two ends. [code] is the core's kind for a line of this path. */
enum class LinePath(val code: Int, val key: String) {
    STRAIGHT(4, "straight"),
    CURVED(6, "curved"),
    ELBOW(5, "elbow"),
    ;

    companion object {
        fun fromKey(key: String?): LinePath = entries.firstOrNull { it.key == key } ?: STRAIGHT
    }
}

/** What a line ends in. */
enum class LineEnd(val key: String) {
    NONE("none"),
    ARROW("arrow"),
    ;

    companion object {
        fun fromKey(key: String?): LineEnd = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * The choices of the Shapes tool: which [kind] to draw and the settings that belong to it. Colour, width and line
 * style come from the pen. Every number is kept inside its range by [coerced].
 */
data class ShapeSettings(
    val kind: ShapeKind = ShapeKind.BOX,
    /** A star's points. */
    val points: Int = 5,
    /** How deep a star's notches cut, in percent. */
    val sharpness: Int = 50,
    /** A polygon's sides. */
    val sides: Int = 5,
    /** A box's corner radius in points. */
    val radius: Int = 0,
    val path: LinePath = LinePath.STRAIGHT,
    val start: LineEnd = LineEnd.NONE,
    val end: LineEnd = LineEnd.ARROW,
) {
    fun coerced() = copy(
        points = points.coerceIn(MIN_SIDES, MAX_SIDES),
        sharpness = sharpness.coerceIn(MIN_SHARPNESS, MAX_SHARPNESS),
        sides = sides.coerceIn(MIN_SIDES, MAX_SIDES),
        radius = radius.coerceIn(0, MAX_RADIUS),
    )

    /**
     * The shape as the core's drawing takes it: `[kind, a, b, head, ends]`. [scale] is view pixels per point and
     * [widthPt] the pen's width, which sizes an arrow head.
     */
    fun spec(scale: Float, widthPt: Float): FloatArray {
        val s = coerced()
        val code = if (s.kind.isLine) s.path.code else s.kind.code
        val a = when (s.kind) {
            ShapeKind.BOX -> s.radius * scale
            ShapeKind.POLYGON -> s.sides.toFloat()
            ShapeKind.STAR -> s.points.toFloat()
            else -> 0f
        }
        val b = if (s.kind == ShapeKind.STAR) s.sharpness / PERCENT else 0f
        val ends = if (s.kind.isLine) {
            (if (s.start == LineEnd.ARROW) START_ARROW else 0) or (if (s.end == LineEnd.ARROW) END_ARROW else 0)
        } else {
            0
        }
        return floatArrayOf(code.toFloat(), a, b, headSize(widthPt) * scale, ends.toFloat())
    }

    /** One string for storage. */
    fun encode(): String = listOf(
        kind.key,
        points,
        sharpness,
        sides,
        radius,
        path.key,
        start.key,
        end.key,
    ).joinToString(",")

    companion object {
        const val MIN_SIDES = 3
        const val MAX_SIDES = 12
        const val MIN_SHARPNESS = 10
        const val MAX_SHARPNESS = 90
        const val MAX_RADIUS = 50
        private const val PERCENT = 100f
        private const val START_ARROW = 1
        private const val END_ARROW = 2
        private const val FIELDS = 8
        private const val HEAD_BASE = 6f
        private const val HEAD_GROWTH = 3f

        /** An arrow head grows with the line, but slower than it, so a thin line keeps a clear head. */
        fun headSize(widthPt: Float): Float = HEAD_BASE + widthPt * HEAD_GROWTH

        /** The settings saved by [encode], or the defaults for anything missing or damaged. */
        fun decode(stored: String?): ShapeSettings {
            val f = stored?.split(',') ?: return ShapeSettings()
            if (f.size != FIELDS) return ShapeSettings()
            val fallback = ShapeSettings()
            return ShapeSettings(
                kind = ShapeKind.fromKey(f[0]),
                points = f[1].toIntOrNull() ?: fallback.points,
                sharpness = f[2].toIntOrNull() ?: fallback.sharpness,
                sides = f[3].toIntOrNull() ?: fallback.sides,
                radius = f[4].toIntOrNull() ?: fallback.radius,
                path = LinePath.fromKey(f[5]),
                start = LineEnd.fromKey(f[6]),
                end = LineEnd.fromKey(f[7]),
            ).coerced()
        }
    }
}
