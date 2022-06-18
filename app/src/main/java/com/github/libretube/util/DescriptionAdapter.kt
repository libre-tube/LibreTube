package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.libretube.MainActivity
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.net.URL

/**
 * The [DescriptionAdapter] is used to show title, uploaderName and thumbnail of the video in the notification
 * Basic example [here](https://github.com/AnthonyMarkD/AudioPlayerSampleTest)
 */
class DescriptionAdapter(
    private val title: String,
    private val channelName: String,
    private val thumbnailUrl: String,
    private val context: Context
) :
    PlayerNotificationManager.MediaDescriptionAdapter {
    /**
     * sets the title of the notification
     */
    override fun getCurrentContentTitle(player: Player): CharSequence {
        // return controller.metadata.description.title.toString()
        return title
    }

    /**
     * overrides the action when clicking the notification
     */
    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        //  return controller.sessionActivity
        /**
         *  starts a new MainActivity Intent when the player notification is clicked
         *  it doesn't start a completely new MainActivity because the MainActivity's launchMode
         *  is set to "singleTop" in the AndroidManifest (important!!!)
         *  that's the only way to launch back into the previous activity (e.g. the player view
         */
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * the description of the notification (below the title)
     */
    override fun getCurrentContentText(player: Player): CharSequence? {
        // return controller.metadata.description.subtitle.toString()
        return channelName
    }

    /**
     * return the icon/thumbnail of the video
     */
    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        lateinit var bitmap: Bitmap

        /**
         * running on a new thread to prevent a NetworkMainThreadException
         */
        val thread = Thread {
            try {
                /**
                 * try to GET the thumbnail from the URL
                 */
                val inputStream = URL(thumbnailUrl).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }
        thread.start()
        thread.join()
        /**
         * returns the scaled bitmap if it got fetched successfully
         */
        return try {
            val resizedBitmap = Bitmap.createScaledBitmap(
                bitmap, 1080, 1080, false
            )
            resizedBitmap
        } catch (e: Exception) {
            null
        }
    }
}
