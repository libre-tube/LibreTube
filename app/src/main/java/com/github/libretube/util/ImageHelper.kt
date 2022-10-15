package com.github.libretube.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.load
import coil.request.ImageRequest
import com.github.libretube.api.CronetHelper
import com.github.libretube.constants.PreferenceKeys
import okio.use
import java.io.File
import java.io.FileOutputStream

object ImageHelper {
    lateinit var imageLoader: ImageLoader

    /**
     * Initialize the image loader
     */
    fun initializeImageLoader(context: Context) {
        val maxImageCacheSize = PreferenceHelper.getString(
            PreferenceKeys.MAX_IMAGE_CACHE,
            "128"
        ).toInt()

        val diskCache = DiskCache.Builder()
            .directory(context.filesDir.resolve("coil"))
            .maxSizeBytes(maxImageCacheSize * 1024 * 1024L)
            .build()

        imageLoader = ImageLoader.Builder(context)
            .callFactory(CronetHelper.callFactory)
            .diskCache(diskCache)
            .build()
    }

    /**
     * load an image from a url into an imageView
     */
    fun loadImage(url: String?, target: ImageView) {
        // only load the image if the data saver mode is disabled
        val dataSaverModeEnabled = PreferenceHelper.getBoolean(
            PreferenceKeys.DATA_SAVER_MODE,
            false
        )
        if (!dataSaverModeEnabled) target.load(url, imageLoader)
    }

    fun downloadImage(context: Context, url: String, fileName: String) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .target { result ->
                val bitmap = (result as BitmapDrawable).bitmap
                saveImage(
                    context,
                    bitmap,
                    Uri.fromFile(
                        File(
                            DownloadHelper.getThumbnailDir(context),
                            fileName
                        )
                    )
                )
            }
            .build()

        imageLoader.enqueue(request)
    }

    fun getDownloadedImage(context: Context, fileName: String): Bitmap? {
        return getImage(
            context,
            Uri.fromFile(
                File(
                    DownloadHelper.getThumbnailDir(context),
                    fileName
                )
            )
        )
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
}
