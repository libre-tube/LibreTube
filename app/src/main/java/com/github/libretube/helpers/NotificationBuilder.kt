package com.github.libretube.helpers

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.libretube.R

class NotificationBuilder {

    companion object{
        val instance: NotificationBuilder by lazy { NotificationBuilder() }
    }

    fun CreateNotification(context: Context): Notification{
        val notificationManager = NotificationManagerCompat.from(context)
        val builder = NotificationCompat.Builder(context, "progress_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading…")
            .setContentText("Hello World")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        return builder.build()
    }


}