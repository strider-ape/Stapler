package com.stapler.app.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.stapler.app.data.ImageLoader
import com.stapler.app.data.PageItem
import com.stapler.app.data.PdfOptions
import com.stapler.app.pdf.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMB_EDGE = 720

/** Upright (EXIF-applied) thumbnails, shared by the grid and the final preview. */
private object Thumbnails {
    private val cache = object : LruCache<Uri, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: Uri, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cached(uri: Uri): ImageBitmap? = cache.get(uri)

    suspend fun load(resolver: ContentResolver, uri: Uri): ImageBitmap? = cache.get(uri) ?: withContext(Dispatchers.IO) {
        runCatching { ImageLoader.load(resolver, uri, THUMB_EDGE) }.getOrNull()?.asImageBitmap()?.also { cache.put(uri, it) }
    }
}

private sealed interface Thumb {
    data object Loading : Thumb
    data object Failed : Thumb
    data class Ready(val image: ImageBitmap) : Thumb
}

/**
 * Draws a page exactly as it will come out: page shape, white margins and image placement
 * follow [options], and rotating a page animates the turn.
 */
@Composable
fun PagePreview(page: PageItem, options: PdfOptions, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val thumb by produceState<Thumb>(Thumbnails.cached(page.uri)?.let { Thumb.Ready(it) } ?: Thumb.Loading, page.uri) {
        value = Thumbnails.load(resolver, page.uri)?.let { Thumb.Ready(it) } ?: Thumb.Failed
    }
    val angle by animateFloatAsState(page.rotation.toFloat(), tween(280), label = "rotation")

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val image = (thumb as? Thumb.Ready)?.image
        val quarterTurn = (page.rotation / 90) % 2 == 1
        val (w, h) = when {
            image == null -> 3 to 4
            quarterTurn -> image.height to image.width
            else -> image.width to image.height
        }
        val layout = PdfBuilder.layout(w, h, options)
        val pageAspect = layout.pageWidth / layout.pageHeight
        val boxAspect = if (constraints.hasBoundedHeight) maxWidth / maxHeight else pageAspect

        Box(
            Modifier
                .aspectRatio(pageAspect, matchHeightConstraintsFirst = pageAspect < boxAspect)
                .shadow(3.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val scale = constraints.maxWidth / layout.pageWidth
                val drawW = layout.drawWidth * scale
                val drawH = layout.drawHeight * scale
                // PDF y runs bottom-up; Compose runs top-down.
                val left = layout.x * scale
                val top = (layout.pageHeight - layout.y - layout.drawHeight) * scale
                with(density) {
                    Box(
                        Modifier
                            .offset(left.toDp(), top.toDp())
                            .size(drawW.toDp(), drawH.toDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (val t = thumb) {
                            is Thumb.Ready -> Image(
                                bitmap = t.image,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    // Before rotating, the image has its unrotated shape.
                                    .requiredSize(
                                        (if (quarterTurn) drawH else drawW).toDp(),
                                        (if (quarterTurn) drawW else drawH).toDp(),
                                    )
                                    .graphicsLayer { rotationZ = angle },
                            )
                            Thumb.Failed -> Icon(
                                Icons.Outlined.BrokenImage,
                                contentDescription = "Image could not be loaded",
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Thumb.Loading -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFECECEC)),
                            )
                        }
                    }
                }
            }
        }
    }
}
