package com.github.libretube.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.disk.DiskCache
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.ImageResult
import com.github.libretube.api.CronetHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.extensions.toAndroidUriOrNull
import com.github.libretube.util.DataSaverMode
import java.nio.file.Path

object ImageHelper {
    lateinit var imageLoader: ImageLoader

    /**
     * Initialize the image loader
     */
    fun initializeImageLoader(context: Context) {
        val maxImageCacheSize = PreferenceHelper.getString(
            PreferenceKeys.MAX_IMAGE_CACHE,
            ""
        )

        imageLoader = ImageLoader.Builder(context)
            .callFactory(CronetHelper.callFactory)
            .apply {
                when (maxImageCacheSize) {
                    "" -> {
                        diskCachePolicy(CachePolicy.DISABLED)
                    }

                    else -> diskCache(
                        DiskCache.Builder()
                            .directory(context.cacheDir.resolve("coil"))
                            .maxSizeBytes(maxImageCacheSize.toInt() * 1024 * 1024L)
                            .build()
                    )
                }
            }
            .build()
    }

    /**
     * load an image from a url into an imageView
     */
    fun loadImage(url: String?, target: ImageView) {
        // only load the image if the data saver mode is disabled
        if (DataSaverMode.isEnabled(target.context) || url == null) return
        val urlToLoad = ProxyHelper.unwrapIfEnabled(url)
        target.load(urlToLoad, imageLoader)
    }

    fun downloadImage(context: Context, url: String, path: Path) {
        getAsync(context, url) { bitmap ->
            saveImage(context, bitmap, path.toAndroidUri())
        }
    }

    fun getAsync(context: Context, url: String?, onSuccess: (Bitmap) -> Unit) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .target { onSuccess(it.toBitmap()) }
            .build()

        imageLoader.enqueue(request)
    }

    suspend fun getImage(context: Context, url: String?): ImageResult {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        return imageLoader.execute(request)
    }

    fun getDownloadedImage(context: Context, path: Path): Bitmap? {
        return path.toAndroidUriOrNull()?.let { getImage(context, it) }
    }

    private fun saveImage(context: Context, bitmapImage: Bitmap, imagePath: Uri) {
        context.contentResolver.openOutputStream(imagePath)?.use {
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 25, it)
        }
    }

    private fun getImage(context: Context, imagePath: Uri): Bitmap? {
        return context.contentResolver.openInputStream(imagePath)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    /**
     * Get a squared bitmap with the same width and height from a bitmap
     * @param bitmap The bitmap to resize
     */
    fun getSquareBitmap(bitmap: Bitmap): Bitmap {
        val newSize = minOf(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(
            bitmap,
            (bitmap.width - newSize) / 2,
            (bitmap.height - newSize) / 2,
            newSize,
            newSize
        )
    }
}
