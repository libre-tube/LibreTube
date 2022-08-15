package com.github.libretube.util

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.disk.DiskCache
import coil.load
import com.github.libretube.api.CronetHelper
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys

object ImageHelper {
    lateinit var imageLoader: ImageLoader

    /**
     * Initialize the image loader
     */
    fun initializeImageLoader(context: Context) {
        val maxImageCacheSize = PreferenceHelper.getInt(
            PreferenceKeys.MAX_IMAGE_CACHE,
            128
        )

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
}
