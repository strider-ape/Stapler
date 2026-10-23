package com.stapler.app.data

import android.net.Uri

/** One page of the PDF: a source image plus the extra rotation the user applied on top of EXIF. */
data class PageItem(
    val id: Long,
    val uri: Uri,
    val rotation: Int = 0,
)

/** Page sizes in PDF points (1/72 inch). FIT makes every page the shape of its image. */
enum class PageSize(val label: String, val widthPt: Float, val heightPt: Float) {
    A4("A4", 595.28f, 841.89f),
    LETTER("Letter", 612f, 792f),
    FIT("Fit image", 0f, 0f),
}

enum class Margin(val label: String, val points: Float) {
    NONE("None", 0f),
    SMALL("Small", 18f),
    MEDIUM("Medium", 36f),
    LARGE("Large", 54f),
}

/**
 * [maxPixels] caps the longest image edge; [jpegQuality] is the JPEG encoder quality.
 * HIGH is roughly 300 dpi on an A4 page, which is print quality.
 */
enum class Quality(val label: String, val maxPixels: Int, val jpegQuality: Int, val hint: String) {
    LOW("Low", 1240, 55, "Smallest file, fine for screens"),
    MEDIUM("Medium", 1754, 75, "Balanced size and clarity"),
    HIGH("High", 2480, 90, "Sharp, print ready"),
    ORIGINAL("Original", 4096, 95, "Largest file, full detail"),
}

data class PdfOptions(
    val pageSize: PageSize = PageSize.A4,
    val margin: Margin = Margin.SMALL,
    val quality: Quality = Quality.HIGH,
)
