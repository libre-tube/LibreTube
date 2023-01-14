package com.github.libretube.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.libretube.compat.ServiceCompat
import com.github.libretube.services.DownloadService

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == null) return

        val serviceIntent = Intent(context, DownloadService::class.java)
        serviceIntent.action = intent.action

        val id = intent.getIntExtra("id", -1)
        if (id == -1) return
        serviceIntent.putExtra("id", id)

        context?.let {
            ServiceCompat(it).startForeground(serviceIntent)
        }
    }

    companion object {
        const val ACTION_DOWNLOAD_RESUME =
            "com.github.libretube.receivers.NotificationReceiver.ACTION_DOWNLOAD_RESUME"
        const val ACTION_DOWNLOAD_PAUSE =
            "com.github.libretube.receivers.NotificationReceiver.ACTION_DOWNLOAD_PAUSE"
    }
}
