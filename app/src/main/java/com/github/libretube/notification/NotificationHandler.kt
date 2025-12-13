package com.github.libretube.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.github.libretube.LibreTubeApp.Companion.IMPORT_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.enums.ImportState
import com.github.libretube.receivers.ImportReceiver
import java.util.UUID

class NotificationHandler (private  val uuid: UUID,private val context: Context): NotificationProvider {


    private val builder = NotificationCompat.Builder(context, IMPORT_CHANNEL_NAME)

     override fun createNotification(): Notification {
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
            R.drawable.baseline_cancel_24,
            context.getString(android.R.string.cancel),
            WorkManager.getInstance(context).createCancelPendingIntent(uuid),
        )
    }


    private val actionPause by lazy {
        NotificationCompat.Action(
            R.drawable.ic_action_pause,
            context.getString(R.string.pause),
            ImportReceiver.createPausePendingIntent(context, uuid),
        )
    }

    private val actionResume by lazy {
        NotificationCompat.Action(
            R.drawable.ic_action_resume,
            context.getString(R.string.resume),
            ImportReceiver.createResumePendingIntent(context, uuid),
        )
    }

    override fun updateState(
        currentState: Int,
        finalState: Int,
        importState: ImportState
    ): Notification {
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
}