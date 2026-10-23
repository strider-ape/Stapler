package com.stapler.app.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.OutputStream

data class SavedPdf(val uri: Uri, val name: String, val sizeBytes: Long, val location: String)

/** Opens destinations for a new PDF: the Downloads folder on Android 10+, or a file the user picked. */
object PdfSaver {

    const val FOLDER = "Stapler"

    /** Android 10+ can write to Downloads through MediaStore without any storage permission. */
    val canSaveToDownloads: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Writes into Download/Stapler. The entry stays hidden until [write] finishes, so a failed
     * export never leaves a broken file behind. MediaStore adds " (1)" if the name is taken.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveToDownloads(resolver: ContentResolver, fileName: String, write: (OutputStream) -> Long): SavedPdf {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the file in Downloads")
        try {
            val size = resolver.openOutputStream(uri)?.use(write) ?: error("Could not open the file for writing")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            val finalName = displayName(resolver, uri) ?: fileName
            return SavedPdf(uri, finalName, size, "Downloads/$FOLDER")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /** Writes into a document the user created with the system "Save as" picker. */
    fun saveToDocument(resolver: ContentResolver, uri: Uri, fallbackName: String, write: (OutputStream) -> Long): SavedPdf {
        val size = resolver.openOutputStream(uri, "wt")?.use(write) ?: error("Could not open the file for writing")
        return SavedPdf(uri, displayName(resolver, uri) ?: fallbackName, size, "the folder you chose")
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}
