package com.onemnemo.grapho.ui.note

import android.content.Context
import com.onemnemo.grapho.R
import com.onemnemo.grapho.ui.components.Notices

/**
 * Deletes the pen at [index] at once and offers Undo, which puts it back where it was. Whether it is the
 * active pen is read from the tool's own settings, so a restored pen is the active one again exactly when
 * the tool still matches it. Each delete has its own toast, so a new one never cancels a pending Undo.
 */
internal fun deletePen(context: Context, store: PenStore, index: Int, notices: Notices) {
    val gone = store.removeAt(index) ?: return
    notices.show(
        text = context.getString(R.string.wheel_pen_deleted, gone.name),
        actionLabel = context.getString(R.string.note_undo),
        onAction = { store.restore(index, gone) },
    )
}
