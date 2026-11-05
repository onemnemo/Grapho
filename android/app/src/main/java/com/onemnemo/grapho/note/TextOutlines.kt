package com.onemnemo.grapho.note

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.ceil
import kotlin.math.max

/**
 * The shapes of a text box's letters as polygons, laid out the way the screen lays them out, for the PDF export.
 * The result is the engine's packed polygon list: a point count, then that many x, y pairs, repeated. Points are in
 * page units with the box's turn already applied.
 */
internal object TextOutlines {
    /** How far apart the points on a curve sit, in page units. */
    private const val STEP = 0.25f

    /** Where the underline sits below the baseline and how thick it is, as a share of the font size. */
    private const val UNDERLINE_OFFSET = 0.1f
    private const val UNDERLINE_THICKNESS = 0.06f

    fun of(box: SceneText): FloatArray {
        if (box.text.isBlank()) return FloatArray(0)
        val layout = TextLayouts.build(box.text, TextLayouts.lookOf(box), box.size, box.width)
        val glyphs = Path()
        val line = Path()
        for (i in 0 until layout.lineCount) {
            var end = layout.getLineEnd(i)
            while (end > layout.getLineStart(i) && box.text[end - 1].let { it == '\n' || it == ' ' }) end--
            if (end <= layout.getLineStart(i)) continue
            val left = layout.getLineLeft(i)
            val baseline = layout.getLineBaseline(i).toFloat()
            layout.paint.getTextPath(box.text, layout.getLineStart(i), end, left, baseline, line)
            glyphs.addPath(line)
            line.rewind()
            if (box.underline) {
                val top = baseline + box.size * UNDERLINE_OFFSET
                val right = left + layout.getLineMax(i)
                glyphs.addRect(left, top, right, top + box.size * UNDERLINE_THICKNESS, Path.Direction.CW)
            }
        }
        val place = Matrix().apply {
            setTranslate(box.x, box.y)
            postRotate(
                Math.toDegrees(box.rotation.toDouble()).toFloat(),
                box.x + box.width / 2f,
                box.y + box.height / 2f,
            )
        }
        glyphs.transform(place)
        return flatten(glyphs)
    }

    private fun flatten(path: Path): FloatArray {
        val out = ArrayList<Float>()
        val measure = PathMeasure(path, false)
        val pos = FloatArray(2)
        do {
            val length = measure.length
            if (length <= 0f) continue
            val count = max(4, ceil(length / STEP).toInt())
            out.add(count.toFloat())
            for (k in 0 until count) {
                measure.getPosTan(length * k / count, pos, null)
                out.add(pos[0])
                out.add(pos[1])
            }
        } while (measure.nextContour())
        return out.toFloatArray()
    }
}
