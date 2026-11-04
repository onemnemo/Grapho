package com.onemnemo.grapho.ui.note

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreaker
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Layout
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import com.onemnemo.grapho.note.TextAlignment
import com.onemnemo.grapho.note.TextSettings
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
internal class TextBoxInput(context: Context, startText: String) : EditText(context) {
    /** Gets each step of a finger drag across the text, in the box's own page-local units. */
    var onDrag: ((dx: Float, dy: Float) -> Unit)? = null

    /** Gets the whole text after every change. */
    var onTyped: ((String) -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPress = ViewConfiguration.getLongPressTimeout()
    private var fontSize = 0f
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
        setText(startText)
        setSelection(text.length)
        addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    onTyped?.invoke(s?.toString().orEmpty())
                }
            },
        )
    }

    /** Draws the text with [style] in [ink], with the cursor and selection in [accent]. */
    fun applyLook(style: TextSettings, ink: Int, accent: Int) {
        val weight = when {
            style.bold && style.italic -> Typeface.BOLD_ITALIC
            style.bold -> Typeface.BOLD
            style.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        if (typeface?.style != weight) typeface = Typeface.create(Typeface.DEFAULT, weight)
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

    private fun applySize() = setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * scale)

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
            }
        }
        return super.onTouchEvent(event)
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
        const val SELECTION_ALPHA = 0x55
        const val CURSOR_DP = 2f
    }
}
