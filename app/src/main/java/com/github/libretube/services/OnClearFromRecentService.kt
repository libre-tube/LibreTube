package com.github.libretube.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.content.getSystemService
import com.github.libretube.util.NowPlayingNotification.Companion.PLAYER_NOTIFICATION_ID

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
}
