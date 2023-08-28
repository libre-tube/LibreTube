package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.BackgroundHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.DownloadDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.SubscriptionsFragment
import com.github.libretube.util.PlayingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [streamItem] to load the content from the right video.
 */
class VideoOptionsBottomSheet(
    private val streamItem: StreamItem,
    private val onVideoChanged: () -> Unit = {}
) : BaseBottomSheet() {
    private val shareData = ShareData(currentVideo = streamItem.title)
    override fun onCreate(savedInstanceState: Bundle?) {
        val videoId = streamItem.url?.toID() ?: return
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

        // show the mark as watched or unwatched option if watch positions are enabled
        if (PlayerHelper.watchPositionsVideo || PlayerHelper.watchHistoryEnabled) {
            val watchPositionEntry = runBlocking(Dispatchers.IO) {
                DatabaseHolder.Database.watchPositionDao().findById(videoId)
            }
            val watchHistoryEntry = runBlocking(Dispatchers.IO) {
                DatabaseHolder.Database.watchHistoryDao().findById(videoId)
            }

            if (streamItem.duration == null ||
                watchPositionEntry == null ||
                watchPositionEntry.position < streamItem.duration * 1000 * 0.9
            ) {
                optionsList += getString(R.string.mark_as_watched)
            }

            if (watchHistoryEntry != null || watchPositionEntry != null) {
                optionsList += getString(R.string.mark_as_unwatched)
            }
        }

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // Start the background mode
                getString(R.string.playOnBackground) -> {
                    BackgroundHelper.playOnBackground(requireContext(), videoId)
                    NavigationHelper.startAudioPlayer(requireContext(), true)
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
                    PlayingQueue.addAsNext(streamItem)
                }

                getString(R.string.add_to_queue) -> {
                    PlayingQueue.add(streamItem)
                }

                getString(R.string.mark_as_watched) -> {
                    val watchPosition = WatchPosition(videoId, Long.MAX_VALUE)
                    withContext(Dispatchers.IO) {
                        DatabaseHolder.Database.watchPositionDao().insert(watchPosition)
                        if (!PlayerHelper.watchHistoryEnabled) return@withContext
                        // add video to watch history
                        DatabaseHelper.addToWatchHistory(videoId, streamItem)
                    }
                    if (PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)) {
                        // get the host fragment containing the current fragment
                        val navHostFragment = (context as MainActivity).supportFragmentManager
                            .findFragmentById(R.id.fragment) as NavHostFragment?
                        // get the current fragment
                        val fragment = navHostFragment?.childFragmentManager?.fragments
                            ?.firstOrNull() as? SubscriptionsFragment
                        fragment?.subscriptionsAdapter?.removeItemById(videoId)
                    }
                    onVideoChanged()
                }

                getString(R.string.mark_as_unwatched) -> {
                    withContext(Dispatchers.IO) {
                        DatabaseHolder.Database.watchPositionDao().deleteByVideoId(videoId)
                        DatabaseHolder.Database.watchHistoryDao().deleteByVideoId(videoId)
                    }
                    onVideoChanged()
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}
