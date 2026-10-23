package com.stapler.app.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stapler.app.data.PageItem
import com.stapler.app.data.PdfOptions
import com.stapler.app.data.PdfSaver
import com.stapler.app.data.SavedPdf
import com.stapler.app.pdf.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { Home, Arrange, Finish, Done }

/** A page removed by the user, kept so the snackbar can put it back where it was. */
data class RemovedPage(val page: PageItem, val index: Int)

class PdfViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private var nextId = 0L

    val pages = mutableStateListOf<PageItem>()
    var screen by mutableStateOf(Screen.Home)
        private set
    var options by mutableStateOf(PdfOptions())
    var fileName by mutableStateOf(defaultName())
    var progress by mutableStateOf<Int?>(null)
        private set
    var saved by mutableStateOf<SavedPdf?>(null)
        private set
    var error by mutableStateOf<String?>(null)

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        // Sharing into the app after a finished PDF starts a new one.
        if (screen == Screen.Done) startOver()
        pages += uris.map { PageItem(nextId++, it) }
        if (screen == Screen.Home) screen = Screen.Arrange
    }

    fun replaceImage(pageId: Long, uri: Uri) {
        val i = pages.indexOfFirst { it.id == pageId }
        if (i >= 0) pages[i] = pages[i].copy(uri = uri, rotation = 0)
    }

    /** Rotation keeps counting past 360 so the animation always turns clockwise. */
    fun rotate(pageId: Long) {
        val i = pages.indexOfFirst { it.id == pageId }
        if (i >= 0) pages[i] = pages[i].copy(rotation = pages[i].rotation + 90)
    }

    fun move(from: Int, to: Int) {
        if (from !in pages.indices || to !in pages.indices || from == to) return
        pages.add(to, pages.removeAt(from))
    }

    fun remove(pageId: Long): RemovedPage? {
        val i = pages.indexOfFirst { it.id == pageId }
        if (i < 0) return null
        return RemovedPage(pages.removeAt(i), i)
    }

    fun restore(removed: RemovedPage) {
        pages.add(removed.index.coerceIn(0, pages.size), removed.page)
    }

    fun goTo(target: Screen) {
        if (target == Screen.Finish && pages.isEmpty()) return
        screen = target
    }

    /** Leaves the current document and starts over with defaults. */
    fun startOver() {
        pages.clear()
        saved = null
        fileName = defaultName()
        screen = Screen.Home
    }

    fun back(): Boolean {
        when (screen) {
            Screen.Home -> return false
            Screen.Arrange -> startOver()
            Screen.Finish -> screen = Screen.Arrange
            Screen.Done -> startOver()
        }
        return true
    }

    val cleanFileName: String
        get() {
            val base = fileName.trim().removeSuffix(".pdf").replace(INVALID_CHARS, "_").trim()
            return (base.ifEmpty { defaultName() }) + ".pdf"
        }

    val fileNameError: String?
        get() = when {
            fileName.isBlank() -> "Enter a name"
            INVALID_CHARS.containsMatchIn(fileName) -> "Can't use / \\ : * ? \" < > |"
            else -> null
        }

    /** Saves into Downloads (Android 10+), or into [target] when the user picked a location. */
    fun createPdf(target: Uri? = null) {
        if (progress != null || pages.isEmpty()) return
        val snapshot = pages.toList()
        val opts = options
        val name = cleanFileName
        progress = 0
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val write: (OutputStream) -> Long = { out ->
                        PdfBuilder.build(resolver, snapshot, opts, out) { done -> progress = done }
                    }
                    when {
                        target != null -> PdfSaver.saveToDocument(resolver, target, name, write)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> PdfSaver.saveToDownloads(resolver, name, write)
                        else -> error("Choose where to save the PDF")
                    }
                }
                saved = result
                screen = Screen.Done
            } catch (e: Throwable) {
                error = when (e) {
                    is OutOfMemoryError -> "Ran out of memory. Try a lower quality."
                    else -> e.message ?: "Something went wrong while creating the PDF."
                }
            } finally {
                progress = null
            }
        }
    }

    companion object {
        private val INVALID_CHARS = Regex("[\\\\/:*?\"<>|]")

        private fun defaultName(): String =
            "PDF_" + SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
    }
}
