package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.WatchHistoryEntryMetadata
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.repo.UserDataRepositoryHelper
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

    private suspend fun onOptionSelect(@StringRes option: Int, videoId: String, playlistId: String?) {
        when (option) {
            // Start the background mode
            R.string.playOnBackground -> {
                NavigationHelper.navigateVideo(
                    requireContext(),
                    videoId = videoId,
                    playlistId = playlistId,
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
                val watchPosition = WatchHistoryEntryMetadata(
                    videoId = videoId,
                    addedDate = -1,
                    finished = true,
                    positionMillis = Long.MAX_VALUE
                )
                withContext(Dispatchers.IO) {
                    UserDataRepositoryHelper.userDataRepository
                        .updateWatchHistoryEntry(watchPosition)

                    if (PlayerHelper.watchHistoryEnabled) {
                        DatabaseHelper.addToWatchHistory(streamItem)
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
                    try {
                        UserDataRepositoryHelper.userDataRepository.removeFromWatchHistory(
                            videoId
                        )
                    } catch (e: Exception) {
                        context?.toastFromMainDispatcher(e.message.orEmpty())
                    }
                }
                setFragmentResult(VIDEO_OPTIONS_SHEET_REQUEST_KEY, bundleOf())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        streamItem = arguments?.parcelable(IntentData.streamItem)!!
        val playlistId = arguments?.getString(IntentData.playlistId)

        val videoId = streamItem.url?.toID() ?: return

        setTitle(streamItem.title)

        val optionsList = mutableListOf(R.string.addToPlaylist, R.string.download, R.string.share)
        if (streamItem.isLive) optionsList.remove(R.string.download)

        // these options are only available for other videos than the currently playing one
        if (PlayingQueue.getCurrent()?.url?.toID() != videoId) {
            getOptionsForNotActivePlayback(videoId) { addedOptions ->
                val optionsList = optionsList + addedOptions

                setSimpleItems(optionsList.map { getString(it) }) { which ->
                    onOptionSelect(optionsList[which], videoId, playlistId)
                }
            }
        } else {
            setSimpleItems(optionsList.map { getString(it) }) { which ->
                onOptionSelect(optionsList[which], videoId, playlistId)
            }
        }

        super.onCreate(savedInstanceState)
    }

    private fun getOptionsForNotActivePlayback(videoId: String, onOptionsList: (List<Int>) -> Unit) {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(R.string.playOnBackground)

        // Check whether the player is running and add queue options
        if (PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.ONLINE) {
            optionsList += R.string.play_next
            optionsList += R.string.add_to_queue
        }

        // show the mark as watched or unwatched option if watch positions are enabled
        if (PlayerHelper.watchPositionsAny || PlayerHelper.watchHistoryEnabled) {
            lifecycleScope.launch(Dispatchers.IO) {
                val watchHistoryEntry =
                    UserDataRepositoryHelper.userDataRepository.getFromWatchHistory(videoId)

                if (watchHistoryEntry != null) {
                    optionsList += R.string.mark_as_unwatched
                }

                if (watchHistoryEntry == null) {
                    optionsList += R.string.mark_as_watched
                }

                withContext(Dispatchers.Main) {
                    onOptionsList(optionsList)
                }
            }
        }

        onOptionsList(optionsList)
    }

    companion object {
        const val VIDEO_OPTIONS_SHEET_REQUEST_KEY = "video_options_sheet_request_key"
    }
}
