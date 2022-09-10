package com.github.libretube.util

import android.content.Context
import com.github.libretube.constants.DownloadType
import com.github.libretube.obj.DownloadedFile
import java.io.File

object DownloadHelper {
    private fun getOfflineStorageDir(context: Context): File {
        return context.getExternalFilesDir(null)!!
    }

    fun getVideoDir(context: Context): File {
        return File(
            getOfflineStorageDir(context),
            "video"
        )
    }

    fun getAudioDir(context: Context): File {
        return File(
            getOfflineStorageDir(context),
            "audio"
        )
    }

    fun getDownloadedFiles(context: Context): MutableList<DownloadedFile> {
        val videoFiles = getVideoDir(context).listFiles()
        val audioFiles = getAudioDir(context).listFiles()?.toMutableList()

        val files = mutableListOf<DownloadedFile>()

        videoFiles?.forEach {
            var type = DownloadType.VIDEO
            audioFiles?.forEach { audioFile ->
                if (audioFile.name == it.name) {
                    type = DownloadType.AUDIO_VIDEO
                    audioFiles.remove(audioFile)
                }
            }
            files.add(
                DownloadedFile(
                    name = it.name,
                    size = it.length(),
                    type = type
                )
            )
        }

        audioFiles?.forEach {
            files.add(
                DownloadedFile(
                    name = it.name,
                    size = it.length(),
                    type = DownloadType.AUDIO
                )
            )
        }

        return files
    }
}
