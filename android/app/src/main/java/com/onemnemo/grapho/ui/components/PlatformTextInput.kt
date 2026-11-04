package com.onemnemo.grapho.ui.components

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.onemnemo.grapho.R

/**
 * A single text input backed by a platform EditText. Samsung's S Pen to text and Android's stylus
 * handwriting only work on real EditTexts, so every text field in the app goes through this one so
 * people can write into it with the pen. The frame, placeholder and icons stay in Compose around it.
 * [autoFocus] focuses the field and opens the keyboard once it is shown. [onEscape] runs on the Escape
 * key and then leaves the field, and [blurOnAction] leaves the field after the keyboard action. [filter]
 * cleans what was typed or written before it reaches [onValueChange], such as a length limit.
 */
@Composable
fun PlatformTextInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textStyle: TextStyle,
    cursorColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitals: Boolean = false,
    autoFocus: Boolean = false,
    blurOnAction: Boolean = false,
    description: String? = null,
    filter: ((String) -> String)? = null,
    onImeAction: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onEscape: (() -> Unit)? = null,
) {
    val latestValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val action by rememberUpdatedState(onImeAction)
    val focusChange by rememberUpdatedState(onFocusChange)
    val escape by rememberUpdatedState(onEscape)
    val blur by rememberUpdatedState(blurOnAction)
    val clean by rememberUpdatedState(filter)
    val semantics = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    AndroidView(
        modifier = modifier.then(semantics),
        factory = { context ->
            InputView(context).apply {
                setText(value.text)
                select(value.selection)
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable?) {
                            if (pushing) return
                            val raw = s?.toString().orEmpty()
                            val text = clean?.invoke(raw) ?: raw
                            if (text != raw) {
                                val caret = (selectionEnd - (raw.length - text.length)).coerceIn(0, text.length)
                                pushing = true
                                setText(text)
                                setSelection(caret)
                                pushing = false
                            }
                            if (text == latestValue.text) return
                            change(
                                TextFieldValue(
                                    text,
                                    TextRange(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0)),
                                ),
                            )
                        }
                    },
                )
                setOnEditorActionListener { _, id, event ->
                    val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                    if (id == EditorInfo.IME_NULL && !enter) return@setOnEditorActionListener false
                    action?.invoke()
                    if (blur) leave()
                    true
                }
                setOnKeyListener { _, code, event ->
                    val handler = escape
                    if (code != KeyEvent.KEYCODE_ESCAPE || handler == null) return@setOnKeyListener false
                    if (event.action == KeyEvent.ACTION_UP) {
                        handler()
                        leave()
                    }
                    true
                }
                setOnFocusChangeListener { _, focused -> focusChange?.invoke(focused) }
                if (autoFocus) post { focusAndShowKeyboard() }
            }
        },
        update = { view ->
            view.style(textStyle, cursorColor)
            view.configure(enabled, readOnly, singleLine, imeAction, keyboardType, capitals)
            if (view.text.toString() != value.text) {
                view.pushing = true
                view.setText(value.text)
                view.select(value.selection)
                view.pushing = false
            }
        },
    )
}

/** [PlatformTextInput] for a plain string. The caret stays where it was, or goes to the end. */
@Composable
fun PlatformTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    cursorColor: Color,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitals: Boolean = false,
    autoFocus: Boolean = false,
    blurOnAction: Boolean = true,
    description: String? = null,
    filter: ((String) -> String)? = null,
    onImeAction: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    PlatformTextInput(
        value = TextFieldValue(value, TextRange(value.length)),
        onValueChange = { onValueChange(it.text) },
        textStyle = textStyle,
        cursorColor = cursorColor,
        modifier = modifier,
        singleLine = singleLine,
        imeAction = imeAction,
        keyboardType = keyboardType,
        capitals = capitals,
        autoFocus = autoFocus,
        blurOnAction = blurOnAction,
        description = description,
        filter = filter,
        onImeAction = onImeAction,
        onFocusChange = onFocusChange,
    )
}

private class InputView(context: Context) : EditText(context) {
    var pushing = false
    private var styled: Pair<TextStyle, Color>? = null
    private var config: List<Any>? = null

    init {
        background = null
        setPadding(0, 0, 0, 0)
        minHeight = 0
        minimumHeight = 0
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
    }

    fun select(range: TextRange) {
        val length = text.length
        setSelection(range.start.coerceIn(0, length), range.end.coerceIn(0, length))
    }

    fun style(textStyle: TextStyle, cursor: Color) {
        if (styled == textStyle to cursor) return
        styled = textStyle to cursor
        val size = if (textStyle.fontSize.isSp) textStyle.fontSize.value else DEFAULT_SIZE_SP
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        typeface = typefaceFor(context, textStyle)
        val spacing = textStyle.letterSpacing
        letterSpacing = when {
            spacing.isEm -> spacing.value
            spacing.isSp -> spacing.value / size
            else -> 0f
        }
        val color = if (textStyle.color == Color.Unspecified) cursor else textStyle.color
        setTextColor(color.toArgb())
        highlightColor = cursor.copy(alpha = SELECTION_ALPHA).toArgb()
        val caret = GradientDrawable()
        caret.setColor(cursor.toArgb())
        caret.setSize((CURSOR_WIDTH_DP * resources.displayMetrics.density).toInt().coerceAtLeast(1), 0)
        setTextCursorDrawable(caret)
    }

    fun configure(
        enabled: Boolean,
        readOnly: Boolean,
        singleLine: Boolean,
        imeAction: ImeAction,
        keyboardType: KeyboardType,
        capitals: Boolean,
    ) {
        val next = listOf(enabled, readOnly, singleLine, imeAction, keyboardType, capitals)
        if (config == next) return
        config = next
        isEnabled = enabled
        var type = when (keyboardType) {
            KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
            KeyboardType.Ascii -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            else -> InputType.TYPE_CLASS_TEXT
        }
        if (capitals) type = type or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        if (!singleLine && type and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT) {
            type = type or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        inputType = type
        setSingleLine(singleLine)
        if (singleLine) maxLines = 1
        imeOptions = imeOptionFor(imeAction) or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        if (readOnly) {
            keyListener = null
            setTextIsSelectable(true)
        }
        isFocusable = enabled
        isFocusableInTouchMode = enabled
    }

    fun focusAndShowKeyboard() {
        if (!isAttachedToWindow || !isEnabled) return
        requestFocus()
        keyboard().showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        // A dialog window may not have focus yet on the first frame, so ask once more a moment later.
        postDelayed({
            if (hasFocus()) keyboard().showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }, KEYBOARD_RETRY_MS)
    }

    fun leave() {
        keyboard().hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    private fun keyboard() = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

private fun imeOptionFor(action: ImeAction): Int = when (action) {
    ImeAction.Done -> EditorInfo.IME_ACTION_DONE
    ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
    ImeAction.Go -> EditorInfo.IME_ACTION_GO
    ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
    ImeAction.Send -> EditorInfo.IME_ACTION_SEND
    else -> EditorInfo.IME_ACTION_UNSPECIFIED
}

private fun typefaceFor(context: Context, style: TextStyle): Typeface? {
    val medium = (style.fontWeight ?: FontWeight.Normal) >= FontWeight.Medium
    return when (style.fontFamily) {
        FontFamily.Serif -> Typeface.SERIF
        FontFamily.Monospace -> Typeface.MONOSPACE
        else -> ResourcesCompat.getFont(context, if (medium) R.font.geist_500 else R.font.geist_400)
    }
}

private const val DEFAULT_SIZE_SP = 16f
private const val SELECTION_ALPHA = 0.3f
private const val CURSOR_WIDTH_DP = 2f
private const val KEYBOARD_RETRY_MS = 250L
