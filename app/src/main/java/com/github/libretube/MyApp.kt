package com.github.libretube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.libretube.api.CronetHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.preferences.PreferenceHelper
import com.github.libretube.preferences.PreferenceKeys
import com.github.libretube.util.ExceptionHandler
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NotificationHelper

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
        PreferenceHelper.setContext(applicationContext)

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
        initializeRetrofit()

        /**
         * Initialize the notification listener in the background
         */
        NotificationHelper(this).enqueueWork(
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
        )

        NotificationHelper(this).checkForNewStreams()

        /**
         * Handler for uncaught exceptions
         */
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val exceptionHandler = ExceptionHandler(defaultExceptionHandler)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

        /**
         * Legacy preference file migration
         */
        prefFileMigration()

        /**
         * Database Migration
         */
        databaseMigration()
    }

    /**
     * Set the api urls needed for the [RetrofitInstance]
     */
    private fun initializeRetrofit() {
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
        CronetHelper.initCronet(this)
        ImageHelper.initializeImageLoader(this)
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

    /**
     * Migration from the preferences to the database
     */
    private fun databaseMigration() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val mapper = ObjectMapper()

        Thread {
            val legacyWatchHistory = prefs.getString("watch_history", "")
            if (legacyWatchHistory != "") {
                try {
                    val type = object : TypeReference<List<WatchHistoryItem>>() {}
                    val watchHistoryItems = mapper.readValue(legacyWatchHistory, type)
                    DatabaseHolder.db.watchHistoryDao().insertAll(
                        *watchHistoryItems.toTypedArray()
                    )
                } catch (e: Exception) {
                }
                prefs.edit().putString("watch_history", "").commit()
            }
            val legacyWatchPositions = prefs.getString("watch_positions", "")
            if (legacyWatchPositions != "") {
                try {
                    val type = object : TypeReference<List<WatchPosition>>() {}
                    val watchPositions = mapper.readValue(legacyWatchPositions, type)
                    DatabaseHolder.db.watchPositionDao().insertAll(
                        *watchPositions.toTypedArray()
                    )
                } catch (e: Exception) {
                }
                prefs.edit().remove("watch_positions").commit()
            }
            prefs.edit()
                .remove("custom_instances")
                .remove("local_subscriptions")
                .commit()
        }.start()
    }
}
