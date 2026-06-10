package com.github.libretube.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.storage.StorageManager
import android.widget.ImageView
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.load
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.github.libretube.BuildConfig
import com.github.libretube.extensions.toAndroidUri
import com.github.libretube.util.DataSaverMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.file.Path

object ImageHelper {
    private lateinit var imageLoader: ImageLoader

    private val Context.coilFile get() = cacheDir.resolve("coil")
    private const val HTTP_SCHEME = "http"

    /**
     * Initialize the image loader
     */
    fun initializeImageLoader(context: Context) {
        val httpClient = OkHttpClient().newBuilder()

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            httpClient.addInterceptor(loggingInterceptor)
        }

        imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(httpClient.build())
                )
            }
            .apply {
                diskCachePolicy(CachePolicy.ENABLED)
                memoryCachePolicy(CachePolicy.ENABLED)

                val storageManager = context.getSystemService<StorageManager>()!!
                val availableCache = storageManager.getCacheQuotaBytes(
                    storageManager.getUuidForPath(context.coilFile)
                )
                val diskCache = DiskCache.Builder()
                    .directory(context.coilFile)
                    // only use a certain percentage of the available cache size for images
                    .maxSizeBytes(availableCache)
                    .build()
                diskCache(diskCache)
            }
            .build()
    }

    /**
     * Checks if the corresponding image for the given key (e.g. a url) is cached.
     */
    private fun isCached(key: String): Boolean {
        val cacheSnapshot = imageLoader.diskCache?.openSnapshot(key)
        val isCacheHit = cacheSnapshot?.data?.toFile()?.exists()
        cacheSnapshot?.close()

        return isCacheHit ?: false
    }

    /**
     * load an image from a url into an imageView
     */
    fun loadImage(url: String?, target: ImageView, whiteBackground: Boolean = false) {
        if (url.isNullOrEmpty()) return

        // clear image to avoid loading issues at fast scrolling
        target.setImageBitmap(null)

        val urlToLoad = ProxyHelper.rewriteUrlUsingProxyPreference(url)

        // only load online images if the data saver mode is disabled
        if (DataSaverMode.isEnabled(target.context)) {
            if (urlToLoad.startsWith(HTTP_SCHEME) && !isCached(urlToLoad)) return
        }

        target.load(urlToLoad) {
            listener(
                onSuccess = { _, _ ->
                    // set the background to white for transparent images
                    if (whiteBackground) target.setBackgroundColor(Color.WHITE)
                }
            )
        }
    }

    suspend fun downloadImage(context: Context, url: String, path: Path) {
        val bitmap = getImage(context, url) ?: return
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(path.toAndroidUri())?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 25, it)
            }
        }
    }

    suspend fun getImage(context: Context, url: String?): Bitmap? {
        return getImage(context, url?.toUri())
    }

    suspend fun getImage(context: Context, url: Uri?): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        return imageLoader.execute(request).image?.toBitmap()
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
