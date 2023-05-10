package com.github.libretube.obj

sealed class DownloadStatus {

    object Completed : DownloadStatus()

    object Paused : DownloadStatus()

    data class Progress(
        val progress: Long,
        val downloaded: Long,
        val total: Long,
    ) : DownloadStatus()

    data class Error(val message: String, val cause: Throwable? = null) : DownloadStatus()
}
