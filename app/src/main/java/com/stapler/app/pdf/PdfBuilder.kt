package com.stapler.app.pdf

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.stapler.app.data.ImageLoader
import com.stapler.app.data.PageItem
import com.stapler.app.data.PageSize
import com.stapler.app.data.PdfOptions
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/** Where an image sits on its page, in PDF points. */
data class PageLayout(
    val pageWidth: Float,
    val pageHeight: Float,
    val x: Float,
    val y: Float,
    val drawWidth: Float,
    val drawHeight: Float,
)

object PdfBuilder {

    /**
     * Fits an image of [imageWidth] x [imageHeight] onto a page. A4 and Letter turn landscape
     * for landscape images; FIT sizes the page around the image so there is no extra white space.
     */
    fun layout(imageWidth: Int, imageHeight: Int, options: PdfOptions): PageLayout {
        val margin = options.margin.points
        val landscape = imageWidth > imageHeight
        val aspect = imageWidth.toFloat() / imageHeight

        if (options.pageSize == PageSize.FIT) {
            // Same footprint as an A4 page in the image's own orientation.
            val (boxW, boxH) = if (landscape) 841.89f to 595.28f else 595.28f to 841.89f
            val scale = minOf(boxW / imageWidth, boxH / imageHeight)
            val w = imageWidth * scale
            val h = imageHeight * scale
            return PageLayout(w + 2 * margin, h + 2 * margin, margin, margin, w, h)
        }

        val (shortSide, longSide) = with(options.pageSize) { widthPt to heightPt }
        val pageW = if (landscape) longSide else shortSide
        val pageH = if (landscape) shortSide else longSide
        val boxW = pageW - 2 * margin
        val boxH = pageH - 2 * margin
        val (w, h) = if (boxW / boxH > aspect) boxH * aspect to boxH else boxW to boxW / aspect
        return PageLayout(pageW, pageH, (pageW - w) / 2, (pageH - h) / 2, w, h)
    }

    /**
     * Writes the PDF to [output], calling [onProgress] with the number of pages finished.
     * Returns the file size in bytes.
     */
    fun build(
        resolver: ContentResolver,
        pages: List<PageItem>,
        options: PdfOptions,
        output: OutputStream,
        onProgress: (Int) -> Unit,
    ): Long {
        val writer = JpegPdfWriter(output)
        writer.use {
            pages.forEachIndexed { index, page ->
                val bitmap = ImageLoader.load(resolver, page.uri, options.quality.maxPixels, page.rotation)
                    ?: throw IOException("Could not read image ${index + 1}")
                val jpeg = toJpeg(bitmap, options.quality.jpegQuality)
                val l = layout(bitmap.width, bitmap.height, options)
                writer.addPage(jpeg, bitmap.width, bitmap.height, l.pageWidth, l.pageHeight, l.x, l.y, l.drawWidth, l.drawHeight)
                bitmap.recycle()
                onProgress(index + 1)
            }
        }
        return writer.bytesWritten
    }

    /** JPEG has no transparency, so transparent PNGs are flattened onto white first. */
    internal fun toJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val opaque = if (bitmap.hasAlpha()) {
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(bitmap, 0f, 0f, null)
                }
            }
        } else {
            bitmap
        }
        val bytes = ByteArrayOutputStream().use {
            opaque.compress(Bitmap.CompressFormat.JPEG, quality, it)
            it.toByteArray()
        }
        if (opaque !== bitmap) opaque.recycle()
        return bytes
    }
}
