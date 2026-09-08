package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
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
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.SubscriptionsFragment
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PlayingQueueMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog with different options for a selected video.
 *
 * Needs the [streamItem] to load the content from the right video.
 */
class VideoOptionsBottomSheet : BaseBottomSheet() {
    private lateinit var streamItem: StreamItem

    /** the currently rendered options; the click listener always resolves against this field */
    private var optionsList: List<Int> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        streamItem = arguments?.parcelable(IntentData.streamItem)!!
        val playlistId = arguments?.getString(IntentData.playlistId)

        val videoId = streamItem.url?.toID() ?: return

        setTitle(streamItem.title)

        val isActivePlayback = PlayingQueue.getCurrent()?.url?.toID() == videoId

        // synchronous, DB-free base options
        val baseOptions = mutableListOf<Int>()
        // these options are only available for other videos than the currently playing one
        if (!isActivePlayback) {
            baseOptions += getPlaybackQueueOptions()
        }

        baseOptions += listOf(R.string.addToPlaylist, R.string.download, R.string.share)
        if (streamItem.isLive) baseOptions.remove(R.string.download)

        if (!isActivePlayback && (PlayerHelper.watchPositionsAny || PlayerHelper.watchHistoryEnabled)) {
            // the mark as watched/unwatched options depend on watch-history / position state which
            // lives in the DB; fetch it async (it used to block the main thread with runBlocking)
            lifecycleScope.launch {
                val watchOptions = getWatchStatusOptions(videoId)
                val insertIndex = baseOptions.indexOf(R.string.addToPlaylist)
                optionsList = baseOptions.take(insertIndex) + watchOptions + baseOptions.drop(insertIndex)
                renderOptions(videoId, playlistId)
            }
        } else {
            optionsList = baseOptions
            renderOptions(videoId, playlistId)
        }

        super.onCreate(savedInstanceState)
    }

    private fun renderOptions(videoId: String, playlistId: String?) {
        val visibleOptions = optionsList
        setSimpleItems(visibleOptions.map { getString(it) }) { which ->
            when (visibleOptions[which]) {
                // Start the background mode
                R.string.playOnBackground -> {
                    NavigationHelper.navigateVideo(
                        requireContext(),
                        playerData = PlayerData(
                            videoId = videoId,
                            playlistId = playlistId,
                        ),
                        audioOnlyPlayerRequested = true
                    )
                }
                // Add Video to Playlist Dialog
                R.string.addToPlaylist -> {
                    AddToPlaylistDialog().apply {
                        arguments = bundleOf(IntentData.videoInfo to streamItem)
                    }.show(
                        parentFragmentManager,
                        AddToPlaylistDialog::class.java.name
                    )
                }

                R.string.download -> {
                    DownloadHelper.startDownloadDialog(
                        requireContext(),
                        parentFragmentManager,
                        videoId
                    )
                }

                R.string.share -> {
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

                R.string.play_next -> {
                    PlayingQueue.addAsNext(streamItem)
                }

                R.string.add_to_queue -> {
                    PlayingQueue.add(streamItem)
                }

                R.string.mark_as_watched -> {
                    val watchPosition = WatchPosition(videoId, Long.MAX_VALUE)
                    withContext(Dispatchers.IO) {
                        DatabaseHolder.Database.watchPositionDao().insert(watchPosition)

                        if (PlayerHelper.watchHistoryEnabled) {
                            DatabaseHelper.addToWatchHistory(streamItem.toWatchHistoryItem(videoId))
                        }
                    }
                    if (PreferenceHelper.getBoolean(PreferenceKeys.HIDE_WATCHED_FROM_FEED, false)) {
                        // get the host fragment containing the current fragment
                        val navHostFragment = (context as MainActivity).supportFragmentManager
                            .findFragmentById(R.id.fragment) as NavHostFragment?
                        // get the current fragment
                        val fragment = navHostFragment?.childFragmentManager?.fragments
                            ?.firstOrNull() as? SubscriptionsFragment
                        fragment?.removeItem(videoId)
                    }
                    setFragmentResult(VIDEO_OPTIONS_SHEET_REQUEST_KEY, bundleOf())
                }

                R.string.mark_as_unwatched -> {
                    withContext(Dispatchers.IO) {
                        DatabaseHolder.Database.watchPositionDao().deleteByVideoId(videoId)
                        DatabaseHolder.Database.watchHistoryDao().deleteByVideoId(videoId)
                    }
                    setFragmentResult(VIDEO_OPTIONS_SHEET_REQUEST_KEY, bundleOf())
                }
            }
        }
    }

    private fun getPlaybackQueueOptions(): List<Int> {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(R.string.playOnBackground)

        // Check whether the player is running and add queue options
        if (PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.ONLINE) {
            optionsList += R.string.play_next
            optionsList += R.string.add_to_queue
        }

        return optionsList
    }

    /**
     * Computes the mark as watched / unwatched options from the local watch-history and
     * watch-position databases. DB-only, so it runs off the main thread.
     */
    private suspend fun getWatchStatusOptions(videoId: String): List<Int> {
        val optionsList = mutableListOf<Int>()

        val (watchHistoryEntry, positionRaw) = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.watchHistoryDao().findById(videoId) to
                DatabaseHelper.getWatchPosition(videoId)
        }
        val position = positionRaw ?: 0
        val isCompleted = DatabaseHelper.isVideoWatched(position, streamItem.duration ?: 0)
        if (position != 0L || watchHistoryEntry != null) {
            optionsList += R.string.mark_as_unwatched
        }

        if (!isCompleted || watchHistoryEntry == null) {
            optionsList += R.string.mark_as_watched
        }

        return optionsList
    }

    companion object {
        const val VIDEO_OPTIONS_SHEET_REQUEST_KEY = "video_options_sheet_request_key"
    }
}
