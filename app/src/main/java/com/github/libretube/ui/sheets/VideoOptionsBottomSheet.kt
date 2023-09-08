package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.parcelable
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
class VideoOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var streamItem: StreamItem
    private var isCurrentlyPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val arguments = requireArguments()
        streamItem = arguments.parcelable(IntentData.streamItem)!!
        isCurrentlyPlaying = arguments.getBoolean(IntentData.isCurrentlyPlaying)

        val videoId = streamItem.url?.toID() ?: return

        setTitle(streamItem.title)

        val optionsList = mutableListOf<String>()
        if (!isCurrentlyPlaying) {
            optionsList += getOptionsForNotActivePlayback(videoId)
        }

        optionsList += listOf(
            getString(R.string.addToPlaylist),
            getString(R.string.download),
            getString(R.string.share)
        )

        setSimpleItems(optionsList) { which ->
            when (optionsList[which]) {
                // Start the background mode
                getString(R.string.playOnBackground) -> {
                    BackgroundHelper.playOnBackground(requireContext(), videoId)
                    NavigationHelper.startAudioPlayer(requireContext(), true)
                }
                // Add Video to Playlist Dialog
                getString(R.string.addToPlaylist) -> {
                    val newAddToPlaylistDialog = AddToPlaylistDialog()
                    newAddToPlaylistDialog.arguments = bundleOf(IntentData.videoId to videoId)
                    newAddToPlaylistDialog.show(
                        parentFragmentManager,
                        AddToPlaylistDialog::class.java.name
                    )
                }

                getString(R.string.download) -> {
                    val newFragment = DownloadDialog()
                    newFragment.arguments = bundleOf(IntentData.videoId to videoId)
                    newFragment.show(parentFragmentManager, DownloadDialog::class.java.name)
                }

                getString(R.string.share) -> {
                    val bundle = bundleOf(
                        IntentData.id to videoId,
                        IntentData.shareObjectType to ShareObjectType.VIDEO,
                        IntentData.shareData to ShareData(currentVideo = streamItem.title)
                    )
                    val newShareDialog = ShareDialog()
                    newShareDialog.arguments = bundle
                    // using parentFragmentManager is important here
                    newShareDialog.show(parentFragmentManager, ShareDialog::class.java.name)
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
                        fragment?.feedAdapter?.removeItemById(videoId)
                    }
                    setFragmentResult(VIDEO_OPTIONS_SHEET_REQUEST_KEY, bundleOf())
                }

                getString(R.string.mark_as_unwatched) -> {
                    withContext(Dispatchers.IO) {
                        DatabaseHolder.Database.watchPositionDao().deleteByVideoId(videoId)
                        DatabaseHolder.Database.watchHistoryDao().deleteByVideoId(videoId)
                    }
                    setFragmentResult(VIDEO_OPTIONS_SHEET_REQUEST_KEY, bundleOf())
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    private fun getOptionsForNotActivePlayback(videoId: String): List<String> {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(
            getString(R.string.playOnBackground)
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
                watchPositionEntry.position < streamItem.duration!! * 1000 * 0.9
            ) {
                optionsList += getString(R.string.mark_as_watched)
            }

            if (watchHistoryEntry != null || watchPositionEntry != null) {
                optionsList += getString(R.string.mark_as_unwatched)
            }
        }

        return optionsList
    }

    companion object {
        const val VIDEO_OPTIONS_SHEET_REQUEST_KEY = "video_options_sheet_request_key"
    }
}
