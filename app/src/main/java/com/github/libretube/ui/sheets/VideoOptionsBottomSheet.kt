package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.SubscriptionsFragment
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [videoId] to load the content from the right video.
 */
class VideoOptionsBottomSheet(
    private val videoId: String,
    videoName: String
) : BaseBottomSheet() {
    private val shareData = ShareData(currentVideo = videoName)
    override fun onCreate(savedInstanceState: Bundle?) {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            getString(R.string.playOnBackground),
            getString(R.string.addToPlaylist),
            getString(R.string.download),
            getString(R.string.share)
        )

        // Check whether the player is running and add queue options
        if (PlayingQueue.isNotEmpty()) {
            optionsList += getString(R.string.play_next)
            optionsList += getString(R.string.add_to_queue)
        }

        // show the mark as watched option if watch positions are enabled
        if (PlayerHelper.watchPositionsVideo) {
            optionsList += getString(R.string.mark_as_watched)
        }

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // Start the background mode
                getString(R.string.playOnBackground) -> {
                    BackgroundHelper.playOnBackground(requireContext(), videoId)
                }
                // Add Video to Playlist Dialog
                getString(R.string.addToPlaylist) -> {
                    AddToPlaylistDialog(videoId).show(
                        parentFragmentManager,
                        AddToPlaylistDialog::class.java.name
                    )
                }
                getString(R.string.download) -> {
                    val downloadDialog = DownloadDialog(videoId)
                    downloadDialog.show(parentFragmentManager, DownloadDialog::class.java.name)
                }
                getString(R.string.share) -> {
                    val shareDialog = ShareDialog(videoId, ShareObjectType.VIDEO, shareData)
                    // using parentFragmentManager is important here
                    shareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
                }
                getString(R.string.play_next) -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            PlayingQueue.addAsNext(
                                RetrofitInstance.api.getStreams(videoId)
                                    .toStreamItem(videoId)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                getString(R.string.add_to_queue) -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            PlayingQueue.add(
                                RetrofitInstance.api.getStreams(videoId)
                                    .toStreamItem(videoId)
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                getString(R.string.mark_as_watched) -> {
                    val watchPosition = WatchPosition(videoId, Long.MAX_VALUE)
                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseHolder.Database.watchPositionDao().insertAll(listOf(watchPosition))
                    }
                    if (PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)) {
                        // get the host fragment containing the current fragment
                        val navHostFragment =
                            (context as MainActivity).supportFragmentManager
                                .findFragmentById(R.id.fragment) as NavHostFragment?
                        // get the current fragment
                        val fragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
                        (fragment as? SubscriptionsFragment)?.subscriptionsAdapter?.removeItemById(
                            videoId
                        )
                    }
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}
