package com.github.libretube.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.github.libretube.LibreTubeApp.Companion.IMPORT_CHANNEL_NAME
import com.github.libretube.R

private const val CHANNEL_ID_DEFAULT = "download"
private const val CHANNEL_ID_SILENT = "download_bg"

class NotificationBuilder private constructor(private val context: Context) {
    private val builder = NotificationCompat.Builder(context, IMPORT_CHANNEL_NAME)

    companion object {
        private val lockObject = Any()
        lateinit var instance: NotificationBuilder
        fun createBuilder(context: Context) {
            if (!::instance.isInitialized) {
                synchronized(lockObject) {
                    instance = NotificationBuilder(context)
                }
            }
        }
    }

    private val notificationChannelId = "DemoNotificationChannelId"

    init {
        createNotificationChannel()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "DemoWorker",
                NotificationManager.IMPORTANCE_DEFAULT,
            )

            val notificationManager: NotificationManager? =
                getSystemService(
                    context,
                    NotificationManager::class.java
                )

            notificationManager?.createNotificationChannel(
                notificationChannel
            )
        }
    }

    fun createNotification(): Notification {
        builder.setContentTitle("Storing")
            .setContentText("Preparing")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setProgress(0, 0, false)
        return builder.build()
    }

    fun updateState(currentState: Int = 0, finalState: Int = 0): Notification {
        val percent =
            if (finalState == 0) 0 else ((currentState.toFloat() / finalState) * 100).toInt()
        builder.setContentText("$percent% completed")
        builder.setProgress(finalState, currentState, false)
        return builder.build()
    }


}