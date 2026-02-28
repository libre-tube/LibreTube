package com.github.libretube.helpers

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.replace
import com.github.libretube.NavDirections
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.activities.AbstractPlayerHostActivity
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.activities.ZoomableImageActivity
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.fragments.AudioPlayerFragment
import com.github.libretube.ui.fragments.DownloadSortingOrder
import com.github.libretube.ui.fragments.DownloadTab
import com.github.libretube.ui.fragments.PlayerFragment
import com.github.libretube.util.PlayingQueue

object NavigationHelper {
    fun navigateChannel(context: Context, channelUrlOrId: String?) {
        if (channelUrlOrId == null) return

        // navigating to channels is only supported in the main activity, not in the no internet activity
        val activity = ContextHelper.tryUnwrapActivity<MainActivity>(context) ?: return
        activity.navController.navigate(NavDirections.openChannel(channelUrlOrId.toID()))
        try {
            // minimize player if currently expanded
            activity.runOnPlayerFragment {
                binding.playerMotionLayout.transitionToEnd()
                true
            }
            activity.minimizePlayerContainerLayout()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Navigate to the given video using the other provided parameters as well
     * If the audio only mode is enabled, play it in the background, else as a normal video
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun navigateVideo(
        context: Context,
        videoId: String?,
        playlistId: String? = null,
        channelId: String? = null,
        keepQueue: Boolean = false,
        timestamp: Long = 0,
        alreadyStarted: Boolean = false,
        forceVideo: Boolean = false,
        audioOnlyPlayerRequested: Boolean = false,
        downloadTab: DownloadTab? = null,
        downloadSortingOrder: DownloadSortingOrder? = null,
        shuffle: Boolean = false,
        isOffline: Boolean = false
    ) {
        if (videoId == null) return
        // TODO: refactor all related methods to take [PlayerData] objects as arguments instead
        // of all these overcomplex amount of arguments!

        // attempt to attach to the current media session first by using the corresponding
        // video/audio player instance
        val activity = ContextHelper.unwrapActivity<AbstractPlayerHostActivity>(context)
        val attachedToRunningPlayer = activity.runOnPlayerFragment {
            try {
                if (this.isOffline != isOffline) return@runOnPlayerFragment false

                PlayingQueue.clearAfterCurrent()
                this.playNextVideo(videoId.toID())

                if (audioOnlyPlayerRequested) {
                    // switch to audio only player
                    this.switchToAudioMode()
                } else {
                    // maximize player
                    this.binding.playerMotionLayout.transitionToStart()
                }

                true
            } catch (e: Exception) {
                this.onDestroy()
                false
            }
        }
        if (attachedToRunningPlayer) return

        val audioOnlyMode = PreferenceHelper.getBoolean(PreferenceKeys.AUDIO_ONLY_MODE, false)
        val attachedToRunningAudioPlayer = activity.runOnAudioPlayerFragment {
            if (this.isOffline != isOffline) return@runOnAudioPlayerFragment false

            PlayingQueue.clearAfterCurrent()
            this.playNextVideo(videoId.toID())

            if (!audioOnlyPlayerRequested && !audioOnlyMode) {
                // switch to video only player
                this.switchToVideoMode(videoId.toID())
            } else {
                // maximize player
                this.binding.playerMotionLayout.transitionToStart()
            }

            true
        }
        if (attachedToRunningAudioPlayer) return

        if (audioOnlyPlayerRequested || (audioOnlyMode && !forceVideo)) {
            // in contrast to the video player, the audio player doesn't start a media service on
            // its own!
            if (isOffline) {
                BackgroundHelper.playOnBackgroundOffline(
                    context,
                    videoId.toID(),
                    playlistId,
                    downloadTab!!,
                    shuffle,
                    downloadSortingOrder
                )
            } else {
                BackgroundHelper.playOnBackground(
                    context,
                    videoId.toID(),
                    timestamp,
                    playlistId,
                    channelId,
                    keepQueue
                )
            }

            openAudioPlayerFragment(context, offlinePlayer = isOffline, minimizeByDefault = true)
        } else {
            openVideoPlayerFragment(
                context,
                videoId.toID(),
                playlistId,
                channelId,
                keepQueue,
                timestamp,
                alreadyStarted,
                shuffle,
                isOffline,
                downloadTab,
                downloadSortingOrder
            )
        }
    }

    fun navigatePlaylist(context: Context, playlistUrlOrId: String?, playlistType: PlaylistType) {
        if (playlistUrlOrId == null) return

        val activity = ContextHelper.unwrapActivity<MainActivity>(context)
        activity.navController.navigate(
            NavDirections.openPlaylist(playlistUrlOrId.toID(), playlistType)
        )
    }

    /**
     * Start the audio player fragment
     */
    fun openAudioPlayerFragment(
        context: Context,
        offlinePlayer: Boolean = false,
        minimizeByDefault: Boolean = false
    ) {
        val activity = ContextHelper.unwrapActivity<BaseActivity>(context)
        activity.supportFragmentManager.commitNow {
            val args = bundleOf(
                IntentData.minimizeByDefault to minimizeByDefault,
                IntentData.offlinePlayer to offlinePlayer
            )
            replace<AudioPlayerFragment>(R.id.container, args = args)
        }
    }

    /**
     * Starts the video player fragment for an already existing med
     */
    fun openVideoPlayerFragment(
        context: Context,
        videoId: String,
        playlistId: String? = null,
        channelId: String? = null,
        keepQueue: Boolean = false,
        timestamp: Long = 0,
        alreadyStarted: Boolean = false,
        shuffle: Boolean = false,
        isOffline: Boolean = false,
        downloadTab: DownloadTab? = null,
        downloadSortingOrder: DownloadSortingOrder? = null,
    ) {
        val activity = ContextHelper.unwrapActivity<BaseActivity>(context)

        val playerData =
            PlayerData(videoId, playlistId, channelId, keepQueue, timestamp, shuffle, isOffline, downloadTab, downloadSortingOrder)
        val bundle = bundleOf(
            IntentData.playerData to playerData,
            IntentData.alreadyStarted to alreadyStarted,
        )
        activity.supportFragmentManager.commitNow {
            replace<PlayerFragment>(R.id.container, args = bundle)
        }
    }

    /**
     * Open a large, zoomable image preview
     */
    fun openImagePreview(context: Context, url: String) {
        val intent = Intent(context, ZoomableImageActivity::class.java)
        intent.putExtra(IntentData.bitmapUrl, url)
        context.startActivity(intent)
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
