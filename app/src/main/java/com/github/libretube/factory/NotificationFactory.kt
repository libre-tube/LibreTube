package com.github.libretube.factory

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.WorkManager
import com.github.libretube.LibreTubeApp.Companion.IMPORT_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.enums.ImportState
import com.github.libretube.receivers.ImportReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val CHANNEL_ID_DEFAULT = "import_history"

class NotificationFactory @AssistedInject constructor(
    @ApplicationContext val context: Context,
    workManager: WorkManager,
    @Assisted private val uuid: UUID
) {

    private val builder = NotificationCompat.Builder(context, IMPORT_CHANNEL_NAME)
    private val mutex = Mutex()

    fun createNotification(uuid: UUID): Notification {
        builder.setContentTitle("Storing")
            .setContentText("Preparing")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_launcher_lockscreen)
            .setProgress(0, 0, false)
        return builder.build()
    }


    private val notificationChannelId = "DemoNotificationChannelId"


    init {
        createNotificationChannel()
    }

    private val actionCancel by lazy {
        NotificationCompat.Action(
            R.drawable.baseline_cancel_24,
            context.getString(android.R.string.cancel),
            workManager.createCancelPendingIntent(uuid),
        )
    }


    private val actionPause by lazy {
        NotificationCompat.Action(
            R.drawable.ic_action_pause,
            context.getString(R.string.pause),
            ImportReceiver.createPausePendingIntent(context),
        )
    }

    private val actionResume by lazy {
        NotificationCompat.Action(
            R.drawable.ic_action_resume,
            context.getString(R.string.resume),
            ImportReceiver.createResumePendingIntent(context),
        )
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                notificationChannelId,
                CHANNEL_ID_DEFAULT,
                NotificationManager.IMPORTANCE_DEFAULT,
            )

            val notificationManager: NotificationManager? =
                getSystemService(
                    context,
                    NotificationManager::class.java
                )

            notificationManager?.createNotificationChannel(
                notificationChannel
            )
        }
    }

    private fun createActionResume(): NotificationCompat.Action {
        val pendingIntent = ImportReceiver.createResumePendingIntent(context)
        return NotificationCompat.Action(
            R.drawable.ic_action_resume,
            context.getString(R.string.resume),
            pendingIntent
        )
    }


    suspend fun updateState(
        currentState: Int = 0,
        finalState: Int = 0,
        importState: ImportState = ImportState.RESUME
    ): Notification = mutex.withLock {
        val percent =
            if (finalState == 0) 0 else ((currentState.toFloat() / finalState) * 100).toInt()
        builder.setContentText("$percent% completed")
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



    @AssistedFactory
    interface Factory {

        fun create(uuid: UUID): NotificationFactory
    }
}


