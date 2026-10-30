package com.onemnemo.grapho.ui.note

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ImageImport
import com.onemnemo.grapho.note.ImageSizing
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.note.SelectedFrame
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Swaps the selected picture for one from the photo picker, which takes the old one's place: as large as fits in its
 * frame, with its own proportions.
 */
@Stable
class ImageReplacer {
    internal var launch: () -> Unit = {}

    fun replace() = launch()
}

/** [view] is read when a picture is ready, since the canvas may have been made again by then. */
@Composable
fun rememberImageReplacer(
    session: NoteSession,
    view: () -> NoteCanvasView?,
    notices: Notices,
    onEdited: () -> Unit,
): ImageReplacer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val replacer = remember { ImageReplacer() }
    // The picture to replace is fixed when the picker opens, so a selection changed meanwhile is left alone.
    val target = remember { arrayOfNulls<SelectedFrame>(1) }

    fun swap(uri: Uri, old: SelectedFrame) {
        scope.launch {
            try {
                val image = withContext(Dispatchers.IO) { ImageImport.prepare(context.contentResolver, uri) }
                if (view()?.activeLayerAccepts() != true) return@launch
                val b = old.bounds
                val at = ImageSizing.fit(old.page, b.left, b.top, b.width(), b.height(), image.width, image.height)
                val ids = session.withNotebook {
                    it.insertImage(at.page, image.bytes, at.x, at.y, at.width, at.height)
                }
                if (ids.isEmpty()) {
                    notices.show(context.getString(R.string.note_insert_image_failed), isError = true)
                } else {
                    view()?.imageReplaced(old.page, longArrayOf(old.id.hi, old.id.lo), ids)
                    onEdited()
                }
            } catch (e: Exception) {
                notices.show(e.userMessage(context, R.string.note_insert_image_failed), isError = true)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        val old = target[0]
        target[0] = null
        if (uri != null && old != null) swap(uri, old)
    }
    replacer.launch = {
        target[0] = view()?.selectedImageFrame()
        if (target[0] != null) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
    return replacer
}
