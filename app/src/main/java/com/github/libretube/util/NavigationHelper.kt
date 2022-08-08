package com.github.libretube.util

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.fragments.PlayerFragment

object NavigationHelper {
    fun navigateChannel(
        context: Context,
        channelId: String?
    ) {
        if (channelId != null) {
            val activity = context as MainActivity
            val bundle = bundleOf("channel_id" to channelId)
            activity.navController.navigate(R.id.channelFragment, bundle)
            try {
                val mainMotionLayout =
                    activity.findViewById<MotionLayout>(R.id.mainMotionLayout)
                if (mainMotionLayout.progress == 0.toFloat()) {
                    mainMotionLayout.transitionToEnd()
                    activity.findViewById<MotionLayout>(R.id.playerMotionLayout)
                        .transitionToEnd()
                }
            } catch (e: Exception) {
            }
        }
    }

    fun navigateVideo(
        context: Context,
        videoId: String?,
        playlistId: String? = null
    ) {
        if (videoId != null) {
            val bundle = Bundle()
            bundle.putString("videoId", videoId.toID())
            if (playlistId != null) bundle.putString("playlistId", playlistId)
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
    }

    fun navigatePlaylist(
        context: Context,
        playlistId: String?,
        isOwner: Boolean
    ) {
        if (playlistId != null) {
            val activity = context as MainActivity
            val bundle = Bundle()
            bundle.putString("playlist_id", playlistId)
            bundle.putBoolean("isOwner", isOwner)
            activity.navController.navigate(R.id.playlistFragment, bundle)
        }
    }
}
