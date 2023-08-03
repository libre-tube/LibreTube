package com.github.libretube.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import androidx.core.content.getSystemService
import com.github.libretube.constants.PLAYER_NOTIFICATION_ID

class ClosingService : Service() {

    override fun onBind(intent: Intent?) = null

    // Handle application closing
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // destroy the player notification when the app gets destroyed
        getSystemService<NotificationManager>()!!.cancel(PLAYER_NOTIFICATION_ID)

        // Destroy the service
        stopSelf()
    }
}
