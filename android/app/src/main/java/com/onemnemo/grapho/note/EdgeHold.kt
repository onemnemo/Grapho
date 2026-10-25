package com.onemnemo.grapho.note

import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/**
 * Keeps a swipe that brings in the system bars or the taskbar from drawing on the page. A touch that lands in the
 * band along the top or bottom edge of the window is held back until [EdgeGuard] knows what it is: a swipe is
 * dropped whole, and a stroke is handed to [deliver] from its first event, so nothing of it is lost. Only edges of
 * [view] that meet the window's edge are guarded, so with the bars showing nothing is held. UI thread only.
 */
internal class EdgeHold(private val view: View, slop: Float, private val deliver: (MotionEvent) -> Unit) {
    private val guard = EdgeGuard(slop)
    private val held = ArrayList<MotionEvent>()
    private var dropping = false
    private val timeout = Runnable { release() }
    private val location = IntArray(2)

    /** Returns true when [event] was held or dropped, false when the caller should handle it now. */
    fun filter(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            clear()
            updateBands()
            if (!guard.begin(event.x, event.y, event.eventTime)) return false
            // Moves must arrive one by one for the swipe to be told apart in time.
            view.requestUnbufferedDispatch(event)
            held.add(MotionEvent.obtain(event))
            view.postDelayed(timeout, EdgeGuard.DECIDE_MS)
            return true
        }
        if (dropping) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                dropping = false
            }
            return true
        }
        if (held.isEmpty()) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> when (guard.move(event.getX(0), event.getY(0), event.eventTime)) {
                EdgeGuard.Verdict.WAIT -> {
                    held.add(MotionEvent.obtain(event))
                    return true
                }

                EdgeGuard.Verdict.SWIPE -> {
                    clear()
                    dropping = true
                    return true
                }

                EdgeGuard.Verdict.DRAW -> release()
            }

            MotionEvent.ACTION_CANCEL -> {
                clear()
                return true
            }

            // A lift, or a second pointer, settles it as a stroke.
            else -> release()
        }
        return false
    }

    /** Forgets a held touch without handing it on, e.g. when the view is detached. */
    fun clear() {
        view.removeCallbacks(timeout)
        held.forEach { it.recycle() }
        held.clear()
        dropping = false
    }

    private fun release() {
        view.removeCallbacks(timeout)
        val events = held.toList()
        held.clear()
        events.forEach {
            deliver(it)
            it.recycle()
        }
    }

    // The bands cover the system gesture zones and the bars, hidden or not, plus a margin, never less than a minimum.
    private fun updateBands() {
        val insets = ViewCompat.getRootWindowInsets(view)
        if (insets == null || view.height <= 0) {
            guard.setBands(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
            return
        }
        val gestures = insets.getInsets(
            WindowInsetsCompat.Type.systemGestures() or WindowInsetsCompat.Type.mandatorySystemGestures(),
        )
        val zone = Insets.max(gestures, insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()))
        val density = view.resources.displayMetrics.density
        val least = MIN_BAND_DP * density
        val margin = MARGIN_DP * density
        view.getLocationInWindow(location)
        val atTop = location[1] <= 0
        val atBottom = location[1] + view.height >= view.rootView.height
        val top = if (atTop) max(zone.top.toFloat(), least) + margin else Float.NEGATIVE_INFINITY
        val bottom = if (atBottom) view.height - max(zone.bottom.toFloat(), least) - margin else Float.POSITIVE_INFINITY
        guard.setBands(top, bottom)
    }

    private companion object {
        const val MIN_BAND_DP = 24f
        const val MARGIN_DP = 8f
    }
}
