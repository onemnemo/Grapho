package com.onemnemo.grapho.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.onemnemo.grapho.R
import java.io.File
import java.util.UUID

private const val TAG = "GraphoPdf"

/** The name a document provider shows for [uri], or a generic one when it gives none. */
fun displayName(context: Context, uri: Uri): String {
    val name = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        Log.w(TAG, "couldn't read the name of a picked document", e)
        null
    }
    return name?.takeIf { it.isNotBlank() } ?: context.getString(R.string.pdf_default_file_name)
}

/** A file name without its `.pdf` ending, for use as a title. */
fun titleOf(fileName: String): String =
    fileName.replace(Regex("\\.pdf$", RegexOption.IGNORE_CASE), "").ifBlank { fileName }

/** Copies the document at [uri] to a new file in the cache, which the caller deletes. Throws [StoreException]. */
fun copyUriToCache(context: Context, uri: Uri): File {
    val dir = File(context.cacheDir, "pdf-import")
    dir.mkdirs()
    val temp = File(dir, "${UUID.randomUUID()}.pdf")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw StoreException(R.string.store_open_file)
        input.use { i -> temp.outputStream().use { o -> i.copyTo(o) } }
    } catch (e: StoreException) {
        temp.delete()
        throw e
    } catch (e: Exception) {
        temp.delete()
        throw StoreException(R.string.store_read_file, e)
    }
    return temp
}
