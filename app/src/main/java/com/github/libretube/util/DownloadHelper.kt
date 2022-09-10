package com.github.libretube.util

import android.content.Context
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
}
