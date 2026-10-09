package com.github.libretube.ui.sheets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.WatchHistoryEntryMetadata
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.enums.menuoption.VideoMenuOption
import com.github.libretube.extensions.parcelable
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.DownloadHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData
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

    private suspend fun onOptionSelect(
        option: VideoMenuOption,
        videoId: String,
        playlistId: String?
    ) {
        when (option) {
            // Start the background mode
            VideoMenuOption.PLAY_ON_BACKGROUND -> {
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
            VideoMenuOption.ADD_TO_PLAYLIST -> {
                AddToPlaylistDialog().apply {
                    arguments = bundleOf(IntentData.videoInfo to streamItem)
                }.show(
                    parentFragmentManager,
                    AddToPlaylistDialog::class.java.name
                )
            }

            VideoMenuOption.DOWNLOAD -> {
                DownloadHelper.startDownloadDialog(
                    requireContext(),
                    parentFragmentManager,
                    videoId
                )
            }

            VideoMenuOption.SHARE -> {
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

            VideoMenuOption.PLAY_NEXT -> {
                PlayingQueue.addAsNext(streamItem)
            }

            VideoMenuOption.ADD_TO_QUEUE -> {
                PlayingQueue.add(streamItem)
            }

            VideoMenuOption.MARK_AS_WATCHED -> {
                withContext(Dispatchers.IO) {
                    if (PlayerHelper.watchHistoryEnabled) {
                        runCatching {
                            UserDataRepositoryHelper.userDataRepository.addToWatchHistory(
                                streamItem.toWatchHistoryEntry(
                                    Long.MAX_VALUE
                                )
                            )
                        }
                    }

                    val watchPosition = WatchHistoryEntryMetadata(
                        videoId = videoId,
                        addedDate = -1,
                        finished = true,
                        positionMillis = Long.MAX_VALUE
                    )
                    UserDataRepositoryHelper.userDataRepository
                        .updateWatchHistoryEntry(watchPosition)
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
                setFragmentResult(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    bundleOf(IS_VIDEO_WATCHED to true)
                )
            }

            VideoMenuOption.MARK_AS_UNWATCHED -> {
                withContext(Dispatchers.IO) {
                    try {
                        UserDataRepositoryHelper.userDataRepository.removeFromWatchHistory(
                            videoId
                        )
                    } catch (e: Exception) {
                        context?.toastFromMainDispatcher(e.message.orEmpty())
                    }
                }
                setFragmentResult(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    bundleOf(IS_VIDEO_WATCHED to false)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        streamItem = arguments?.parcelable(IntentData.streamItem)!!
        val playlistId = arguments?.getString(IntentData.playlistId)

        val videoId = streamItem.url?.toID() ?: return

        setTitle(streamItem.title)

        var optionsList = buildList {
            add(VideoMenuOption.ADD_TO_PLAYLIST)
            if (!streamItem.isLive) add(VideoMenuOption.DOWNLOAD)
            add(VideoMenuOption.SHARE)
        }

        setItems(optionsList.map { option ->
            option.toBottomSheetItem(::getString)
        }) { which ->
            onOptionSelect(optionsList[which], videoId, playlistId)
        }

        // these options are only available for other videos than the currently playing one
        if (PlayingQueue.getCurrent()?.url?.toID() != videoId) {
            getOptionsForNotActivePlayback(videoId) { addedOptions ->
                optionsList = addedOptions + optionsList

                setItems(optionsList.map { option ->
                    option.toBottomSheetItem(::getString)
                }) { which ->
                    onOptionSelect(optionsList[which], videoId, playlistId)
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    private fun getOptionsForNotActivePlayback(
        videoId: String,
        onOptionsList: (List<VideoMenuOption>) -> Unit
    ) {
        // List that stores the different menu options. In the future could be add more options here.
        val optionsList = mutableListOf(VideoMenuOption.PLAY_ON_BACKGROUND)

        // Check whether the player is running and add queue options
        if (PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.ONLINE) {
            optionsList += VideoMenuOption.PLAY_NEXT
            optionsList += VideoMenuOption.ADD_TO_QUEUE
        }

        if (!PlayerHelper.watchPositionsAny && !PlayerHelper.watchHistoryEnabled) {
            onOptionsList(optionsList)
            return
        }

        // show the mark as watched or unwatched option if watch positions are enabled
        lifecycleScope.launch(Dispatchers.IO) {
            val watchHistoryEntry =
                UserDataRepositoryHelper.userDataRepository.getFromWatchHistory(videoId)

            if (watchHistoryEntry != null) {
                optionsList += VideoMenuOption.MARK_AS_UNWATCHED
            }

            if (watchHistoryEntry?.metadata?.finished != true) {
                optionsList += VideoMenuOption.MARK_AS_WATCHED
            }

            withContext(Dispatchers.Main) {
                onOptionsList(optionsList)
            }
        }
    }

    companion object {
        const val VIDEO_OPTIONS_SHEET_REQUEST_KEY = "video_options_sheet_request_key"
        const val IS_VIDEO_WATCHED = "isVideoWatched"
    }
}
