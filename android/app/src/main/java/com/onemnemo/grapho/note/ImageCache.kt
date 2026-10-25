package com.onemnemo.grapho.note

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.math.max

/** The decoded pictures of a note's images, by source id. */
interface ImageBitmaps {
    /** The picture for [source], or null while it is not available. Safe to call from any thread. */
    fun bitmap(source: String): Bitmap?
}

/** Decodes encoded PNG or JPEG bytes, shrunk so the longest side is at most a given size. */
internal object ImageDecoder {
    /** Returns null if the bytes are not a picture. */
    fun decode(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(max(bounds.outWidth, bounds.outHeight), maxSide)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

/**
 * Pictures decoded off the UI thread, for the screen. [bitmap] returns what is ready and, for a picture that is not,
 * starts decoding it once; [onReady] is called on the decoding thread whenever a new picture can be drawn, so the
 * canvas draws again. Bitmaps are only dropped from the cache, never recycled, because a render may still be
 * drawing one.
 */
internal class ImageCache(private val notebook: Notebook, private val onReady: () -> Unit) : ImageBitmaps {
    private val lock = Any()
    private val ready = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
    private var readyBytes = 0L
    private val wanted = HashSet<String>()
    private val failed = HashSet<String>()
    private var executor: ExecutorService? = null

    override fun bitmap(source: String): Bitmap? {
        synchronized(lock) {
            ready[source]?.let { return it }
            if (source in failed || !wanted.add(source)) return null
            // A view can be detached and attached again, so start a fresh thread when needed.
            val ex = executor?.takeIf { !it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executor = it }
            try {
                ex.execute { decode(source) }
            } catch (_: RejectedExecutionException) {
                // Shut down in between; the next request starts a new thread.
                wanted.remove(source)
            }
        }
        return null
    }

    /** Stops decoding and forgets every picture. Asking for one again starts over. */
    fun shutdown() {
        synchronized(lock) {
            executor?.shutdownNow()
            executor = null
            wanted.clear()
            ready.clear()
            readyBytes = 0L
        }
    }

    private fun decode(source: String) {
        val bitmap = try {
            notebook.imageBytes(source)?.let { ImageDecoder.decode(it, ImageSizing.MAX_SCREEN_SIDE) }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "the note closed before image $source was decoded", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "not enough memory to decode image $source", e)
            null
        }
        synchronized(lock) {
            wanted.remove(source)
            if (bitmap == null) {
                failed.add(source)
            } else {
                ready[source] = bitmap
                readyBytes += bitmap.allocationByteCount
                trim()
            }
        }
        if (bitmap != null) onReady()
    }

    /** Drops the pictures used least recently once there are more than [BUDGET_BYTES], keeping the newest. */
    private fun trim() {
        val it = ready.entries.iterator()
        while (readyBytes > BUDGET_BYTES && ready.size > 1 && it.hasNext()) {
            val entry = it.next()
            readyBytes -= entry.value.allocationByteCount
            it.remove()
        }
    }

    private companion object {
        const val TAG = "ImageCache"
        const val BUDGET_BYTES = 192L * 1024 * 1024
    }
}
