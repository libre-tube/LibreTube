package com.github.libretube.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.libretube.LibreTubeApp.Companion.IMPORT_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.constants.WorkersData
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.receivers.ImportReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

class ImportHandler {
    private var paused = MutableStateFlow(false)

    val isPaused: Boolean
        get() = paused.value

    fun pause() {
        paused.value = true
    }

    fun resume() {
        paused.value = false
    }

    suspend fun awaitResumed() {
        paused.first { !it }
    }
}

enum class ImportState {
    PAUSED,
    RUNNING,
}

class ImportCoroutineWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val notificationFactory =
        ImportNotificationHandler(id, appContext)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())

        val pausingHandle = ImportHandler()
        val importTypeString = inputData.getString(WorkersData.IMPORT_TYPE)
            ?: throw Exception(appContext.getString(R.string.import_format_unsupported))
        val importType: ImportType = enumValueOf<ImportType>(importTypeString)
        when (importType) {
            ImportType.IMPORT_WATCH_HISTORY -> {
                importWatchHistory(pausingHandle)
            }

            else -> Unit
        }
        return Result.success()
    }


    private suspend fun importWatchHistory(importHandler: ImportHandler) {
        registerReceiver(importHandler)

        val fileUrisString = inputData.getStringArray(WorkersData.FILES) ?: return
        val importFormatString =
            inputData.getString(WorkersData.IMPORT_FORMAT) ?: ImportFormat.YOUTUBEJSON.toString()
        val importFormat = enumValueOf<ImportFormat>(importFormatString)

        val videos = ImportHelper.parseWatchHistory(
            applicationContext,
            fileUrisString.map { it.toUri() },
            importFormat
        )

        var index = 0
        while (videos.isNotEmpty() && index < videos.size) {
            runActionAndUpdateNotificationIfNotPaused(importHandler, index, videos.size) {
                DatabaseHolder.Database.watchHistoryDao().insert(videos[index])
                index++
            }
        }

        if (videos.isEmpty()) {
            applicationContext.toastFromMainDispatcher(R.string.emptyList)
        } else {
            applicationContext.toastFromMainDispatcher(R.string.success)
        }
    }

    private suspend fun runActionAndUpdateNotificationIfNotPaused(
        importHandler: ImportHandler,
        currentState: Int,
        finalState: Int,
        action: suspend () -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (importHandler.isPaused) {
            updateNotification(
                notificationFactory.updateState(
                    currentState,
                    finalState,
                    ImportState.PAUSED
                )
            )
            importHandler.awaitResumed()
        } else if (!importHandler.isPaused) {
            action()
            if (now - lastUpdateTime >= UPDATE_INTERVAL) {
                updateNotification(
                    notificationFactory.updateState(
                        currentState,
                        finalState,
                        ImportState.RUNNING
                    )
                )
                lastUpdateTime = now
            }
        }
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(id.hashCode(), notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            id.hashCode(), notificationFactory.createNotification()
        )
    }

    private fun registerReceiver(importHandler: ImportHandler) {
        val importReceiver = ImportReceiver(id, importHandler)
        ContextCompat.registerReceiver(
            applicationContext,
            importReceiver,
            ImportReceiver.createIntentFilter(),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    companion object {
        fun startImportWatchHistoryWorker(
            context: Context,
            uris: List<Uri>,
            importFormat: ImportFormat
        ) {
            val uuid = UUID.randomUUID()
            val workRequest = OneTimeWorkRequestBuilder<ImportCoroutineWorker>()
                .setId(uuid)
                .setInputData(
                    workDataOf(
                        WorkersData.FILES to uris.map { it.toString() }.toTypedArray(),
                        WorkersData.IMPORT_TYPE to ImportType.IMPORT_WATCH_HISTORY.toString(),
                        WorkersData.IMPORT_FORMAT to importFormat.value
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        private var lastUpdateTime = 0L
        private const val UPDATE_INTERVAL = 500L
    }
}

class ImportNotificationHandler(private val uuid: UUID, private val context: Context) {
    private val builder = NotificationCompat.Builder(context, IMPORT_CHANNEL_NAME)

    fun createNotification(): Notification {
        builder.setContentTitle(context.getString(R.string.importing))
            .setContentText(context.getString(R.string.preparing))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setProgress(0, 0, false)
        return builder.build()
    }

    private val actionCancel by lazy {
        NotificationCompat.Action(
            R.drawable.ic_baseline_cancel,
            context.getString(android.R.string.cancel),
            WorkManager.getInstance(context).createCancelPendingIntent(uuid),
        )
    }

    private val actionPause by lazy {
        NotificationCompat.Action(
            R.drawable.ic_pause,
            context.getString(R.string.pause),
            ImportReceiver.createPausePendingIntent(context, uuid),
        )
    }

    private val actionResume by lazy {
        NotificationCompat.Action(
            R.drawable.ic_play,
            context.getString(R.string.resume),
            ImportReceiver.createResumePendingIntent(context, uuid),
        )
    }

    fun updateState(
        currentState: Int,
        finalState: Int,
        importState: ImportState
    ): Notification {
        val percent =
            if (finalState == 0) 0 else ((currentState.toFloat() / finalState) * 100).toInt()
        builder.setContentText(context.getString(R.string.percentage_completed, percent))
        builder.setProgress(finalState, currentState, false)
        builder.clearActions()
        when (importState) {
            ImportState.PAUSED -> {
                builder.addAction(actionResume)
                builder.addAction(actionCancel)
            }

            else -> {
                builder.addAction(actionPause)
                builder.addAction(actionCancel)
            }
        }
        return builder.build()
    }
}