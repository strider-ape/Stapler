package com.stapler.app.pdf

import java.io.OutputStream

/**
 * A minimal PDF 1.4 writer that embeds each page's image as a JPEG stream (DCTDecode).
 *
 * Android's built-in PdfDocument stores bitmaps losslessly, which makes photo PDFs huge and
 * ignores any quality setting. Writing the JPEG bytes directly keeps files small and makes
 * the quality option actually matter.
 *
 * Object layout: 1 = catalog, 2 = page tree, then three objects per page
 * (page, content stream, image), so every id is known before it is written.
 */
class JpegPdfWriter(output: OutputStream) : AutoCloseable {

    private val out = CountingStream(output)
    private val offsets = sortedMapOf<Int, Long>()
    private var pageCount = 0

    /** File size so far; after [close] it is the final size. */
    val bytesWritten: Long get() = out.count

    init {
        out.ascii("%PDF-1.4\n")
        // A binary comment so tools treat the file as binary, as the spec recommends.
        out.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
        startObject(1)
        out.ascii("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    }

    /**
     * Adds a page of [pageWidth] x [pageHeight] points with the JPEG drawn at
     * ([x], [y], [drawWidth], [drawHeight]) measured from the bottom-left corner.
     * Areas the image does not cover stay white.
     */
    fun addPage(
        jpeg: ByteArray,
        imageWidthPx: Int,
        imageHeightPx: Int,
        pageWidth: Float,
        pageHeight: Float,
        x: Float,
        y: Float,
        drawWidth: Float,
        drawHeight: Float,
    ) {
        val pageId = 3 + pageCount * 3
        val contentId = pageId + 1
        val imageId = pageId + 2

        startObject(pageId)
        out.ascii(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${n(pageWidth)} ${n(pageHeight)}] " +
                "/Resources << /XObject << /Im0 $imageId 0 R >> >> /Contents $contentId 0 R >>\nendobj\n",
        )

        val content = "q\n${n(drawWidth)} 0 0 ${n(drawHeight)} ${n(x)} ${n(y)} cm\n/Im0 Do\nQ\n".toByteArray(Charsets.US_ASCII)
        startObject(contentId)
        out.ascii("<< /Length ${content.size} >>\nstream\n")
        out.write(content)
        out.ascii("\nendstream\nendobj\n")

        startObject(imageId)
        out.ascii(
            "<< /Type /XObject /Subtype /Image /Width $imageWidthPx /Height $imageHeightPx " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n",
        )
        out.write(jpeg)
        out.ascii("\nendstream\nendobj\n")

        pageCount++
    }

    override fun close() {
        startObject(2)
        val kids = (0 until pageCount).joinToString(" ") { "${3 + it * 3} 0 R" }
        out.ascii("<< /Type /Pages /Kids [$kids] /Count $pageCount >>\nendobj\n")

        val xrefStart = out.count
        val size = offsets.lastKey() + 1
        out.ascii("xref\n0 $size\n0000000000 65535 f \n")
        for (id in 1 until size) {
            out.ascii(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets.getValue(id)))
        }
        out.ascii("trailer\n<< /Size $size /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")
        out.flush()
        out.close()
    }

    private fun startObject(id: Int) {
        offsets[id] = out.count
        out.ascii("$id 0 obj\n")
    }

    /** PDF numbers must not use locale separators or exponents. */
    private fun n(value: Float): String = String.format(java.util.Locale.US, "%.2f", value)

    private class CountingStream(private val inner: OutputStream) : OutputStream() {
        var count = 0L
            private set

        override fun write(b: Int) {
            inner.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            inner.write(b, off, len)
            count += len
        }

        override fun flush() = inner.flush()
        override fun close() = inner.close()

        fun ascii(text: String) = write(text.toByteArray(Charsets.US_ASCII))
    }
}
