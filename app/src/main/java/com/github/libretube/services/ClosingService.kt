package com.github.libretube.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable

class ClosingService : Service() {
    private val TAG = "ClosingService"

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Handle application closing
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // destroy all notifications (especially the player notification)
        val nManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        Log.e(TAG, "closed")

        // Destroy the service
        stopSelf()
    }
}
