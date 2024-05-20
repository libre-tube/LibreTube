package com.github.libretube.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.content.getSystemService

class OnClearFromRecentService : Service() {
    private var nManager: NotificationManager? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        nManager = getSystemService<NotificationManager>()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        nManager?.cancel(PLAYER_NOTIFICATION_ID)
        stopSelf()
    }

    companion object {
        private const val PLAYER_NOTIFICATION_ID = 1
    }
}
