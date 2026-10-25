package com.onemnemo.grapho.ui.note

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ImageImport
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Brings a picture into the note from the photo picker, the camera or the clipboard, and places it on the page in
 * view. [busy] is true while the picture is being prepared and added.
 */
@Stable
class ImageInserter {
    var busy by mutableStateOf(false)
        internal set

    internal var launchPhoto: () -> Unit = {}
    internal var launchCamera: () -> Unit = {}
    internal var pasteClipboard: () -> Unit = {}
    internal var clipboardHasImage: () -> Boolean = { false }

    fun pickPhoto() {
        if (!busy) launchPhoto()
    }

    fun takePhoto() {
        if (!busy) launchCamera()
    }

    fun pasteImage() {
        if (!busy) pasteClipboard()
    }

    /** Whether the system clipboard holds a picture that can be pasted. */
    fun canPaste(): Boolean = clipboardHasImage()
}

/** [view] is read when a picture is ready, since the canvas may have been made again by then. */
@Composable
fun rememberImageInserter(
    session: NoteSession,
    view: () -> NoteCanvasView?,
    notices: Notices,
    onEdited: () -> Unit,
): ImageInserter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inserter = remember { ImageInserter() }
    var photoFile by remember { mutableStateOf<File?>(null) }

    // [temporary] is deleted once the picture has been read.
    fun insert(uri: Uri, temporary: File?) {
        inserter.busy = true
        scope.launch {
            try {
                val image = withContext(Dispatchers.IO) { ImageImport.prepare(context.contentResolver, uri) }
                val placement = view()?.imagePlacement(image.width, image.height)
                if (placement != null) {
                    val ids = session.withNotebook {
                        it.insertImage(
                            placement.page,
                            image.bytes,
                            placement.x,
                            placement.y,
                            placement.width,
                            placement.height,
                        )
                    }
                    if (ids.isEmpty()) {
                        notices.show(context.getString(R.string.note_insert_image_failed), isError = true)
                    } else {
                        view()?.imageInserted(placement.page, ids)
                        onEdited()
                    }
                }
            } catch (e: Exception) {
                notices.show(e.userMessage(context, R.string.note_insert_image_failed), isError = true)
            } finally {
                inserter.busy = false
                temporary?.delete()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) insert(uri, null)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved: Boolean ->
        val file = photoFile
        photoFile = null
        if (file != null) {
            if (saved) insert(Uri.fromFile(file), file) else file.delete()
        }
    }

    inserter.launchPhoto = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    inserter.launchCamera = {
        val file = File(File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }, "photo-${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        photoFile = file
        try {
            camera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            photoFile = null
            notices.show(context.getString(R.string.note_no_camera), isError = true)
        }
    }
    inserter.clipboardHasImage = { clipboardHoldsImage(context) }
    inserter.pasteClipboard = {
        val uri = clipboardImageUri(context)
        if (uri != null) {
            insert(uri, null)
        } else {
            notices.show(context.getString(R.string.note_insert_image_failed), isError = true)
        }
    }
    return inserter
}

private const val CAMERA_DIR = "camera"
private const val IMAGE_MIME = "image/*"

/** Whether the clipboard says it holds a picture. Does not read the clip, which would tell the person it was read. */
private fun clipboardHoldsImage(context: Context): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    return clipboard.primaryClipDescription?.hasMimeType(IMAGE_MIME) == true
}

/** The address of the picture on the clipboard, or null if it holds none. */
private fun clipboardImageUri(context: Context): Uri? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    if (!clipboardHoldsImage(context)) return null
    val clip = clipboard.primaryClip ?: return null
    return if (clip.itemCount > 0) clip.getItemAt(0).uri else null
}
