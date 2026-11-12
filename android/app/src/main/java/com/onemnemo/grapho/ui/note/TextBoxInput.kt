package com.onemnemo.grapho.ui.note

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.InputFilter
import android.text.InputType
import android.text.Layout
import android.text.Selection
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import com.onemnemo.grapho.note.FormatState
import com.onemnemo.grapho.note.HeadingLook
import com.onemnemo.grapho.note.ParaKinds
import com.onemnemo.grapho.note.ParaSpan
import com.onemnemo.grapho.note.RichEdit
import com.onemnemo.grapho.note.RichPaste
import com.onemnemo.grapho.note.RichSpans
import com.onemnemo.grapho.note.RichText
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextLayouts
import com.onemnemo.grapho.note.TextSettings
import com.onemnemo.grapho.note.displayColor
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The field a text box is typed into. It is a platform EditText rather than a Compose field so the S Pen can write
 * into it: Samsung's handwriting to text and the system's stylus handwriting only know platform text views.
 *
 * Text is drawn at the page's scale, so it lines up with how the box looks once it is closed. A finger dragged
 * across the text moves the whole box instead of selecting, while a tap still puts the cursor down, a long press
 * still selects and the pen still writes.
 */
@SuppressLint("ViewConstructor")
internal class TextBoxInput(
    context: Context,
    startText: String,
    startRich: RichText,
    boxSize: Float,
    private val paper: Int,
    startMarks: Int,
) : EditText(context) {
    /** Gets each step of a finger drag across the text, in the box's own page-local units. */
    var onDrag: ((dx: Float, dy: Float) -> Unit)? = null

    /** Gets the whole text after every change. */
    var onTyped: ((String) -> Unit)? = null

    /** Gets what the selection or cursor looks like, whenever it or the text under it changes. */
    var onFormat: ((FormatState) -> Unit)? = null

    /** Told when a lot of text arrived in one go, such as a paste, so the box can make room for it. */
    var onBigInsert: (() -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPress = ViewConfiguration.getLongPressTimeout()
    private var fontSize = 0f
    private var boxFont = startRich.font
    private var appliedFace: Pair<Int, Int>? = null
    private var lastLook: TextSettings? = null
    private var ready = false
    private var fixing = false
    private var changeStart = 0
    private var changeBefore = 0
    private var changeCount = 0
    private var cursorColour: Int? = null
    private var scale = 1f
    private var degrees = 0f
    private var tracking = false
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    init {
        background = null
        setPadding(0, 0, 0, 0)
        includeFontPadding = false
        // The same breaking the painter's StaticLayout uses, so lines wrap at the same words once the box closes.
        breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        filters = arrayOf(InputFilter.LengthFilter(TextSettings.MAX_CHARS))
        setText(RichSpans.build(startText, startRich, boxSize, boxSize, paper, editing = true), BufferType.EDITABLE)
        for (bit in RichSpans.MARK_BITS) if (startMarks and bit != 0) RichEdit.set(text, bit, 0, text.length, true)
        RichEdit.fix(text, boxSize)
        setSelection(text.length)
        addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    changeStart = start
                    changeBefore = before
                    changeCount = count
                }

                override fun afterTextChanged(s: Editable?) {
                    if (fixing || s == null) return
                    fixing = true
                    try {
                        leaveEmptyLine(s)
                        RichEdit.fix(s, em())
                    } finally {
                        fixing = false
                    }
                    if (changeCount >= BIG_INSERT) onBigInsert?.invoke()
                    onTyped?.invoke(s.toString())
                    reportFormat()
                }
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // The pen only starts writing inside the field, so it can still grab the handles round it.
            setHandwritingBoundsOffsets(0f, 0f, 0f, 0f)
        }
        ready = true
        reportFormat()
    }

    // The em of the spans: the size of the text in the pixels it is drawn in.
    private fun em(): Float = fontSize * scale

    // A line break typed on a list or quote line with nothing on it ends the list instead of starting a new line.
    private fun leaveEmptyLine(s: Editable) {
        if (changeCount != 1 || changeBefore != 0 || s.getOrNull(changeStart) != '\n') return
        val lineStart = s.lastIndexOf('\n', changeStart - 1) + 1
        if (lineStart != changeStart) return
        val span =
            s.getSpans(lineStart, lineStart, ParaSpan::class.java).firstOrNull { s.getSpanStart(it) == lineStart }
                ?: return
        if (span.kind == ParaKinds.BODY || ParaKinds.isHeading(span.kind)) return
        s.delete(changeStart, changeStart + 1)
        span.kind = ParaKinds.BODY
        span.indent = 0
    }

    private fun reportFormat() {
        if (ready) {
            onFormat?.invoke(
                RichEdit.state(text, minOf(selectionStart, selectionEnd), maxOf(selectionStart, selectionEnd)),
            )
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        reportFormat()
    }

    private fun range(): Pair<Int, Int> = minOf(selectionStart, selectionEnd) to maxOf(selectionStart, selectionEnd)

    /** The formatting held by the text now, ready to save. */
    fun snapshot(): RichText = RichSpans.read(text, fontSize).copy(font = boxFont)

    /** Whether the field holds nothing yet. */
    val isBlank: Boolean get() = text.isEmpty()

    /** Turns a mark on or off for the selection, or for what is typed next. */
    fun toggleMark(bit: Int) {
        val (a, b) = range()
        RichEdit.toggle(text, bit, a, b)
        reportFormat()
    }

    /** Colours the selection, or what is typed next. Null takes the colour off. */
    fun setColour(argb: Int?) {
        val (a, b) = range()
        RichEdit.colour(text, argb, argb?.let { displayColor(it, paper) } ?: 0, a, b)
        reportFormat()
    }

    /** Puts a marker colour behind the selection, or what is typed next. Null takes it off. */
    fun setHighlight(argb: Int?) {
        val (a, b) = range()
        RichEdit.highlight(text, argb, a, b)
        reportFormat()
    }

    /** Sets the size of the selection, or of what is typed next, in points. Null takes the size off. */
    fun setSize(points: Float?) {
        val (a, b) = range()
        RichEdit.size(text, points, fontSize, a, b)
        reportFormat()
    }

    /** Sets the typeface of the selection, or of the whole box when nothing is selected. */
    fun setFont(id: Int) {
        val (a, b) = range()
        if (a != b) {
            RichEdit.font(text, id, a, b)
        } else {
            boxFont = id
            RichEdit.font(text, null, 0, text.length)
            applyFace()
        }
        reportFormat()
    }

    /** Gives the lines under the selection a kind, or plain if they all have it already. */
    fun setKind(kind: Int) {
        val (a, b) = range()
        RichEdit.setKind(text, kind, a, b, em())
        reportFormat()
    }

    /** Moves the list lines under the selection [by] steps. */
    fun indent(by: Int) {
        val (a, b) = range()
        RichEdit.indent(text, by, a, b, em())
        reportFormat()
    }

    /** Puts the clipboard at the cursor, with the formatting a box can show and the size of this box. */
    fun pasteClipboard(plain: Boolean) {
        val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val item = clip.getItemAt(0)
        val html = if (plain) null else item.htmlText
        val pasted = if (html != null) {
            RichPaste.fromStyled(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT))
        } else {
            RichPaste.fromPlain(item.coerceToText(context).toString())
        }
        if (pasted.text.isEmpty()) return
        val (a, b) = range()
        val room = TextSettings.MAX_CHARS - (text.length - (b - a))
        if (room <= 0) return
        val cut = pasted.text.take(room)
        val firstLine = text.substring(0, a).count { it == '\n' }
        val fresh = (a == 0 || text[a - 1] == '\n') && (b == text.length || text[b] == '\n')
        val built = RichSpans.build(cut, pasted.rich, fontSize, em(), paper, editing = true)
        for (span in built.getSpans(0, built.length, ParaSpan::class.java)) built.removeSpan(span)
        for (span in built.getSpans(0, built.length, HeadingLook::class.java)) built.removeSpan(span)
        text.replace(a, b, built)
        val lines = RichSpans.lineRanges(cut).size
        for (k in 0 until lines) {
            if (k == 0 && !fresh) continue
            val p = pasted.rich.para(k)
            RichEdit.setPara(text, firstLine + k, p.kind, p.indent, p.checked)
        }
        RichEdit.fix(text, em())
        Selection.setSelection(text, (a + built.length).coerceAtMost(text.length))
        reportFormat()
    }

    override fun onTextContextMenuItem(id: Int): Boolean = when (id) {
        android.R.id.paste -> true.also { pasteClipboard(plain = false) }
        android.R.id.pasteAsPlainText -> true.also { pasteClipboard(plain = true) }
        else -> super.onTextContextMenuItem(id)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(base, true) {
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
                if (beforeLength == 1 && afterLength == 0 &&
                    leaveListStart()
                ) {
                    true
                } else {
                    super.deleteSurroundingText(beforeLength, afterLength)
                }

            override fun sendKeyEvent(event: KeyEvent): Boolean =
                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL && leaveListStart()) {
                    true
                } else {
                    super.sendKeyEvent(event)
                }
        }
    }

    // Backspace at the start of a list, quote or heading line makes it less of one before it joins the line above.
    private fun leaveListStart(): Boolean {
        val at = selectionStart
        if (at != selectionEnd || at < 0) return false
        if (at != 0 && text[at - 1] != '\n') return false
        val span =
            text.getSpans(at, at, ParaSpan::class.java).firstOrNull { text.getSpanStart(it) == at } ?: return false
        if (span.kind == ParaKinds.BODY) return false
        if (span.indent > 0) span.indent-- else span.kind = ParaKinds.BODY
        RichEdit.fix(text, em())
        reportFormat()
        return true
    }

    // The box's typeface with the weight and slant of the last look, set again only when one of them changed.
    private fun applyFace() {
        val look = lastLook ?: return
        val weight = when {
            look.bold && look.italic -> Typeface.BOLD_ITALIC
            look.bold -> Typeface.BOLD
            look.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        if (appliedFace == boxFont to weight) return
        appliedFace = boxFont to weight
        typeface = TextLayouts.faceOf(boxFont, look)
    }

    /** Draws the text with [style] in [ink], with the cursor and selection in [accent]. */
    fun applyLook(style: TextSettings, ink: Int, accent: Int) {
        lastLook = style
        applyFace()
        paintFlags = if (style.underline) {
            paintFlags or Paint.UNDERLINE_TEXT_FLAG
        } else {
            paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
        }
        gravity = Gravity.TOP or when (style.align) {
            TextAlignment.Start -> Gravity.START
            TextAlignment.Centre -> Gravity.CENTER_HORIZONTAL
            TextAlignment.End -> Gravity.END
        }
        setTextColor(ink)
        highlightColor = ColorUtils.setAlphaComponent(accent, SELECTION_ALPHA)
        if (accent != cursorColour) {
            cursorColour = accent
            val wide = (CURSOR_DP * resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
            textCursorDrawable = GradientDrawable().apply {
                setColor(accent)
                setSize(wide, 1)
            }
        }
        if (fontSize != style.size.toFloat()) {
            fontSize = style.size.toFloat()
            applySize()
        }
    }

    /** Follows the page: [pixels] view pixels per page point and the box turned by [turn] degrees. */
    fun follow(pixels: Float, turn: Float) {
        degrees = turn
        if (pixels != scale) {
            scale = pixels
            applySize()
        }
    }

    private fun applySize() {
        RichSpans.setEm(text, em())
        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * scale)
    }

    /** Takes the keyboard's focus, once the field is on screen. */
    fun focusAndShowKeyboard() {
        post {
            requestFocus()
            context.getSystemService(InputMethodManager::class.java)?.showSoftInput(this, 0)
        }
    }

    /** Lets go of the keyboard when the box closes. */
    fun release() {
        onDrag = null
        onTyped = null
        onFormat = null
        onBigInsert = null
        context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                dragging = false
                downX = event.rawX
                downY = event.rawY
                lastX = downX
                lastY = downY
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (!dragging) tracking = false

            MotionEvent.ACTION_MOVE -> if (tracking) {
                if (!dragging) startDragIfMoved(event)
                if (dragging) {
                    dragBy(event.rawX - lastX, event.rawY - lastY)
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                tracking = false
                dragging = false
                if (wasDragging) return true
                if (event.actionMasked == MotionEvent.ACTION_UP && tickedBoxAt(event)) {
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    super.onTouchEvent(cancel)
                    cancel.recycle()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // A short tap on the tick box of a checklist line ticks it. Returns true when it did.
    private fun tickedBoxAt(event: MotionEvent): Boolean {
        val shown = layout ?: return false
        if (hypot(event.rawX - downX, event.rawY - downY) > slop ||
            event.eventTime - event.downTime >= longPress
        ) {
            return false
        }
        val x = event.x - totalPaddingLeft + scrollX
        val y = event.y - totalPaddingTop + scrollY
        val line = shown.getLineForVertical(y.roundToInt())
        val start = shown.getLineStart(line)
        val span = text.getSpans(start, start, ParaSpan::class.java).firstOrNull { text.getSpanStart(it) == start }
        if (span == null || span.kind != ParaKinds.CHECK) return false
        val reach = span.em * ParaSpan.gutterEnd(span.indent)
        if (x > reach) return false
        RichEdit.tick(text, text.substring(0, start).count { it == '\n' }, em())
        reportFormat()
        return true
    }

    // A finger that moves past the slop before a long press moves the box. What the text had started on the down,
    // a cursor move or a pending long press, is called off first.
    private fun startDragIfMoved(event: MotionEvent) {
        if (event.eventTime - event.downTime >= longPress || hasSelection()) {
            tracking = false
            return
        }
        if (hypot(event.rawX - downX, event.rawY - downY) <= slop) return
        dragging = true
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.onTouchEvent(cancel)
        cancel.recycle()
        parent?.requestDisallowInterceptTouchEvent(true)
        lastX = event.rawX
        lastY = event.rawY
    }

    // Screen pixels to the box's own axes, then to page-local units.
    private fun dragBy(dx: Float, dy: Float) {
        val turn = Math.toRadians(degrees.toDouble())
        val c = cos(turn).toFloat()
        val s = sin(turn).toFloat()
        val across = dx * c + dy * s
        val down = -dx * s + dy * c
        onDrag?.invoke(across / scale, down / scale)
    }

    private companion object {
        const val BIG_INSERT = 120
        const val SELECTION_ALPHA = 0x55
        const val CURSOR_DP = 2f
    }
}
