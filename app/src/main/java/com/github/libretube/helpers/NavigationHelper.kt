package com.github.libretube.helpers

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.views.SingleViewTouchableMotionLayout

object NavigationHelper {
    private val handler = Handler(Looper.getMainLooper())

    fun navigateChannel(
        context: Context,
        channelId: String?,
    ) {
        if (channelId == null) return

        val activity = ContextHelper.unwrapActivity(context)
        val bundle = bundleOf(IntentData.channelId to channelId)
        activity.navController.navigate(R.id.channelFragment, bundle)
        try {
            if (activity.binding.mainMotionLayout.progress == 0.toFloat()) {
                activity.binding.mainMotionLayout.transitionToEnd()
                activity.findViewById<SingleViewTouchableMotionLayout>(R.id.playerMotionLayout)
                    .transitionToEnd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Navigate to the given video using the other provided parameters as well
     * If the audio only mode is enabled, play it in the background, else as a normal video
     */
    fun navigateVideo(
        context: Context,
        videoId: String?,
        playlistId: String? = null,
        channelId: String? = null,
        keepQueue: Boolean = false,
        timeStamp: Long? = null,
        forceVideo: Boolean = false,
    ) {
        if (videoId == null) return
        BackgroundHelper.stopBackgroundPlay(context)

        if (PreferenceHelper.getBoolean(PreferenceKeys.AUDIO_ONLY_MODE, false) && !forceVideo) {
            BackgroundHelper.playOnBackground(
                context,
                videoId.toID(),
                timeStamp,
                playlistId,
                channelId,
                keepQueue,
            )
            handler.postDelayed(500) {
                startAudioPlayer(context)
            }
            return
        }

        val bundle = bundleOf(
            IntentData.videoId to videoId.toID(),
            IntentData.playlistId to playlistId,
            IntentData.channelId to channelId,
            IntentData.keepQueue to keepQueue,
            IntentData.timeStamp to timeStamp,
        )

        val activity = ContextHelper.unwrapActivity(context)
        activity.supportFragmentManager.commitNow {
            replace<PlayerFragment>(R.id.container, args = bundle)
        }
    }

    fun navigatePlaylist(
        context: Context,
        playlistId: String?,
        playlistType: PlaylistType,
    ) {
        if (playlistId == null) return

        val activity = ContextHelper.unwrapActivity(context)
        val bundle = bundleOf(
            IntentData.playlistId to playlistId,
            IntentData.playlistType to playlistType,
        )
        activity.navController.navigate(R.id.playlistFragment, bundle)
    }

    /**
     * Start the audio player fragment
     */
    fun startAudioPlayer(context: Context) {
        val activity = ContextHelper.unwrapActivity(context)
        activity.supportFragmentManager.commitNow {
            replace<AudioPlayerFragment>(R.id.container)
        }
    }

    /**
     * Needed due to different MainActivity Aliases because of the app icons
     */
    fun restartMainActivity(context: Context) {
        // kill player notification
        context.getSystemService<NotificationManager>()!!.cancelAll()
        // start a new Intent of the app
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        // kill the old application
        Process.killProcess(Process.myPid())
    }
}
