package com.github.libretube.workers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.libretube.LibreTubeApp.Companion.IMPORT_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.constants.ImportWorkerData
import com.github.libretube.enums.ImportFormat
import com.github.libretube.enums.ImportType
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.ImportHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import java.util.UUID

class ImportCoroutineWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private var exceptions: MutableList<Exception> = mutableListOf()

    private lateinit var notificationFactory: ImportNotificationHandler
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val importTypeString = inputData.getString(ImportWorkerData.IMPORT_TYPE)!!
        val importType: ImportType = enumValueOf<ImportType>(importTypeString)

        notificationFactory = ImportNotificationHandler(id, appContext, importType)
        setForeground(getForegroundInfo())

        val fileUris = inputData.getStringArray(ImportWorkerData.FILES)?.map { it.toUri() }
            ?: return Result.failure()
        val importFormatString = inputData.getString(ImportWorkerData.IMPORT_FORMAT)!!
        val importFormat = enumValueOf<ImportFormat>(importFormatString)

        val dataSize = when (importType) {
            ImportType.IMPORT_WATCH_HISTORY -> importWatchHistory(
                fileUris,
                importFormat
            )

            ImportType.IMPORT_SUBSCRIPTIONS -> importSubscriptions(fileUris, importFormat)
            ImportType.IMPORT_PLAYLISTS -> importPlaylists(fileUris, importFormat)
        }

        if (exceptions.isEmpty()) {
            applicationContext.toastFromMainDispatcher(R.string.success)
        } else {
            // TODO: find a way to display the entries that failed to the user
            applicationContext.toastFromMainDispatcher(
                applicationContext.getString(
                    R.string.partial_import_success,
                    dataSize - exceptions.size,
                    exceptions.size
                )
            )
        }

        return Result.success()
    }

    private suspend fun importWatchHistory(
        fileUris: List<Uri>,
        importFormat: ImportFormat
    ): Int {
        val videos = fileUris.flatMap {
            ImportHelper.parseWatchHistory(applicationContext, it, importFormat)
        }

        videos.forEachIndexed { index, video ->
            runActionAndUpdateProgress(index, videos.size) {
                @Suppress("DEPRECATION")
                UserDataRepositoryHelper.userDataRepository.addToWatchHistory(
                    video.toStreamItem().toWatchHistoryEntry(null)
                )
            }
        }

        return videos.size
    }

    private suspend fun importPlaylists(
        fileUris: List<Uri>,
        importFormat: ImportFormat
    ): Int {
        val playlists = fileUris.flatMap {
            ImportHelper.parsePlaylists(applicationContext, it, importFormat)
        }

        playlists.forEachIndexed { index, playlist ->
            runActionAndUpdateProgress(index, playlists.size) {
                PlaylistsHelper.importPlaylist(playlist)
            }
        }

        return playlists.size
    }

    private suspend fun importSubscriptions(
        fileUris: List<Uri>,
        importFormat: ImportFormat
    ): Int {
        val subscriptions = fileUris.flatMap {
            ImportHelper.parseSubscriptions(applicationContext, it, importFormat)
        }

        // subscription imports can't be paused
        SubscriptionHelper.importSubscriptions(subscriptions) {
            updateNotification(
                notificationFactory.updateProgress(it, subscriptions.size)
            )
        }

        return subscriptions.size
    }

    private suspend fun runActionAndUpdateProgress(
        currentState: Int,
        finalState: Int,
        action: suspend () -> Unit
    ) {
        try {
            action()
        } catch (e: Exception) {
            exceptions.add(e)
        }

        val now = System.currentTimeMillis()
        if (now - lastUpdateTime >= UPDATE_INTERVAL) {
            updateNotification(
                notificationFactory.updateProgress(currentState, finalState)
            )
            lastUpdateTime = now
        }
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(id.hashCode(), notification)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            id.hashCode(),
            notificationFactory.createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        fun startImportWorker(
            context: Context,
            uris: List<Uri>,
            importType: ImportType,
            importFormat: ImportFormat
        ) {
            val uuid = UUID.randomUUID()
            val workRequest = OneTimeWorkRequestBuilder<ImportCoroutineWorker>()
                .setId(uuid)
                .setInputData(
                    workDataOf(
                        ImportWorkerData.FILES to uris.map { it.toString() }.toTypedArray(),
                        ImportWorkerData.IMPORT_TYPE to importType.toString(),
                        ImportWorkerData.IMPORT_FORMAT to importFormat.name
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        private var lastUpdateTime = 0L
        private const val UPDATE_INTERVAL = 500L
    }
}

class ImportNotificationHandler(
    private val uuid: UUID,
    private val context: Context,
    private val importType: ImportType
) {
    private val builder = NotificationCompat.Builder(context, IMPORT_CHANNEL_NAME)

    fun createNotification(): Notification {
        val actionCancel by lazy {
            NotificationCompat.Action(
                R.drawable.ic_baseline_cancel,
                context.getString(android.R.string.cancel),
                WorkManager.getInstance(context).createCancelPendingIntent(uuid),
            )
        }

        builder.setContentTitle(
            context.getString(
                R.string.importing,
                context.getString(importType.stringRes)
            )
        )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setProgress(0, 0, true)
            .addAction(actionCancel)

        return builder.build()
    }

    fun updateProgress(currentState: Int, finalState: Int): Notification {
        builder.setProgress(finalState, currentState, false)
        return builder.build()
    }
}