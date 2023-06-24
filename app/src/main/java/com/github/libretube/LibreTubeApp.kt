package com.github.libretube

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.BACKGROUND_CHANNEL_ID
import com.github.libretube.constants.DOWNLOAD_CHANNEL_ID
import com.github.libretube.constants.PUSH_CHANNEL_ID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NotificationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.helpers.ShortcutHelper
import com.github.libretube.util.ExceptionHandler

class LibreTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        /**
         * Initialize the needed notification channels for DownloadService and BackgroundMode
         */
        initializeNotificationChannels()

        /**
         * Initialize the [PreferenceHelper]
         */
        PreferenceHelper.initialize(applicationContext)

        /**
         * Set the api and the auth api url
         */
        RetrofitInstance.initialize()
        ImageHelper.initializeImageLoader(this)

        /**
         * Initialize the notification listener in the background
         */
        NotificationHelper.enqueueWork(
            context = this,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
        )

        /**
         * Fetch the image proxy URL for local playlists and the watch history
         */
        ProxyHelper.fetchProxyUrl()

        /**
         * Handler for uncaught exceptions
         */
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val exceptionHandler = ExceptionHandler(defaultExceptionHandler)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

        /**
         * Dynamically create App Shortcuts
         */
        ShortcutHelper.createShortcuts(this)
    }

    /**
     * Initializes the required notification channels for the app.
     */
    private fun initializeNotificationChannels() {
        val downloadChannel = NotificationChannelCompat.Builder(
            DOWNLOAD_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.download_channel_name))
            .setDescription(getString(R.string.download_channel_description))
            .build()
        val backgroundChannel = NotificationChannelCompat.Builder(
            BACKGROUND_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(getString(R.string.background_channel_name))
            .setDescription(getString(R.string.background_channel_description))
            .build()
        val pushChannel = NotificationChannelCompat.Builder(
            PUSH_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(getString(R.string.push_channel_name))
            .setDescription(getString(R.string.push_channel_description))
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.createNotificationChannelsCompat(
            listOf(
                downloadChannel,
                backgroundChannel,
                pushChannel
            )
        )
    }

    companion object {
        lateinit var instance: LibreTubeApp
    }
}
