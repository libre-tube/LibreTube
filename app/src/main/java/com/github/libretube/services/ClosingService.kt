package com.github.libretube.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.annotation.Nullable
import com.github.libretube.PLAYER_NOTIFICATION_ID

class ClosingService : Service() {
    private val TAG = "ClosingService"

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Handle application closing
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // destroy the player notification when the app gets destroyed
        val nManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancel(PLAYER_NOTIFICATION_ID)

        // Destroy the service
        stopSelf()
    }
}
