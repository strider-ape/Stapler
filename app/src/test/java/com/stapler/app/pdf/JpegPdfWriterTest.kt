package com.stapler.app.pdf

import com.stapler.app.data.Margin
import com.stapler.app.data.PageSize
import com.stapler.app.data.PdfOptions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class JpegPdfWriterTest {

    private val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())

    @Test
    fun xrefOffsetsPointAtTheirObjects() {
        val out = ByteArrayOutputStream()
        val writer = JpegPdfWriter(out)
        writer.addPage(fakeJpeg, 10, 20, 595f, 842f, 18f, 18f, 559f, 806f)
        writer.addPage(fakeJpeg, 20, 10, 842f, 595f, 18f, 18f, 806f, 559f)
        writer.close()
        val bytes = out.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)

        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))
        assertEquals(bytes.size.toLong(), writer.bytesWritten)

        val startxref = Regex("startxref\n(\\d+)").find(text)!!.groupValues[1].toInt()
        assertTrue(text.startsWith("xref", startxref))

        val entries = Regex("(\\d{10}) 00000 n ").findAll(text).map { it.groupValues[1].toInt() }.toList()
        assertEquals(8, entries.size) // catalog, page tree, 3 objects x 2 pages
        entries.forEachIndexed { i, offset -> assertTrue("object ${i + 1}", text.startsWith("${i + 1} 0 obj", offset)) }

        assertTrue(text.contains("/Kids [3 0 R 6 0 R] /Count 2"))
        val jpegAt = text.indexOf("/DCTDecode /Length 7 >>\nstream\n") + "/DCTDecode /Length 7 >>\nstream\n".length
        assertArrayEquals(fakeJpeg, bytes.copyOfRange(jpegAt, jpegAt + fakeJpeg.size))
    }

    @Test
    fun a4LayoutCentresImageInsideMargins() {
        val l = PdfBuilder.layout(1000, 1000, PdfOptions(PageSize.A4, Margin.MEDIUM))
        assertEquals(595.28f, l.pageWidth, 0.01f)
        assertEquals(595.28f - 72f, l.drawWidth, 0.01f)
        assertEquals(l.drawWidth, l.drawHeight, 0.01f)
        assertEquals((841.89f - l.drawHeight) / 2, l.y, 0.01f)
    }

    @Test
    fun landscapeImageGetsLandscapePage() {
        val l = PdfBuilder.layout(4000, 3000, PdfOptions(PageSize.LETTER, Margin.NONE))
        assertEquals(792f, l.pageWidth, 0.01f)
        assertEquals(612f, l.pageHeight, 0.01f)
    }

    @Test
    fun fitPageWrapsImagePlusMargins() {
        val l = PdfBuilder.layout(300, 900, PdfOptions(PageSize.FIT, Margin.SMALL))
        assertEquals(l.drawWidth + 36f, l.pageWidth, 0.01f)
        assertEquals(l.drawHeight + 36f, l.pageHeight, 0.01f)
        assertEquals(1f / 3, l.drawWidth / l.drawHeight, 0.001f)
    }
}
