package com.github.libretube.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.PendingIntentCompat
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.github.libretube.LibreTubeApp.Companion.PLAYER_CHANNEL_NAME
import com.github.libretube.R
import com.github.libretube.enums.NotificationId
import com.github.libretube.enums.PlayerEvent
import com.github.libretube.helpers.PlayerHelper
import com.google.common.collect.ImmutableList

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class NowPlayingNotification(
    private val context: Context,
    var notificationIntent: Intent = Intent(),
): MediaNotification.Provider {
    private val nProvider = DefaultMediaNotificationProvider.Builder(context)
        .setNotificationId(NotificationId.PLAYER_PLAYBACK.id)
        .setChannelId(PLAYER_CHANNEL_NAME)
        .setChannelName(R.string.player_channel_name)
        .build()

    private fun createCurrentContentIntent(): PendingIntent? {
        // starts a new MainActivity Intent when the player notification is clicked
        // it doesn't start a completely new MainActivity because the MainActivity's launchMode
        // is set to "singleTop" in the AndroidManifest (important!!!)
        // that's the only way to launch back into the previous activity (e.g. the player view)


        return PendingIntentCompat
            .getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT, false)
    }

    /**
     * Forward the action to the responsible notification owner (e.g. PlayerFragment)
     */
    private fun handlePlayerAction(action: PlayerEvent) {
        val intent = Intent(PlayerHelper.getIntentActionName(context))
            .setPackage(context.packageName)
            .putExtra(PlayerHelper.CONTROL_TYPE, action)
        context.sendBroadcast(intent)
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        createCurrentContentIntent()?.let { mediaSession.setSessionActivity(it) }
        nProvider.setSmallIcon(R.drawable.ic_launcher_lockscreen)
        return nProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        runCatching { handlePlayerAction(PlayerEvent.valueOf(action)) }
        return true
    }
}
