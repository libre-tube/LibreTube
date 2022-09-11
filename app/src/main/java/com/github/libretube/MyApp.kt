package com.github.libretube

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.libretube.api.CronetHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.util.ExceptionHandler
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NotificationHelper
import com.github.libretube.util.PreferenceHelper

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * Initialize the needed [NotificationChannel]s for DownloadService and BackgroundMode
         */
        initializeNotificationChannels()

        /**
         * Initialize the [PreferenceHelper]
         */
        PreferenceHelper.initialize(applicationContext)

        /**
         * Initialize the [DatabaseHolder]
         */
        DatabaseHolder.initializeDatabase(this)

        /**
         * Bypassing fileUriExposedException, see https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
         */
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        /**
         * Set the api and the auth api url
         */
        RetrofitInstance.initialize()
        CronetHelper.initCronet(this)
        ImageHelper.initializeImageLoader(this)

        /**
         * Initialize the notification listener in the background
         */
        NotificationHelper(this).enqueueWork(
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
        )

        /**
         * Handler for uncaught exceptions
         */
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val exceptionHandler = ExceptionHandler(defaultExceptionHandler)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
    }

    /**
     * Initializes the required [NotificationChannel]s for the app.
     */
    @SuppressLint("InlinedApi")
    private fun initializeNotificationChannels() {
        createNotificationChannel(
            DOWNLOAD_CHANNEL_ID,
            "Download Service",
            "Shows a notification when downloading media.",
            NotificationManager.IMPORTANCE_NONE
        )
        createNotificationChannel(
            BACKGROUND_CHANNEL_ID,
            "Background Mode",
            "Shows a notification with buttons to control the audio player",
            NotificationManager.IMPORTANCE_LOW
        )
        createNotificationChannel(
            PUSH_CHANNEL_ID,
            "Notification Worker",
            "Shows a notification when new streams are available.",
            NotificationManager.IMPORTANCE_DEFAULT
        )
    }

    /**
     * Creates a [NotificationChannel]
     */
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
}
