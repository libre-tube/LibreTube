package com.github.libretube.helpers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.obj.DownloadItem
import com.github.libretube.services.DownloadService
import java.nio.file.Path

object DownloadHelper {
    const val VIDEO_DIR = "video"
    const val AUDIO_DIR = "audio"
    const val SUBTITLE_DIR = "subtitle"
    const val METADATA_DIR = "metadata"
    const val THUMBNAIL_DIR = "thumbnail"
    const val DOWNLOAD_CHUNK_SIZE = 8L * 1024
    const val DEFAULT_TIMEOUT = 15 * 1000
    const val DEFAULT_RETRY = 3

    private fun getOfflineStorageDir(context: Context): Path {
        val file = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            context.filesDir
        } else {
            try {
                context.getExternalFilesDir(null)!!
            } catch (e: Exception) {
                context.filesDir
            }
        }
        return file.toPath()
    }

    fun getDownloadDir(context: Context, path: String): Path {
        // TODO: Use createDirectories() when https://issuetracker.google.com/issues/279034662 is
        // fixed.
        return getOfflineStorageDir(context).resolve(path).apply {
            toFile().mkdirs()
        }
    }

    fun getMaxConcurrentDownloads(): Int {
        return PreferenceHelper.getString(
            PreferenceKeys.MAX_CONCURRENT_DOWNLOADS,
            "6",
        ).toFloat().toInt()
    }

    fun startDownloadService(
        context: Context,
        videoId: String? = null,
        fileName: String? = null,
        videoFormat: String? = null,
        videoQuality: String? = null,
        audioFormat: String? = null,
        audioQuality: String? = null,
        subtitleCode: String? = null,
    ) {
        val intent = Intent(context, DownloadService::class.java)

        intent.putExtra(IntentData.videoId, videoId)
        intent.putExtra(IntentData.fileName, fileName)
        intent.putExtra(IntentData.videoFormat, videoFormat)
        intent.putExtra(IntentData.videoQuality, videoQuality)
        intent.putExtra(IntentData.audioFormat, audioFormat)
        intent.putExtra(IntentData.audioQuality, audioQuality)
        intent.putExtra(IntentData.subtitleCode, subtitleCode)

        ContextCompat.startForegroundService(context, intent)
    }

    fun DownloadItem.getNotificationId(): Int {
        return Int.MAX_VALUE - id
    }
}
