package com.github.libretube.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
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
import com.github.libretube.factory.NotificationFactory
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import com.github.libretube.util.YoutubeHlsPlaylistParser
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.decodeFromStream

class ImportCoroutineWorker @AssistedInject constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    notificationFactoryFactory: NotificationFactory.Factory
) :
    CoroutineWorker(appContext, workerParams) {
    private val notificationFactory = notificationFactoryFactory.create()

    private val IMPORT_THUMBNAIL_QUALITY = "mqdefault"
    private val VIDEO_ID_LENGTH = 11
    private val YOUTUBE_IMG_URL = "https://img.youtube.com"
    override suspend fun doWork(): Result {
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


    override suspend fun getForegroundInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(
            id.hashCode(),
            notificationFactory.create(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    } else {
        ForegroundInfo(
            id.hashCode(),
            notificationFactory.create(),
        )
    }


}