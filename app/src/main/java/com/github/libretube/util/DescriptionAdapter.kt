package com.github.libretube.util

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.net.URL

// used to show title and thumbnail of the video in the notification
class DescriptionAdapter(
    private val title: String,
    private val channelName: String,
    private val thumbnailUrl: String
) :
    PlayerNotificationManager.MediaDescriptionAdapter {
    override fun getCurrentContentTitle(player: Player): CharSequence {
        // return controller.metadata.description.title.toString()
        return title
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        //  return controller.sessionActivity
        return null
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        // return controller.metadata.description.subtitle.toString()
        return channelName
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        lateinit var bitmap: Bitmap
        val thread = Thread {
            try {
                // try to parse the thumbnailUrl to a Bitmap
                val inputStream = URL(thumbnailUrl).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        // return bitmap if initialized
        return try {
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
