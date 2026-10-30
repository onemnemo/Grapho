package com.onemnemo.grapho.ui.note

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.NoteCanvasView
import com.onemnemo.grapho.ui.components.Notices
import com.onemnemo.grapho.ui.userMessage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Turns the lasso's selection into a PNG that is copied to the clipboard or shared. */
@Stable
class SelectionImager {
    internal var copyImage: () -> Unit = {}
    internal var exportImage: () -> Unit = {}

    fun copy() = copyImage()

    fun export() = exportImage()
}

/** Cache folder the pictures are written to, matching the selection path in `file_paths.xml`. */
private const val PICTURE_DIR = "selection"

/** [view] is read when the picture is made, so it is the canvas the selection is on now. */
@Composable
fun rememberSelectionImager(view: () -> NoteCanvasView?, notices: Notices): SelectionImager {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imager = remember { SelectionImager() }
    val copied = stringResource(R.string.lasso_copied_as_image)
    val chooser = stringResource(R.string.lasso_export_selection)

    // Draws on the UI thread, where the scene is read; the PNG is encoded and written off it.
    fun picture(name: String, then: (Context, Uri, File) -> Unit) {
        val bitmap = view()?.renderSelection()
        if (bitmap == null) {
            notices.show(context.getString(R.string.lasso_image_failed), isError = true)
            return
        }
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { writePng(context, bitmap, name) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                then(context, uri, file)
            } catch (e: ActivityNotFoundException) {
                notices.show(context.getString(R.string.note_share_failed), isError = true)
            } catch (e: Exception) {
                notices.show(e.userMessage(context, R.string.lasso_image_failed), isError = true)
            }
        }
    }

    imager.copyImage = {
        picture("copy.png") { c, uri, file ->
            val clipboard = c.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newUri(c.contentResolver, file.name, uri))
            // Android 13 and later confirm a copy themselves.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) notices.show(copied)
        }
    }
    imager.exportImage = {
        picture("selection.png") { c, uri, file ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(file.name, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            c.startActivity(Intent.createChooser(send, chooser))
        }
    }
    return imager
}

/** Writes [bitmap] as a PNG named [name] in the pictures folder of the cache, then lets the bitmap go. */
private fun writePng(context: Context, bitmap: Bitmap, name: String): File {
    val dir = File(context.cacheDir, PICTURE_DIR).apply { mkdirs() }
    val file = File(dir, name)
    try {
        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out) }
    } finally {
        bitmap.recycle()
    }
    return file
}

/** PNG ignores the quality; the call wants a number. */
private const val PNG_QUALITY = 100
