package com.onemnemo.grapho.ui.note

import android.content.Context

/**
 * What the person chose for the note bar, in shared preferences: its style (floating is the default), the tools on
 * it in order, whether it shows the note's title, the edge it sits on (top is the default) and when it hides.
 */
class NoteBarStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): NoteBarStyle {
        val stored = prefs.getString(STYLE, null)
        return NoteBarStyle.entries.firstOrNull { it.name == stored } ?: NoteBarStyle.Floating
    }

    fun save(style: NoteBarStyle) = prefs.edit().putString(STYLE, style.name).apply()

    /** The tools on the bar, in order. A person who has switched every tool off keeps an empty bar. */
    fun loadTools(): List<NoteBarTool> {
        val saved = prefs.getString(TOOLS, null)?.let(NoteBarTool::decode) ?: return NoteBarTool.defaults
        // A bar saved before Shapes or Text existed gets each once. After that it can be switched off and stays off.
        val shapesNew = !prefs.getBoolean(SHAPES_OFFERED, false)
        val textNew = !prefs.getBoolean(TEXT_OFFERED, false)
        if (!shapesNew && !textNew) return saved
        val withShapes = if (shapesNew) NoteBarTool.withShapesOffered(saved) else saved
        val offered = if (textNew) NoteBarTool.withTextOffered(withShapes) else withShapes
        saveTools(offered)
        return offered
    }

    fun saveTools(tools: List<NoteBarTool>) = prefs.edit()
        .putString(TOOLS, NoteBarTool.encode(tools))
        .putBoolean(SHAPES_OFFERED, true)
        .putBoolean(TEXT_OFFERED, true)
        .apply()

    fun loadShowTitle(): Boolean = prefs.getBoolean(SHOW_TITLE, true)

    fun saveShowTitle(show: Boolean) = prefs.edit().putBoolean(SHOW_TITLE, show).apply()

    fun loadPosition(): BarPosition = barPositionFrom(prefs.getString(POSITION, null))

    fun savePosition(position: BarPosition) = prefs.edit().putString(POSITION, position.name).apply()

    fun loadAutoHide(): AutoHide = autoHideFrom(prefs.getString(AUTO_HIDE, null))

    fun saveAutoHide(autoHide: AutoHide) = prefs.edit().putString(AUTO_HIDE, autoHide.name).apply()

    private companion object {
        const val PREFS = "grapho_note_bar"
        const val STYLE = "style"
        const val TOOLS = "tools"
        const val SHAPES_OFFERED = "shapes_offered"
        const val TEXT_OFFERED = "text_offered"
        const val SHOW_TITLE = "show_title"
        const val POSITION = "position"
        const val AUTO_HIDE = "auto_hide"
    }
}
