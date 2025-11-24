package com.github.libretube.factory

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.WorkManager
import coil3.ImageLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import com.github.libretube.R

private const val CHANNEL_ID_DEFAULT = "download"
private const val CHANNEL_ID_SILENT = "download_bg"
class NotificationFactory @AssistedInject constructor(
    @ApplicationContext private val context: Context) {

    private val builder = NotificationCompat.Builder(context,CHANNEL_ID_DEFAULT)

    suspend fun create(): Notification {
        val builder = NotificationCompat.Builder(context, "progress_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading…")
            .setContentText("Hello World")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        return builder.build()
    }

    @AssistedFactory
    interface Factory {
        fun create(): NotificationFactory
    }
}


