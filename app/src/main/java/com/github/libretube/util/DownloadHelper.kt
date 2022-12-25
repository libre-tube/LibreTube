package com.github.libretube.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.libretube.constants.IntentData
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.services.DownloadService
import java.io.File

object DownloadHelper {
    const val VIDEO_DIR = "video"
    const val AUDIO_DIR = "audio"
    const val SUBTITLE_DIR = "subtitle"
    const val METADATA_DIR = "metadata"
    const val THUMBNAIL_DIR = "thumbnail"
    const val DOWNLOAD_CHUNK_SIZE = 8L * 1024
    const val DEFAULT_TIMEOUT = 15 * 1000
    const val DEFAULT_RETRY = 3

    fun getOfflineStorageDir(context: Context): File {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return context.filesDir

        return try {
            context.getExternalFilesDir(null)!!
        } catch (e: Exception) {
            context.filesDir
        }
    }

    fun getDownloadDir(context: Context, path: String): File {
        return File(
            getOfflineStorageDir(context),
            path
        ).apply {
            if (!this.exists()) this.mkdirs()
        }
    }

    fun startDownloadService(
        context: Context,
        videoId: String? = null,
        fileName: String? = null,
        videoFormat: String? = null,
        videoQuality: String? = null,
        audioFormat: String? = null,
        audioQuality: String? = null,
        subtitleCode: String? = null
    ) {
        val intent = Intent(context, DownloadService::class.java)

        intent.putExtra(IntentData.videoId, videoId)
        intent.putExtra(IntentData.fileName, fileName)
        intent.putExtra(IntentData.videoFormat, videoFormat)
        intent.putExtra(IntentData.videoQuality, videoQuality)
        intent.putExtra(IntentData.audioFormat, audioFormat)
        intent.putExtra(IntentData.audioQuality, audioQuality)
        intent.putExtra(IntentData.subtitleCode, subtitleCode)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun DownloadItem.getNotificationId(): Int {
        return Int.MAX_VALUE - id
    }
}
