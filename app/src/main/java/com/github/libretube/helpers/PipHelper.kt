package com.github.libretube.helpers

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.PendingIntentCompat
import androidx.core.app.RemoteActionCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.res.ResourcesCompat
import com.github.libretube.R
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.extensions.seekBy
import com.github.libretube.extensions.togglePlayPauseState
import androidx.media3.common.Player

object PipHelper {

    private const val ACTION_MEDIA_CONTROL = "media_control"

    fun getIntentActionName(context: android.content.Context): String {
        return "${context.packageName}.$ACTION_MEDIA_CONTROL"
    }

    private fun getRemoteAction(
        activity: Activity,
        icon: IconCompat,
        @StringRes title: Int,
        event: PlayerEvent
    ): RemoteActionCompat {
        val intent = Intent(getIntentActionName(activity))
            .setPackage(activity.packageName)
            .putExtra(PlayerHelper.CONTROL_TYPE, event)
        val pendingIntent =
            PendingIntentCompat.getBroadcast(activity, event.ordinal, intent, 0, false)!!

        val text = activity.getString(title)
        return RemoteActionCompat(icon, text, text, pendingIntent)
    }

    private fun seekIconWithSpeed(resources: Resources, @DrawableRes resourceId: Int): IconCompat {
        val textSize = 15 * resources.displayMetrics.density
        val bitmap = ResourcesCompat.getDrawable(resources, resourceId, null)!!.toBitmap()
        ImageHelper.insertText(bitmap, PlayerHelper.seekIncrement.div(1000).toString(), 0.5f, 0.65f, textSize)
        return IconCompat.createWithBitmap(bitmap)
    }

    fun getPiPModeActions(activity: Activity, isPlaying: Boolean): List<RemoteActionCompat> {
        val audioModeAction = getRemoteAction(
            activity,
            IconCompat.createWithResource(activity, R.drawable.ic_headphones),
            R.string.background_mode,
            PlayerEvent.Background
        )

        val rewindAction = getRemoteAction(
            activity,
            seekIconWithSpeed(activity.resources, R.drawable.ic_rewind),
            R.string.rewind,
            PlayerEvent.Rewind
        )

        val playPauseAction = getRemoteAction(
            activity,
            IconCompat.createWithResource(activity, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            if (isPlaying) R.string.resume else R.string.pause,
            PlayerEvent.PlayPause
        )

        val skipNextAction = getRemoteAction(
            activity,
            IconCompat.createWithResource(activity, R.drawable.ic_next),
            R.string.play_next,
            PlayerEvent.Next
        )

        val forwardAction = getRemoteAction(
            activity,
            seekIconWithSpeed(activity.resources, R.drawable.ic_forward),
            R.string.forward,
            PlayerEvent.Forward
        )

        return if (PlayerHelper.alternativePiPControls) {
            listOf(audioModeAction, playPauseAction, skipNextAction)
        } else {
            listOf(rewindAction, playPauseAction, forwardAction)
        }
    }

    fun handlePlayerAction(player: Player, playerEvent: PlayerEvent): Boolean {
        return when (playerEvent) {
            PlayerEvent.PlayPause -> {
                player.togglePlayPauseState()
                true
            }
            PlayerEvent.Forward -> {
                player.seekBy(PlayerHelper.seekIncrement)
                true
            }
            PlayerEvent.Rewind -> {
                player.seekBy(-PlayerHelper.seekIncrement)
                true
            }
            else -> false
        }
    }
}
