package com.onemnemo.grapho.note

import com.onemnemo.grapho.engine.NativeNotebook
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A text box being typed into. [id] is null for a box that is not in the note yet; it is added when the edit ends.
 * The frame is in page-local units and [style] is what the box is drawn with, which the options card can change
 * while the box is open. [startText] and [startStyle] are what the box held when it opened, so an edit that changed
 * nothing is not saved, and so is [moved], which is true once the box was dragged or resized. [serial] tells one
 * edit from the next, so a field starts afresh for each.
 */
data class TextEdit(
    val page: Int,
    val id: StrokeId?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val startText: String,
    val startStyle: TextSettings,
    val style: TextSettings,
    val serial: Int,
    val moved: Boolean = false,
)

/** What the text editor needs from the canvas it lives in. */
internal interface TextHost {
    val currentViewport: Viewport

    fun pageId(page: Int): String?

    /** The text boxes the page shows, in drawing order. */
    fun texts(pageId: String): List<SceneText>

    /** True, after telling the person why, when the page's active layer takes no new boxes. */
    fun blocked(page: Int): Boolean

    /** The open edit changed or ended: the box it holds is hidden from the page while it is open. */
    fun editChanged(edit: TextEdit?)

    /** The document changed on [page]: read it again, show it and save it. */
    fun documentEdited(page: Int)
}

/**
 * The Text tool: a tap on a box opens it for typing, a tap on empty page opens a new one. The typing itself happens in
 * a field over the page; this keeps the box it belongs to, what has been typed, and writes the result to the note as
 * one undo step when the edit ends. Touch input arrives from the canvas on the UI thread.
 */
internal class TextEditor(private val notebook: Notebook, private val host: TextHost) {
    @Volatile var edit: TextEdit? = null
        private set

    private var draft = ""
    private var serial = 0
    private var hiddenBox: HiddenStrokes? = null
    private val scratch = FloatArray(2)

    /** The box held out of the page's render while it is typed into, or null. */
    fun hidden(): HiddenStrokes? = hiddenBox

    /**
     * A tap at ([x], [y]) in view pixels on [page]: ends any edit in progress, then opens the box under the tap or a
     * new one there, with [style] for a new box.
     */
    fun tap(x: Float, y: Float, page: Int, style: TextSettings) {
        commit()
        val pageId = host.pageId(page) ?: return
        val vp = host.currentViewport
        val localX = vp.localX(page, x)
        val localY = vp.localY(page, y)
        val hit = NativeNotebook.nativeTextAt(notebook.nativeHandle, page, localX, localY)
        val box = if (hit.size >= 2) {
            host.texts(pageId).firstOrNull { it.id == StrokeId(hit[0], hit[1]) }
        } else {
            null
        }
        if (box != null) {
            open(page, box)
        } else if (hit.isEmpty() && !host.blocked(page)) {
            val width = TextSettings.DEFAULT_WIDTH
            val left = if (vp.infinite) localX else TextFrame.keptInside(localX, width, vp.pages.width(page))
            val height = style.size * TextLayouts.LINE_HEIGHT
            begin(TextEdit(page, null, left, localY, width, height, 0f, "", style, style, ++serial), pageId, "")
        }
    }

    private fun open(page: Int, box: SceneText) {
        val style = TextSettings(
            size = box.size.roundToInt(),
            argb = box.argb,
            align = box.align,
            bold = box.bold,
            italic = box.italic,
            underline = box.underline,
        ).coerced()
        val frame = TextEdit(
            page,
            box.id,
            box.x,
            box.y,
            box.width,
            box.height,
            box.rotation,
            box.text,
            style,
            style,
            ++serial,
        )
        begin(frame, host.pageId(page) ?: return, box.text)
    }

    private fun begin(next: TextEdit, pageId: String, text: String) {
        draft = text
        edit = next
        hiddenBox = next.id?.let { HiddenStrokes(next.page, pageId, emptySet(), setOf(it)) }
        host.editChanged(next)
    }

    /** What the field holds now, kept so the edit can end from anywhere. */
    fun typed(text: String) {
        if (edit != null) draft = text
    }

    /** Draws and saves the open box with [style] from now on. */
    fun restyle(style: TextSettings) {
        val current = edit ?: return
        reframe(current.copy(style = style.coerced()))
    }

    /** Moves the open box by ([dx], [dy]) along its own axes, in page-local units. */
    fun move(dx: Float, dy: Float) {
        val current = edit ?: return
        val c = cos(current.rotation)
        val s = sin(current.rotation)
        reframe(current.copy(x = current.x + dx * c - dy * s, y = current.y + dx * s + dy * c, moved = true))
    }

    /**
     * Drags a side of the open box by [delta] along the box, page-local units: the left side when [fromLeft], else
     * the right. The box takes the height its lines need at the new width.
     */
    fun stretch(fromLeft: Boolean, delta: Float) {
        val current = edit ?: return
        val width = TextFrame.draggedWidth(current.width, fromLeft, delta)
        val height = heightOf(draft, current.style, width)
        TextFrame.resized(
            current.x,
            current.y,
            current.width,
            current.height,
            current.rotation,
            width,
            height,
            fromLeft,
            scratch,
        )
        reframe(current.copy(x = scratch[0], y = scratch[1], width = width, height = height, moved = true))
    }

    private fun reframe(next: TextEdit) {
        edit = next
        host.editChanged(next)
    }

    /**
     * Ends the edit and writes it to the note: a new box is added, a changed one updated with the height its lines
     * need, and one left empty is removed. One undo step, or none when nothing changed.
     */
    fun commit() {
        val current = edit ?: return
        val text = draft
        edit = null
        hiddenBox = null
        val changed = write(current, text)
        host.editChanged(null)
        if (changed) host.documentEdited(current.page)
    }

    private fun write(current: TextEdit, text: String): Boolean {
        val handle = notebook.nativeHandle
        val style = current.style
        val id = current.id
        val blank = text.isBlank()
        if (id == null) {
            if (blank) return false
            val height = heightOf(text, style, current.width)
            val frame = floatArrayOf(current.x, current.y, current.width, height)
            return NativeNotebook.nativeAddText(
                handle,
                current.page,
                frame,
                text,
                style.size.toFloat(),
                style.argb,
                style.align.code,
                style.bold,
                style.italic,
                style.underline,
            ).isNotEmpty()
        }
        val key = longArrayOf(id.hi, id.lo)
        if (blank) return NativeNotebook.nativeDeleteText(handle, current.page, key)
        if (text == current.startText && style == current.startStyle && !current.moved) return false
        val height = heightOf(text, style, current.width)
        TextFrame.regrown(current.x, current.y, current.width, current.height, current.rotation, height, scratch)
        val frame = floatArrayOf(scratch[0], scratch[1], current.width, height, current.rotation)
        return NativeNotebook.nativeUpdateText(
            handle,
            current.page,
            key,
            frame,
            text,
            style.size.toFloat(),
            style.argb,
            style.align.code,
            style.bold,
            style.italic,
            style.underline,
        )
    }

    private fun heightOf(text: String, style: TextSettings, width: Float): Float =
        TextLayouts.height(text, style, style.size.toFloat(), width)

    /**
     * Where the open box sits in view pixels, for the field over it: its turned top left corner (`out[0]`,
     * `out[1]`), its width (`out[2]`), pixels per point (`out[3]`) and its turn in degrees (`out[4]`). False when no
     * box is open.
     */
    fun placement(out: FloatArray): Boolean {
        val current = edit ?: return false
        val vp = host.currentViewport
        TextFrame.corner(current.x, current.y, current.width, current.height, current.rotation, scratch)
        out[0] = vp.viewX(current.page, scratch[0])
        out[1] = vp.viewY(current.page, scratch[1])
        out[2] = current.width * vp.scale
        out[3] = vp.scale
        out[4] = Math.toDegrees(current.rotation.toDouble()).toFloat()
        return true
    }
}
