package com.github.libretube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.ExceptionHandler
import com.github.libretube.util.NotificationHelper
import com.github.libretube.util.RetrofitInstance

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        /**
         * initialize the needed [NotificationChannel]s for DownloadService and BackgroundMode
         */
        initializeNotificationChannels()

        /**
         * set the applicationContext as context for the [PreferenceHelper]
         */
        PreferenceHelper.setContext(applicationContext)

        /**
         * bypassing fileUriExposedException, see https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
         */
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        /**
         * set the api and the auth api url
         */
        setRetrofitApiUrls()

        /**
         * initialize the notification listener in the background
         */
        NotificationHelper.enqueueWork(this, ExistingPeriodicWorkPolicy.KEEP)

        /**
         * Handler for uncaught exceptions
         */
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val exceptionHandler = ExceptionHandler(defaultExceptionHandler)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

        /**
         * legacy preference file migration
         */
        prefFileMigration()
    }

    /**
     * set the api urls needed for the [RetrofitInstance]
     */
    private fun setRetrofitApiUrls() {
        RetrofitInstance.url =
            PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, PIPED_API_URL)
        // set auth instance
        RetrofitInstance.authUrl =
            if (PreferenceHelper.getBoolean(PreferenceKeys.AUTH_INSTANCE_TOGGLE, false)) {
                PreferenceHelper.getString(
                    PreferenceKeys.AUTH_INSTANCE,
                    PIPED_API_URL
                )
            } else {
                RetrofitInstance.url
            }
    }

    /**
     * Initializes the required [NotificationChannel]s for the app.
     */
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

    /**
     * Migration from old preference files to new one
     */
    private fun prefFileMigration() {
        val legacyUserPrefs = getSharedPreferences("username", Context.MODE_PRIVATE)
        val username = legacyUserPrefs.getString("username", "")!!
        if (username != "") {
            PreferenceHelper.setUsername(username)
            legacyUserPrefs.edit().putString("username", "")
        }
        val legacyTokenPrefs = getSharedPreferences("token", Context.MODE_PRIVATE)
        val token = legacyUserPrefs.getString("token", "")!!
        if (token != "") {
            PreferenceHelper.setToken(token)
            legacyTokenPrefs.edit().putString("token", "")
        }
    }
}
