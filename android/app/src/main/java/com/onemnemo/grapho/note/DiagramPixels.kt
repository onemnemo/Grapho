package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException

/** The image the lasso holds: where it is, its id and the id of the embedded source it shows. */
class SelectedImage(val page: Int, val id: StrokeId, val source: String) {
    val idPair: LongArray get() = longArrayOf(id.hi, id.lo)
}

/** A picture's pixels as `0xAARRGGBB` ints, straight (not premultiplied), top row first. */
class DecodedPixels(val width: Int, val height: Int, val argb: IntArray)

/** Reads an embedded picture at full size for the engine to clean up. */
object DiagramPixels {
    /**
     * Decodes [bytes] without shrinking, because the engine needs the source's own pixels. Slow and large (a
     * 4096 pixel square is 64 MB of ints), so call it off the UI thread. Throws [IOException] when the bytes are not
     * a picture or there is not enough memory, never lets the memory error escape.
     */
    fun decode(bytes: ByteArray): DecodedPixels {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = false
        }
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: throw IOException("Couldn't read that picture.")
            val argb = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return DecodedPixels(bitmap.width, bitmap.height, argb)
        } catch (e: OutOfMemoryError) {
            throw IOException("There isn't enough memory for that picture.", e)
        } finally {
            bitmap?.recycle()
        }
    }
}
