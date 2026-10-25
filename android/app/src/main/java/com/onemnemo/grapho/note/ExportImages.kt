package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.util.Log
import java.io.Closeable

/**
 * Pictures for an export, decoded on the calling thread at up to [maxSide] pixels on the long side, which is as many
 * as the exported page has. Not thread safe. [close] recycles what was decoded and may be followed by more use.
 */
internal class ExportImages(private val notebook: Notebook, private val maxSide: Int) :
    ImageBitmaps,
    Closeable {
    private val decoded = HashMap<String, Bitmap?>()

    override fun bitmap(source: String): Bitmap? {
        if (decoded.containsKey(source)) return decoded[source]
        val bitmap = try {
            notebook.imageBytes(source)?.let { ImageDecoder.decode(it, maxSide) }
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "not enough memory to decode image $source for export", e)
            null
        }
        decoded[source] = bitmap
        return bitmap
    }

    override fun close() {
        for (bitmap in decoded.values) bitmap?.recycle()
        decoded.clear()
    }

    private companion object {
        const val TAG = "ExportImages"
    }
}
