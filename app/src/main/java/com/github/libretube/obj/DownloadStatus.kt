package com.github.libretube.obj

sealed class DownloadStatus {

    object Unknown : DownloadStatus()

    object Completed : DownloadStatus()

    object Paused : DownloadStatus()

    data class Progress(val downloaded: Long, val total: Long) : DownloadStatus()

    data class Error(val message: String, val cause: Throwable? = null) : DownloadStatus()
}
