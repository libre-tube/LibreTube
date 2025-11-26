package com.github.libretube.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.github.libretube.LibreTubeApp.Companion.PUSH_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.WorkersData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.factory.NotificationFactory
import com.github.libretube.helpers.NotificationBuilder
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import com.github.libretube.ui.activities.MainActivity
import kotlinx.serialization.json.decodeFromStream
import kotlin.properties.Delegates

class ImportCoroutineWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationManager = NotificationManagerCompat.from(appContext)

    private val IMPORT_THUMBNAIL_QUALITY = "mqdefault"
    private val VIDEO_ID_LENGTH = 11
    private val YOUTUBE_IMG_URL = "https://img.youtube.com"
    override suspend fun doWork(): Result {
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(0, createNotification())
        }
        val importType = inputData.getString(WorkersData.IMPORT_TYPE)
            ?: ImportType.IMPORT_WATCH_HISTORY.toString()
        val importEnum: ImportType = enumValueOf<ImportType>(importType)
        when (importEnum) {
            ImportType.IMPORT_WATCH_HISTORY -> {
                ImportWatchHistory()
            }

            else -> Unit
        }
        return Result.success()
    }


    private suspend fun ImportWatchHistory(): Unit {
        val fileUrisString = inputData.getString(WorkersData.FILES)
        val importFormatString =
            inputData.getString(WorkersData.IMPORT_FORMAT) ?: ImportFormat.YOUTUBEJSON.toString()
        val importFormat = enumValueOf<ImportFormat>(importFormatString)
        fileUrisString?.let { a ->
            val fileUrisDeserialized = JsonHelper.json.decodeFromString<List<Uri>>(a)
            val videos = when (importFormat) {
                ImportFormat.YOUTUBEJSON -> {
                    fileUrisDeserialized.flatMap { uri ->
                        applicationContext.contentResolver.openInputStream(uri)?.use {
                            JsonHelper.json.decodeFromStream<List<YouTubeWatchHistoryFileItem>>(it)
                        }
                            .orEmpty()
                            .filter { it.activityControls.isNotEmpty() && it.subtitles.isNotEmpty() && it.titleUrl.isNotEmpty() }
                            .reversed()
                            .map {
                                val videoId = it.titleUrl.takeLast(VIDEO_ID_LENGTH)

                                WatchHistoryItem(
                                    videoId = videoId,
                                    title = it.title.replaceFirst("Watched ", ""),
                                    uploader = it.subtitles.firstOrNull()?.name,
                                    uploaderUrl = it.subtitles.firstOrNull()?.url?.let { url ->
                                        url.substring(url.length - 24)
                                    },
                                    thumbnailUrl = "${YOUTUBE_IMG_URL}/vi/${videoId}/${IMPORT_THUMBNAIL_QUALITY}.jpg"
                                )
                            }
                    }
                }

                else -> emptyList()
            }
            for (video in videos) {
                DatabaseHelper.addToWatchHistory(video)
            }

            if (videos.isEmpty()) {
                applicationContext.toastFromMainDispatcher(R.string.emptyList)
            } else {
                applicationContext.toastFromMainDispatcher(R.string.success)
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createChannels()
        val notification = NotificationCompat.Builder(applicationContext, "progress_channel")
            .setSmallIcon(R.drawable.ic_notification) // mandatory
            .setContentTitle("Downloading…")
            .setContentText("Downloading file…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return ForegroundInfo(1, notification) // id must be >0
    }

    private  val CHANNEL_ID_SILENT = "download_bg"
    private  val CHANNEL_ID_DEFAULT = "download"

    private fun createChannels() {
        val manager = NotificationManagerCompat.from(applicationContext)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID_DEFAULT, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(applicationContext.getString(R.string.downloads))
                .setVibrationEnabled(false)
                .setLightsEnabled(false)
                .setSound(null, null)
                .build(),
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID_SILENT, NotificationManagerCompat.IMPORTANCE_MIN)
                .setName(applicationContext.getString(R.string.downloads))
                .setVibrationEnabled(false)
                .setLightsEnabled(false)
                .setSound(null, null)
                .setShowBadge(false)
                .build(),
        )
    }

    private fun createNotificationBuilder(group: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, PUSH_CHANNEL_NAME)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(group)
            .setCategory(Notification.CATEGORY_SOCIAL)
    }

    private val notificationChannelId = "DemoNotificationChannelId"

    /*...*/

    private fun createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "DemoWorker",
                NotificationManager.IMPORTANCE_DEFAULT,
            )

            val notificationManager: NotificationManager? =
                getSystemService(
                    applicationContext,
                    NotificationManager::class.java)

            notificationManager?.createNotificationChannel(
                notificationChannel
            )
        }
    }

    private fun createNotification() : Notification {
        createNotificationChannel()

        val mainActivityIntent = Intent(
            applicationContext,
            MainActivity::class.java)

        var pendingIntentFlag by Delegates.notNull<Int>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag = PendingIntent.FLAG_IMMUTABLE
        } else {
            pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT
        }

        val mainActivityPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            mainActivityIntent,
            pendingIntentFlag)


        return NotificationCompat.Builder(
            applicationContext,
            notificationChannelId
        )
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText("Work Request Done!")
            .setContentIntent(mainActivityPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            0, createNotification()
        )
    }

}