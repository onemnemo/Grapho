package com.onemnemo.grapho.ui.note

import android.content.Context
import com.onemnemo.grapho.note.LineStyle
import com.onemnemo.grapho.note.ShapeSettings
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.note.ToolMode

/** What a [ToolbarState] keeps between notes and launches, as plain values that compare by content. */
internal data class ToolSettings(
    val mode: ToolMode,
    val drawMode: ToolMode,
    val pen: PenSetting,
    val highlighter: PenSetting,
    val penAlpha: Int,
    val highlighterAlpha: Int,
    val eraserArea: Boolean,
    val eraserPt: Float,
    val shape: ShapeSettings,
    val text: TextSettings,
    val recent: List<Int>,
    val lassoRect: Boolean = false,
)

/** The value snapshot of the persisted fields of this state. */
internal fun ToolbarState.settings() = ToolSettings(
    mode = mode,
    drawMode = drawMode,
    pen = pen,
    highlighter = highlighter,
    penAlpha = penAlpha,
    highlighterAlpha = highlighterAlpha,
    eraserArea = eraserArea,
    eraserPt = eraserPt,
    shape = shape,
    text = text,
    recent = recent.toList(),
    lassoRect = lassoRect,
)

/** Keeps the selected tool and each tool's settings in shared preferences, so they survive notes and restarts. */
internal class ToolSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Applies the saved values to [state]. A missing key keeps the state's default. */
    fun load(state: ToolbarState) {
        state.pen = readPen(PEN, state.pen, PEN_MIN_PT, PEN_MAX_PT)
        state.highlighter = readPen(HIGHLIGHTER, state.highlighter, HIGHLIGHTER_MIN_PT, HIGHLIGHTER_MAX_PT)
        state.penAlpha = readAlpha(PEN_ALPHA, state.penAlpha)
        state.highlighterAlpha = readAlpha(HIGHLIGHTER_ALPHA, state.highlighterAlpha)
        if (prefs.contains(ERASER_AREA)) state.eraserArea = prefs.getBoolean(ERASER_AREA, state.eraserArea)
        if (prefs.contains(ERASER_PT)) state.eraserPt = prefs.getFloat(ERASER_PT, state.eraserPt)
        if (prefs.contains(LASSO_RECT)) state.lassoRect = prefs.getBoolean(LASSO_RECT, state.lassoRect)
        if (prefs.contains(SHAPE)) state.shape = ShapeSettings.decode(prefs.getString(SHAPE, null))
        if (prefs.contains(TEXT)) state.text = TextSettings.decode(prefs.getString(TEXT, null))
        val recent = prefs.getString(RECENT, null).orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .map { it or OPAQUE }
            .distinct()
            .take(ToolbarState.RECENT_MAX)
        if (prefs.contains(RECENT)) {
            state.recent.clear()
            state.recent.addAll(recent)
        }
        // The draw mode goes in first, so the active tool can sit on top of it.
        state.mode = readMode(DRAW_MODE) ?: state.drawMode
        val saved = readMode(MODE) ?: return
        state.mode = if (saved == ToolMode.LASSO) state.drawMode else saved
    }

    fun save(state: ToolbarState) {
        val s = state.settings()
        prefs.edit()
            .putString(MODE, s.mode.name)
            .putString(DRAW_MODE, s.drawMode.name)
            .putInt(PEN + COLOUR, s.pen.color)
            .putFloat(PEN + WIDTH, s.pen.widthPt)
            .putString(PEN + LINE, s.pen.line.key)
            .putInt(HIGHLIGHTER + COLOUR, s.highlighter.color)
            .putFloat(HIGHLIGHTER + WIDTH, s.highlighter.widthPt)
            .putString(HIGHLIGHTER + LINE, s.highlighter.line.key)
            .putInt(PEN_ALPHA, s.penAlpha)
            .putInt(HIGHLIGHTER_ALPHA, s.highlighterAlpha)
            .putBoolean(ERASER_AREA, s.eraserArea)
            .putFloat(ERASER_PT, s.eraserPt)
            .putBoolean(LASSO_RECT, s.lassoRect)
            .putString(SHAPE, s.shape.encode())
            .putString(TEXT, s.text.encode())
            .putString(RECENT, s.recent.joinToString(","))
            .apply()
    }

    private fun readMode(key: String): ToolMode? =
        runCatching { ToolMode.valueOf(prefs.getString(key, null).orEmpty()) }.getOrNull()
            ?.takeIf { it == ToolMode.PEN || it == ToolMode.HIGHLIGHTER || key == MODE }

    private fun readAlpha(key: String, fallback: Int): Int =
        if (prefs.contains(key)) prefs.getInt(key, fallback).coerceIn(ALPHA_MIN, ALPHA_MAX) else fallback

    private fun readPen(prefix: String, fallback: PenSetting, minPt: Float, maxPt: Float): PenSetting {
        val colour = if (prefs.contains(prefix + COLOUR)) prefs.getInt(prefix + COLOUR, 0) else fallback.color
        val width = if (prefs.contains(prefix + WIDTH)) prefs.getFloat(prefix + WIDTH, 0f) else fallback.widthPt
        val line = if (prefs.contains(prefix + LINE)) {
            LineStyle.fromKey(prefs.getString(prefix + LINE, null))
        } else {
            fallback.line
        }
        return PenSetting(
            color = colour or OPAQUE,
            widthPt = if (width.isNaN()) fallback.widthPt else width.coerceIn(minPt, maxPt),
            line = line,
        )
    }

    private companion object {
        const val PREFS = "grapho_tools"
        const val MODE = "mode"
        const val DRAW_MODE = "draw_mode"
        const val PEN = "pen_"
        const val LASSO_RECT = "lasso_rect"
        const val HIGHLIGHTER = "highlighter_"
        const val COLOUR = "colour"
        const val WIDTH = "width"
        const val LINE = "line"
        const val PEN_ALPHA = "pen_alpha"
        const val HIGHLIGHTER_ALPHA = "highlighter_alpha"
        const val ERASER_AREA = "eraser_area"
        const val ERASER_PT = "eraser_pt"
        const val SHAPE = "shape"
        const val TEXT = "text"
        const val RECENT = "recent"
        const val OPAQUE = 0xFF000000.toInt()
    }
}
