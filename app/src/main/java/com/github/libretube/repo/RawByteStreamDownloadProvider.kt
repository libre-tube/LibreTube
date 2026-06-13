package com.github.libretube.repo

import android.util.Log
import com.github.libretube.db.obj.DownloadItem
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
        val source =
            startConnection(url, startByteOffset, item.downloadSize) ?: return DownloadProgressResult.Failed

        val sourceByte = source.byteStream().source()

        var totalRead = 0L
        var lastRead = 0L
        // Check if downloading is still active and read next bytes.
        while (sourceByte
                .read(sink.buffer, DownloadHelper.DOWNLOAD_CHUNK_SIZE)
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
            // generate a random byte distance to make it more difficult to fingerprint
            val nextBytesToReadSize = (BYTES_PER_REQUEST_MIN..BYTES_PER_REQUEST_MAX).random()
            min(readLimit, alreadyRead + nextBytesToReadSize)
        }?.toString().orEmpty()

        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .header("Range", "bytes=$alreadyRead-$limit")
            .build()

        return withContext(Dispatchers.IO) {
            // Retry connecting to server for n times.
            try {
                val call = httpClient.newCall(request)
                val response = call.execute()

                if (response.code == 403) {
                    response.close()
                    return@withContext null
                } else if (response.code !in 200..299) {
                    response.close()
                    return@withContext null // TODO: print response.message
                }

                return@withContext response.body
            } catch (e: IOException) {
                Log.e(this::javaClass.name, e.printStackTrace().toString())
                // TODO: forward error message

                return@withContext null
            }
        }
    }

    companion object {
        // any values that are not in that range are strictly rate limited by YT or are very slow due
        // to the amount of requests that's being made
        private const val BYTES_PER_REQUEST_MIN = 500_000L

        private const val BYTES_PER_REQUEST_MAX = 3_000_000L
    }
}