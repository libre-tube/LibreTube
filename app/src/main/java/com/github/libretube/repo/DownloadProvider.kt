package com.github.libretube.repo

import com.github.libretube.db.obj.DownloadItem
import okio.BufferedSink



sealed class DownloadProgressResult {
    /**
     * Failed to download an additional chunk of data.
     */
    object Failed: DownloadProgressResult()

    /**
     * Successfully downloaded an additional chunk of data of size [bytes].
     */
    class Progressed(val bytes: Long): DownloadProgressResult()

    /**
     * Full [DownloadItem] is downloaded, the download can be stopped.
     */
    object DownloadComplete: DownloadProgressResult()
}

interface DownloadProvider {
    /**
     * Start or continue downloading from `byteStartPosition`.
     */
    suspend fun downloadNextChunk(
        item: DownloadItem,
        sink: BufferedSink,
    ): DownloadProgressResult
}