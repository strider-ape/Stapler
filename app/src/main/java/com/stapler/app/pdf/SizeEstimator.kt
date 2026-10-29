package com.stapler.app.pdf

import android.content.ContentResolver
import com.stapler.app.data.ImageLoader
import com.stapler.app.data.PageItem
import com.stapler.app.data.Quality
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Predicts the PDF's file size before it is created. Page size and margins don't change it,
 * because they only move the image on the page; only the image pixels and JPEG quality matter.
 */
object SizeEstimator {

    /** Pages encoded for real. Up to this many pages the estimate is exact. */
    private const val SAMPLES = 3

    /** Page, content and image object headers, plus the xref entries, for one page. */
    private const val PAGE_OVERHEAD = 600L

    /** Catalog, page tree, header and trailer. */
    private const val FILE_OVERHEAD = 400L

    /**
     * Encodes up to [SAMPLES] evenly spaced pages exactly as [PdfBuilder] would, then scales their
     * bytes-per-pixel to the remaining pages using each page's pixel count from its image header.
     */
    suspend fun estimate(resolver: ContentResolver, pages: List<PageItem>, quality: Quality): Long {
        if (pages.isEmpty()) return 0
        val sampled = if (pages.size <= SAMPLES) {
            pages.indices.toSet()
        } else {
            (0 until SAMPLES).map { it * (pages.size - 1) / (SAMPLES - 1) }.toSet()
        }

        var sampleBytes = 0L
        var samplePixels = 0L
        for (i in sampled) {
            currentCoroutineContext().ensureActive()
            val bitmap = ImageLoader.load(resolver, pages[i].uri, quality.maxPixels) ?: continue
            sampleBytes += PdfBuilder.toJpeg(bitmap, quality.jpegQuality).size
            samplePixels += bitmap.width.toLong() * bitmap.height
            bitmap.recycle()
        }

        var otherPixels = 0L
        for (i in pages.indices) {
            if (i in sampled) continue
            currentCoroutineContext().ensureActive()
            otherPixels += ImageLoader.outputPixels(resolver, pages[i].uri, quality.maxPixels) ?: 0
        }
        val otherBytes = if (samplePixels > 0) otherPixels * sampleBytes / samplePixels else 0
        return FILE_OVERHEAD + pages.size * PAGE_OVERHEAD + sampleBytes + otherBytes
    }
}
