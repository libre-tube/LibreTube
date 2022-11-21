package com.github.libretube.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.ui.views.SingleViewTouchableMotionLayout

object NavigationHelper {
    fun navigateChannel(
        context: Context,
        channelId: String?
    ) {
        if (channelId == null) return

        val activity = context as MainActivity
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

    fun navigateVideo(
        context: Context,
        videoId: String?,
        playlistId: String? = null
    ) {
        if (videoId == null) return

        val bundle = Bundle()
        bundle.putString(IntentData.videoId, videoId.toID())
        if (playlistId != null) bundle.putString(IntentData.playlistId, playlistId)
        val frag = PlayerFragment()
        frag.arguments = bundle
        val activity = context as AppCompatActivity
        activity.supportFragmentManager.beginTransaction()
            .remove(PlayerFragment())
            .commit()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.container, frag)
            .commitNow()
    }

    fun navigatePlaylist(
        context: Context,
        playlistId: String?,
        playlistType: PlaylistType
    ) {
        if (playlistId == null) return

        val activity = context as MainActivity
        val bundle = Bundle()
        bundle.putString(IntentData.playlistId, playlistId)
        bundle.putSerializable(IntentData.playlistType, playlistType)
        activity.navController.navigate(R.id.playlistFragment, bundle)
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
