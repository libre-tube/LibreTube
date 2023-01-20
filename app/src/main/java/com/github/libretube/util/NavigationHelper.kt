package com.github.libretube.util

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.views.SingleViewTouchableMotionLayout

object NavigationHelper {
    private val handler = Handler(Looper.getMainLooper())

    fun navigateChannel(
        context: Context,
        channelId: String?
    ) {
        if (channelId == null) return

        val activity = unwrap(context)
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

    private fun unwrap(context: Context): MainActivity {
        var correctContext: Context? = context
        while (correctContext !is MainActivity && correctContext is ContextWrapper) {
            correctContext = correctContext.baseContext
        }
        return correctContext as MainActivity
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
        forceVideo: Boolean = false
    ) {
        if (videoId == null) return

        if (PreferenceHelper.getBoolean(PreferenceKeys.AUDIO_ONLY_MODE, false) && !forceVideo) {
            BackgroundHelper.stopBackgroundPlay(context)
            BackgroundHelper.playOnBackground(
                context,
                videoId.toID(),
                timeStamp,
                playlistId,
                channelId,
                keepQueue
            )
            handler.postDelayed({
                startAudioPlayer(context)
            }, 500)
            return
        }

        val bundle = bundleOf(
            IntentData.videoId to videoId.toID(),
            IntentData.playlistId to playlistId,
            IntentData.channelId to channelId,
            IntentData.keepQueue to keepQueue,
            IntentData.timeStamp to timeStamp
        )

        val activity = context as AppCompatActivity
        activity.supportFragmentManager.beginTransaction()
            .remove(PlayerFragment())
            .commit()
        activity.supportFragmentManager.beginTransaction()
            .replace(
                R.id.container,
                PlayerFragment().apply {
                    arguments = bundle
                }
            )
            .commitNow()
    }

    fun navigatePlaylist(
        context: Context,
        playlistId: String?,
        playlistType: PlaylistType
    ) {
        if (playlistId == null) return

        val activity = unwrap(context)
        val bundle = bundleOf(
            IntentData.playlistId to playlistId,
            IntentData.playlistType to playlistType
        )
        activity.navController.navigate(R.id.playlistFragment, bundle)
    }

    /**
     * Start the audio player fragment
     */
    fun startAudioPlayer(context: Context) {
        val activity = unwrap(context)
        activity.navController.navigate(R.id.audioPlayerFragment)
    }

    /**
     * Needed due to different MainActivity Aliases because of the app icons
     */
    fun restartMainActivity(context: Context) {
        // kill player notification
        val nManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        // start a new Intent of the app
        val pm: PackageManager = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        // kill the old application
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
