package com.onemnemo.grapho.ui.note

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.onemnemo.grapho.R
import com.onemnemo.grapho.note.ExportPlan
import com.onemnemo.grapho.note.Notebook
import com.onemnemo.grapho.note.PageImages
import com.onemnemo.grapho.ui.StoreException
import com.onemnemo.grapho.ui.asStoreException
import com.onemnemo.grapho.ui.copyFileToUri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Writes a note out as a PDF or as PNG images. Every function blocks, so call them on the notebook thread, where
 * nothing can close the note underneath them. Failures are [StoreException]s with a message fit to show.
 */
object NoteExports {
    /** Exports [pages] (zero-based, or null for all) as one PDF into the document at [uri]. */
    fun pdf(context: Context, notebook: Notebook, uri: Uri, backgrounds: Boolean, pages: List<Int>?) {
        inWorkDir(context) { dir ->
            val file = File(dir, "export.pdf")
            try {
                notebook.exportPdf(file, backgrounds, pages)
            } catch (e: IOException) {
                throw e.asStoreException(R.string.store_export_pdf)
            }
            copyFileToUri(context, file, uri)
        }
    }

    /** Exports one page as a PNG into the document at [uri]. */
    fun image(context: Context, notebook: Notebook, uri: Uri, backgrounds: Boolean, page: Int) {
        inWorkDir(context) { dir ->
            PageImages(notebook, dir, backgrounds).use { images ->
                val file = File(dir, "page.png")
                writeImage(images, page, file)
                copyFileToUri(context, file, uri)
            }
        }
    }

    /** Exports each of [pages] as `<title> p<N>.png` inside the folder at [tree]. */
    fun images(
        context: Context,
        notebook: Notebook,
        tree: Uri,
        backgrounds: Boolean,
        pages: List<Int>,
        title: String,
        untitled: String,
    ) {
        inWorkDir(context) { dir ->
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            PageImages(notebook, dir, backgrounds).use { images ->
                val file = File(dir, "page.png")
                for (page in pages) {
                    writeImage(images, page, file)
                    val target = createDocument(context, parent, ExportPlan.imageName(title, page, untitled))
                    copyFileToUri(context, file, target)
                }
            }
        }
    }

    private fun writeImage(images: PageImages, page: Int, file: File) {
        try {
            images.write(page, file)
        } catch (e: IOException) {
            throw e.asStoreException(R.string.store_export_images)
        }
    }

    private fun createDocument(context: Context, parent: Uri, name: String): Uri {
        try {
            return DocumentsContract.createDocument(context.contentResolver, parent, "image/png", name)
                ?: throw StoreException(R.string.store_write_folder)
        } catch (e: StoreException) {
            throw e
        } catch (e: Exception) {
            throw StoreException(R.string.store_write_folder, e)
        }
    }

    private fun inWorkDir(context: Context, block: (File) -> Unit) {
        val dir = File(context.cacheDir, "export/${UUID.randomUUID()}")
        if (!dir.mkdirs()) throw StoreException(R.string.store_prepare_export)
        try {
            block(dir)
        } catch (e: StoreException) {
            throw e
        } catch (e: IOException) {
            throw e.asStoreException(R.string.note_export_failed)
        } finally {
            dir.deleteRecursively()
        }
    }
}
