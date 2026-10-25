package com.onemnemo.grapho.note

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max

/** A picture ready to embed: PNG or JPEG bytes, [width] by [height] pixels with the camera's rotation applied. */
class PreparedImage(val bytes: ByteArray, val width: Int, val height: Int)

/**
 * Turns a picture the person chose into what the note keeps: at most [ImageSizing.MAX_STORED_SIDE] pixels on the
 * long side, upright, and encoded as PNG when it has transparency and as JPEG otherwise. Slow, so call it off the UI
 * thread.
 */
object ImageImport {
    private const val TAG = "ImageImport"
    private const val JPEG_QUALITY = 90
    private const val PNG_QUALITY = 100

    /** Throws [IOException] if the picture cannot be read or there is not enough memory for it. */
    fun prepare(resolver: ContentResolver, uri: Uri): PreparedImage {
        val orientation = orientationOf(resolver, uri)
        val decoded = decode(resolver, uri)
        try {
            val upright = upright(decoded, orientation)
            try {
                return PreparedImage(encode(upright), upright.width, upright.height)
            } finally {
                if (upright !== decoded) upright.recycle()
            }
        } finally {
            decoded.recycle()
        }
    }

    private fun decode(resolver: ContentResolver, uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(resolver, uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("That isn't a picture.")
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(max(bounds.outWidth, bounds.outHeight), ImageSizing.MAX_STORED_SIDE)
        }
        try {
            return open(resolver, uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("Couldn't read that picture.")
        } catch (e: OutOfMemoryError) {
            throw IOException("There isn't enough memory for that picture.", e)
        }
    }

    private fun open(resolver: ContentResolver, uri: Uri) =
        resolver.openInputStream(uri) ?: throw IOException("Couldn't open that picture.")

    private fun orientationOf(resolver: ContentResolver, uri: Uri): Int = try {
        open(resolver, uri).use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (e: IOException) {
        // Not every format carries this; the picture is then used as it is.
        Log.w(TAG, "couldn't read the orientation of $uri", e)
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun upright(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)

            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)

            else -> return bitmap
        }
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            throw IOException("There isn't enough memory for that picture.", e)
        }
    }

    private fun encode(bitmap: Bitmap): ByteArray {
        val png = bitmap.hasAlpha()
        val out = ByteArrayOutputStream()
        val format = if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        if (!bitmap.compress(format, if (png) PNG_QUALITY else JPEG_QUALITY, out)) {
            throw IOException("Couldn't prepare that picture.")
        }
        return out.toByteArray()
    }
}
