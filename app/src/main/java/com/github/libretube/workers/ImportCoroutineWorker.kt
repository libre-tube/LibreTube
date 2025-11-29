package com.github.libretube.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.constants.WorkersData
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportState
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.factory.NotificationFactory
import com.github.libretube.handler.ImportHandler
import com.github.libretube.obj.YouTubeWatchHistoryFileItem
import com.github.libretube.receivers.ImportReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromStream
import java.util.UUID
import javax.inject.Inject

@HiltWorker
class ImportCoroutineWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    notificationFactoryFactory: NotificationFactory.Factory
) : CoroutineWorker(appContext, workerParams) {

    private val notificationFactory = notificationFactoryFactory.create(id)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val IMPORT_THUMBNAIL_QUALITY = "mqdefault"
    private val VIDEO_ID_LENGTH = 11
    private val YOUTUBE_IMG_URL = "https://img.youtube.com"
    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())
        val pausingHandle = ImportHandler()
        val importType = inputData.getString(WorkersData.IMPORT_TYPE)
            ?: ImportType.IMPORT_WATCH_HISTORY.toString()
        val importEnum: ImportType = enumValueOf<ImportType>(importType)
        when (importEnum) {
            ImportType.IMPORT_WATCH_HISTORY -> {
                withContext(pausingHandle) {
                    ImportWatchHistory()
                }
            }

            else -> Unit
        }
        return Result.success()
    }


    private suspend fun ImportWatchHistory(): Unit {
        registerReceiver()

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

        registerReceiver()
        var index = 0
        while (index < videos.size) {
            checkIfPausedOrCancelled(index, videos.size) {
                if (it) {
                    DatabaseHelper.addToWatchHistory(videos.get(index))
                    index++
                }
            }
        }

        if (videos.isEmpty()) {
            applicationContext.toastFromMainDispatcher(R.string.emptyList)
        } else {
            applicationContext.toastFromMainDispatcher(R.string.success)
        }
    }


    var lastUpdateTime = 0L
    val updateInterval = 500L
    private suspend fun checkIfPausedOrCancelled(
        currentState: Int,
        finalState: Int,
        dispatch: suspend (Boolean) -> Unit
    ) {
        val importHandler = ImportHandler.current()
        val now = System.currentTimeMillis()
        if (importHandler.isPaused) {
            dispatch(false)
            notifyNotification(notificationFactory.updateState(currentState, finalState, ImportState.PAUSED))
            importHandler.awaitResumed()
        } else if (!importHandler.isPaused) {
            dispatch(true)
            if (now - lastUpdateTime >= updateInterval) {
                notifyNotification(notificationFactory.updateState(currentState, finalState, ImportState.RESUME))
                lastUpdateTime = now
            }
        }
    }

    private fun notifyNotification(notification: Notification) {
        notificationManager.notify(id.hashCode(), notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            id.hashCode(), notificationFactory.createNotification(id)
        )
    }

    private suspend fun registerReceiver() {
        val importReceiver = ImportReceiver(id,ImportHandler.current())
        ContextCompat.registerReceiver(
            applicationContext,
            importReceiver,
            ImportReceiver.createIntentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    class Scheduler @Inject constructor(
        @ApplicationContext private val context: Context,
        private val workManager: WorkManager
    ) {
        fun importWatchHistory(uris: List<Uri>, importFormat: ImportFormat) {
            val uuid = UUID.randomUUID()
            val workRequest = OneTimeWorkRequestBuilder<ImportCoroutineWorker>()
                .setId(uuid)
                .setInputData(
                    workDataOf(
                        WorkersData.FILES to uris.map { it.toString() }.toTypedArray(),
                        WorkersData.IMPORT_TYPE to ImportFormat.YOUTUBEJSON.value,
                        WorkersData.IMPORT_FORMAT to importFormat.value
                    )
                )
                .build()
            workManager.enqueue(workRequest)
        }
    }
}