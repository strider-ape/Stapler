package com.stapler.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/** Decodes images at a bounded size and applies EXIF orientation, so phone photos are never sideways. */
object ImageLoader {

    /** Width and height after EXIF and [extraRotation] are applied, without decoding pixels. */
    fun orientedSize(resolver: ContentResolver, uri: Uri, extraRotation: Int = 0): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        val turns = (exifRotation(resolver, uri) + extraRotation) % 180 != 0
        return if (turns) opts.outHeight to opts.outWidth else opts.outWidth to opts.outHeight
    }

    /**
     * Decodes [uri] so its longest edge is at most [maxEdge] pixels, upright, with [extraRotation]
     * degrees (a multiple of 90) applied on top of the EXIF orientation.
     */
    fun load(resolver: ContentResolver, uri: Uri, maxEdge: Int, extraRotation: Int = 0): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
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

    private fun exifRotation(resolver: ContentResolver, uri: Uri): Int = exifTransform(resolver, uri).second

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
