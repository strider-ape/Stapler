package com.stapler.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/** Decodes images at a bounded size and applies EXIF orientation, so phone photos are never sideways. */
object ImageLoader {

    /** Pixel count [load] would produce for [maxEdge], read from the image header without decoding. */
    fun outputPixels(resolver: ContentResolver, uri: Uri, maxEdge: Int): Long? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val scale = minOf(1.0, maxEdge.toDouble() / maxOf(bounds.outWidth, bounds.outHeight))
        return (bounds.outWidth * scale).toLong() * (bounds.outHeight * scale).toLong()
    }

    /**
     * Decodes [uri] so its longest edge is at most [maxEdge] pixels, upright, with [extraRotation]
     * degrees (a multiple of 90) applied on top of the EXIF orientation.
     */
    fun load(resolver: ContentResolver, uri: Uri, maxEdge: Int, extraRotation: Int = 0): Bitmap? {
        // A bounds-only decode always returns null, so success is judged by outWidth/outHeight.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = resolver.openInputStream(uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Power-of-two subsampling keeps memory low; the exact resize happens below.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null

        val (flipX, exifDegrees) = exifTransform(resolver, uri)
        val degrees = (exifDegrees + extraRotation) % 360
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(decoded.width, decoded.height))
        if (degrees == 0 && !flipX && scale == 1f) return decoded

        val matrix = Matrix().apply {
            if (flipX) postScale(-1f, 1f)
            postRotate(degrees.toFloat())
            postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (result !== decoded) decoded.recycle()
        return result
    }

    /** Returns (mirror horizontally first, then rotate by degrees) for the image's EXIF orientation. */
    private fun exifTransform(resolver: ContentResolver, uri: Uri): Pair<Boolean, Int> {
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> false to 90
            ExifInterface.ORIENTATION_ROTATE_180 -> false to 180
            ExifInterface.ORIENTATION_ROTATE_270 -> false to 270
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> true to 0
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> true to 180
            ExifInterface.ORIENTATION_TRANSPOSE -> true to 270
            ExifInterface.ORIENTATION_TRANSVERSE -> true to 90
            else -> false to 0
        }
    }
}
