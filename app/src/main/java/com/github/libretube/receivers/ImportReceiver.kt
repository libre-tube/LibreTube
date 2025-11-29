package com.github.libretube.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import com.github.libretube.constants.IntentData
import com.github.libretube.handler.ImportHandler
import com.github.libretube.services.DownloadService
import com.github.libretube.ui.activities.MainActivity
import java.util.UUID

class ImportReceiver(
    private val importHandler: ImportHandler,
    ) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_IMPORT_PAUSE -> {
                importHandler.pause()
            }
            ACTION_IMPORT_RESUME ->{
                importHandler.resume()
            }
            ACTION_IMPORT_STOP ->{
                importHandler.cancel()
            }
        }
    }

    companion object {

        fun getPauseIntent(context: Context) = createIntent(context, ACTION_IMPORT_PAUSE)

        fun getResumeIntent(context: Context) = createIntent(context, ACTION_IMPORT_RESUME)

        fun getCancelIntent(context: Context) = createIntent(context, ACTION_IMPORT_STOP)


        fun createPausePendingIntent(context: Context) = PendingIntentCompat.getBroadcast(
            context,
            0,
            getPauseIntent(context),
            0,
            false,
        )

        fun createResumePendingIntent(context: Context) =
            PendingIntentCompat.getBroadcast(
                context,
                0,
                getResumeIntent(
                    context
                ),
                0,
                false,
            )

        fun createCancelIntent(context: Context) =
            PendingIntentCompat.getBroadcast(
                context,
                0,
                getCancelIntent(
                    context
                ),
                0,
                false,
            )

        fun createIntentFilter() = IntentFilter().apply {
            addAction(ACTION_IMPORT_RESUME)
            addAction(ACTION_IMPORT_PAUSE)
            addAction(ACTION_IMPORT_STOP)
        }

        private fun createIntent(context: Context, action: String) = Intent(action)
            .setPackage(context.packageName)

        const val ACTION_IMPORT_RESUME =
            "com.github.libretube.receivers.ImportReceiver.ACTION_IMPORT_RESUME"
        const val ACTION_IMPORT_PAUSE =
            "com.github.libretube.receivers.ImportReceiver.ACTION_IMPORT_PAUSE"
        const val ACTION_IMPORT_STOP =
            "com.github.libretube.receivers.ImportReceiver.ACTION_IMPORT_CANCEL"
    }
}