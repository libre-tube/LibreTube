package com.github.libretube.util

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.libretube.constants.IntentData
import com.github.libretube.obj.DownloadedFile
import com.github.libretube.services.DownloadService
import java.io.File

object DownloadHelper {
    const val VIDEO_DIR = "video"
    const val AUDIO_DIR = "audio"
    const val METADATA_DIR = "metadata"
    const val THUMBNAIL_DIR = "thumbnail"

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

    private fun File.toDownloadedFile(): DownloadedFile {
        return DownloadedFile(
            name = this.name,
            size = this.length()
        )
    }

    fun getDownloadedFiles(context: Context): MutableList<DownloadedFile> {
        val videoFiles = getDownloadDir(context, VIDEO_DIR).listFiles().orEmpty()
        val audioFiles = getDownloadDir(context, AUDIO_DIR).listFiles().orEmpty().toMutableList()

        val files = mutableListOf<DownloadedFile>()

        videoFiles.forEach {
            audioFiles.removeIf { audioFile -> audioFile.name == it.name }
            files.add(it.toDownloadedFile())
        }

        audioFiles.forEach {
            files.add(it.toDownloadedFile())
        }

        return files
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
}
