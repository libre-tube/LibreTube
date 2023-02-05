package com.github.libretube.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.load
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.github.libretube.api.CronetHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.util.DataSaverMode
import java.io.File
import java.io.FileOutputStream
import okio.use

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
        if (!DataSaverMode.isEnabled(target.context)) target.load(url, imageLoader)
    }

    fun downloadImage(context: Context, url: String, path: String) {
        getAsync(context, url) { bitmap ->
            saveImage(context, bitmap, Uri.fromFile(File(path)))
        }
    }

    fun getAsync(context: Context, url: String?, onSuccess: (Bitmap) -> Unit) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .target { result ->
                val bitmap = (result as BitmapDrawable).bitmap
                onSuccess.invoke(bitmap)
            }
            .build()

        imageLoader.enqueue(request)
    }

    fun getDownloadedImage(context: Context, path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        return getImage(context, Uri.fromFile(file))
    }

    private fun saveImage(context: Context, bitmapImage: Bitmap, imagePath: Uri) {
        context.contentResolver.openFileDescriptor(imagePath, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { fos ->
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 25, fos)
            }
        }
    }

    private fun getImage(context: Context, imagePath: Uri): Bitmap? {
        context.contentResolver.openInputStream(imagePath)?.use {
            return BitmapFactory.decodeStream(it)
        }
        return null
    }

    /**
     * Get a squared bitmap with the same width and height from a bitmap
     * @param bitmap The bitmap to resize
     */
    fun getSquareBitmap(bitmap: Bitmap?): Bitmap? {
        bitmap ?: return null
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
