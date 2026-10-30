package com.onemnemo.grapho.note

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.ceil
import kotlin.math.roundToInt

/** How a text box is laid out, shared by the painter and by whatever measures a box before it is saved. */
internal object TextLayouts {
    /** A line of text is this many times its size tall, about what the default font needs. */
    const val LINE_HEIGHT = 1.25f

    private fun style(look: TextSettings): Int = when {
        look.bold && look.italic -> Typeface.BOLD_ITALIC
        look.bold -> Typeface.BOLD
        look.italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }

    /** The look of a box as the layout needs it, whatever its colour and size. */
    fun lookOf(box: SceneText) = TextSettings(
        align = box.align,
        bold = box.bold,
        italic = box.italic,
        underline = box.underline,
    )

    /**
     * The layout of [text] in a box [width] wide, with the font [size] in page-local units. Only the weight, slant,
     * underline and alignment of [look] count.
     */
    fun build(text: String, look: TextSettings, size: Float, width: Float): StaticLayout {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            isLinearText = true
            isUnderlineText = look.underline
            typeface = Typeface.create(Typeface.DEFAULT, style(look))
        }
        val alignment = when (look.align) {
            TextAlignment.Start -> Layout.Alignment.ALIGN_NORMAL
            TextAlignment.Centre -> Layout.Alignment.ALIGN_CENTER
            TextAlignment.End -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val wide = ceil(width).toInt().coerceAtLeast(1)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, wide)
            .setAlignment(alignment)
            .setIncludePad(false)
            .build()
    }

    /** The height the lines of [text] need in a box [width] wide: at least one line, so an empty box can be clicked. */
    fun height(text: String, look: TextSettings, size: Float, width: Float): Float =
        maxOf(build(text, look, size, width).height.toFloat(), size * LINE_HEIGHT)
}

/**
 * Draws text boxes onto a page canvas that is scaled to page-local units. Keeps the layouts it built, so one
 * instance belongs to one drawing thread.
 */
internal class TextPainter {
    private val layouts = object : LinkedHashMap<SceneText, StaticLayout>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<SceneText, StaticLayout>?): Boolean =
            size > MAX_LAYOUTS
    }

    /**
     * Draws [box] at [alpha] (0 to 255) turned about its centre. Near-black text turns near-white on dark [paper],
     * as ink does.
     */
    fun draw(canvas: Canvas, box: SceneText, paper: Int, alpha: Int = LAYER_OPAQUE) {
        val layout = layouts.getOrPut(box) {
            TextLayouts.build(box.text, TextLayouts.lookOf(box), box.size, box.width)
        }
        val colour = displayColor(box.argb, paper)
        layout.paint.color = colour
        layout.paint.alpha = (Color.alpha(colour) * alpha / LAYER_OPAQUE.toFloat()).roundToInt()
        canvas.save()
        canvas.rotate(
            Math.toDegrees(box.rotation.toDouble()).toFloat(),
            box.x + box.width / 2f,
            box.y + box.height / 2f,
        )
        canvas.translate(box.x, box.y)
        layout.draw(canvas)
        canvas.restore()
    }

    private companion object {
        const val MAX_LAYOUTS = 128
    }
}
