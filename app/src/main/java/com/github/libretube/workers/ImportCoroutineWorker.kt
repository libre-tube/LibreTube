package com.github.libretube.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.WorkersData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.NotificationBuilder
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import kotlinx.serialization.json.decodeFromStream

class ImportCoroutineWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val notificationFactory = NotificationBuilder.createBuilder(appContext)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val IMPORT_NOTIFICATION_ID = 1001

    private val IMPORT_THUMBNAIL_QUALITY = "mqdefault"
    private val VIDEO_ID_LENGTH = 11
    private val YOUTUBE_IMG_URL = "https://img.youtube.com"
    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
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
        val fileUrisString = inputData.getStringArray(WorkersData.FILES)
        val importFormatString =
            inputData.getString(WorkersData.IMPORT_FORMAT) ?: ImportFormat.YOUTUBEJSON.toString()
        val importFormat = enumValueOf<ImportFormat>(importFormatString)
        val videos = fileUrisString?.map { a ->
            val fileUrisDeserialized = Uri.parse(a)
            when (importFormat) {
                ImportFormat.YOUTUBEJSON -> {
                    applicationContext.contentResolver.openInputStream(fileUrisDeserialized)?.use {
                        JsonHelper.json.decodeFromStream<List<YouTubeWatchHistoryFileItem>>(it)

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
                    } ?: emptyList()
                }

                else -> emptyList()
            }
        }?.flatten().orEmpty()

        var lastUpdateTime = 0L
        val updateInterval = 500L

        for ((index, video) in videos.withIndex()) {
            DatabaseHelper.addToWatchHistory(video)
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= updateInterval) {
                publishState(index, videos.size)
            }
        }

        if (videos.isEmpty()) {
            applicationContext.toastFromMainDispatcher(R.string.emptyList)
        } else {
            applicationContext.toastFromMainDispatcher(R.string.success)
        }
    }

    private fun publishState(currentState: Int = 0, finalState: Int = 0) {
        val notification: Notification =
            NotificationBuilder.instance.updateState(currentState, finalState)
        notificationManager.notify(IMPORT_NOTIFICATION_ID, notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            IMPORT_NOTIFICATION_ID, NotificationBuilder.instance.createNotification()
        )
    }

}