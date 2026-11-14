package com.github.libretube.util

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import com.github.libretube.helpers.ImageHelper
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException

/**
 * [BitmapLoader] that skips loading remote artwork while the data saver mode is
 * enabled, so that e.g. the media notification does not download thumbnails.
 *
 * Mirrors the guard used by [ImageHelper.loadImage] and the audio player
 * fragment: online images are only fetched in data saver mode when they are
 * already cached; local (offline download) artwork is never affected.
 */
@UnstableApi
class DataSaverAwareBitmapLoader(
    context: Context,
) : BitmapLoader {
    private val appContext = context.applicationContext
    private val delegate = DataSourceBitmapLoader(appContext)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        delegate.decodeBitmap(data)

    override fun supportsMimeType(mimeType: String): Boolean =
        delegate.supportsMimeType(mimeType)

    override fun loadBitmap(uri: android.net.Uri): ListenableFuture<Bitmap> {
        val url = uri.toString()
        if (
            shouldSkipArtwork(
                url = url,
                dataSaverEnabled = DataSaverMode.isEnabled(appContext),
                cached = ImageHelper.isCached(url),
            )
        ) {
            // A failed artwork future makes the notification fall back to the
            // default artwork instead of downloading the image.
            return Futures.immediateFailedFuture(
                IOException("Skipped remote artwork '$url' in data saver mode"),
            )
        }
        return delegate.loadBitmap(uri)
    }

    companion object {
        // "http" prefix intentionally covers both http and https, matching
        // the scheme check in ImageHelper.loadImage.
        internal fun shouldSkipArtwork(url: String, dataSaverEnabled: Boolean, cached: Boolean): Boolean =
            dataSaverEnabled && url.startsWith("http") && !cached
    }
}
