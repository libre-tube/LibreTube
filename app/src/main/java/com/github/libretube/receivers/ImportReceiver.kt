package com.github.libretube.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import com.github.libretube.workers.ImportHandler
import java.util.UUID

class ImportReceiver(
    private val uuid: UUID,
    private val importHandler: ImportHandler,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_UUID)
        val parsedId = runCatching {
            UUID.fromString(id)
        }.getOrNull()

        if (parsedId != uuid) {
            return
        }

        when (intent?.action) {
            ACTION_IMPORT_PAUSE -> {
                importHandler.pause()
            }

            ACTION_IMPORT_RESUME -> {
                importHandler.resume()
            }
        }
    }

    companion object {
        private const val EXTRA_UUID = "uuid"
        private const val SCHEME = "workuid"

        private fun getPauseIntent(context: Context, uuid: UUID) =
            createIntent(context, ACTION_IMPORT_PAUSE, uuid)

        private fun getResumeIntent(context: Context, uuid: UUID) =
            createIntent(context, ACTION_IMPORT_RESUME, uuid)


        fun createPausePendingIntent(context: Context, uuid: UUID) =
            PendingIntentCompat.getBroadcast(
                context,
                0,
                getPauseIntent(context, uuid),
                0,
                false,
            )

        fun createResumePendingIntent(context: Context, uuid: UUID) =
            PendingIntentCompat.getBroadcast(
                context,
                0,
                getResumeIntent(
                    context,
                    uuid
                ),
                0,
                false,
            )


        fun createIntentFilter() = IntentFilter().apply {
            addAction(ACTION_IMPORT_RESUME)
            addAction(ACTION_IMPORT_PAUSE)
            addDataScheme(SCHEME)
        }

        private fun createIntent(context: Context, action: String, uuid: UUID) = Intent(action)
            .setData("$SCHEME://$uuid".toUri())
            .setPackage(context.packageName)
            .putExtra(EXTRA_UUID, uuid.toString())

        const val ACTION_IMPORT_RESUME =
            "com.github.libretube.receivers.ImportReceiver.ACTION_IMPORT_RESUME"
        const val ACTION_IMPORT_PAUSE =
            "com.github.libretube.receivers.ImportReceiver.ACTION_IMPORT_PAUSE"
    }
}