package com.onemnemo.grapho.ui.note

import com.onemnemo.grapho.note.PenActivity
import android.view.MotionEvent

/** A touch wider than this is a palm or the side of a hand, never a fingertip, in millimetres. */
internal const val TAB_MAX_CONTACT_MM = 12f

/** A touch this soon after the pen was near the screen is most likely a resting palm, in milliseconds. */
internal const val TAB_PEN_QUIET_MS = 400L

private const val MM_PER_INCH = 25.4f

/** Whether a touch on the wheel's tab is a deliberate finger tap. */
internal fun tabTouchWanted(contactMm: Float, canceled: Boolean, sincePenMs: Long): Boolean =
    !canceled && contactMm <= TAB_MAX_CONTACT_MM && sincePenMs >= TAB_PEN_QUIET_MS

/** Remembers how the touch that is on the tab landed, from the raw events. It never takes a touch. */
internal class TabContact(private val xdpi: Float) {
    private var unwanted = false

    /** Feed it each raw event; true while the touch so far is one the tab should act on. */
    fun see(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val mm = e.getTouchMajor(0) / xdpi * MM_PER_INCH
                val stylus = e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
                unwanted = !stylus && !tabTouchWanted(mm, false, e.eventTime - PenActivity.lastMs)
            }
            MotionEvent.ACTION_UP -> if (e.flags and MotionEvent.FLAG_CANCELED != 0) unwanted = true
        }
        return !unwanted
    }

    val wanted: Boolean get() = !unwanted
}
