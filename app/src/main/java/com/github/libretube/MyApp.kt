package com.github.libretube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeNotificationChannels()
    }

    /**
     * Initializes the required [NotificationChannel]s for the app.
     */
    private fun initializeNotificationChannels() {
        createNotificationChannel(
            "download_service",
            "Download Service",
            "DownloadService",
            NotificationManager.IMPORTANCE_NONE
        )
        createNotificationChannel(
            "background_mode",
            "Background Mode",
            "Shows a notification with buttons to control the audio player",
            NotificationManager.IMPORTANCE_LOW
        )
    }

    private fun createNotificationChannel(
        id: String,
        name: String,
        descriptionText: String,
        importance: Int
    ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance)
            channel.description = descriptionText
            // Register the channel in the system
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        @JvmField
        var seekTo: Long? = 0
    }
}
