package com.github.libretube.repo

import android.util.Log
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.extensions.TAG
import com.github.libretube.helpers.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.IOException
import okio.source
import java.time.Duration
import kotlin.io.path.fileSize
import kotlin.math.min

/**
 * Download from RAW HTTP stream.
 */
class RawByteStreamDownloadProvider(val url: HttpUrl) : DownloadProvider {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMillis(DownloadHelper.DEFAULT_TIMEOUT.toLong()))
            .readTimeout(Duration.ofMillis(DownloadHelper.DEFAULT_TIMEOUT.toLong()))
            .retryOnConnectionFailure(true)
            .build()
    }

    override suspend fun downloadNextChunk(
        item: DownloadItem,
        sink: BufferedSink,
    ): DownloadProgressResult {
        val startByteOffset = item.path.fileSize()
        val source = startConnection(url, startByteOffset, item.downloadSize)
            ?: return DownloadProgressResult.Failed

        val sourceByte = source.byteStream().source()

        var totalRead = 0L
        var lastRead = 0L
        while (sourceByte.read(sink.buffer, DownloadHelper.DOWNLOAD_CHUNK_SIZE)
                .also { lastRead = it } != -1L
        ) {
            sink.emit()
            totalRead += lastRead
        }

        withContext(Dispatchers.IO) {
            sourceByte.close()
            source.close()
        }

        return if (startByteOffset + totalRead < item.downloadSize) {
            DownloadProgressResult.Progressed(totalRead)
        } else {
            DownloadProgressResult.DownloadComplete
        }
    }

    private suspend fun startConnection(
        url: HttpUrl,
        alreadyRead: Long,
        readLimit: Long?
    ): ResponseBody? {
        val limit = readLimit?.let {
            min(readLimit, alreadyRead + BYTES_PER_REQUEST)
        }?.toString().orEmpty()

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .header("Range", "bytes=$alreadyRead-$limit")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.newCall(request).execute()
                val body = response.body

                if (response.code == 403) {
                    val errorBody = body?.string().orEmpty()
                    Log.e(TAG(), "HTTP 403 while downloading: $errorBody")
                    body?.close()
                    return@withContext null
                }

                if (response.code !in 200..299 || body == null) {
                    body?.close()
                    return@withContext null
                }

                return@withContext body
            } catch (e: IOException) {
                Log.e(TAG(), "Connection failed: ${e.message}")
                return@withContext null
            }
        }
    }

    companion object {
        private const val BYTES_PER_REQUEST = 512 * 1024L
    }
}
