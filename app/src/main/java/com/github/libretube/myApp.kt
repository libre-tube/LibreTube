package com.github.libretube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class myApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeNotificationChannels()
    }

    /**
     * Initializes the required [NotificationChannel] for the app.
     */
    private fun initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Background Mode"
            val descriptionText = "Shows a notification with buttons to control the audio player"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel("background_mode", name, importance)
            mChannel.description = descriptionText

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    companion object {
        @JvmField
        var seekTo: Long? = 0
    }
}
