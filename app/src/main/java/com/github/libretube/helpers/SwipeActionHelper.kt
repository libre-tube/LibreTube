package com.github.libretube.helpers

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchPosition
import com.github.libretube.enums.ShareObjectType
import com.github.libretube.extensions.SwipeOptions
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.obj.ShareData
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.activities.MainActivity
import com.github.libretube.ui.dialogs.AddToPlaylistDialog
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.SubscriptionsFragment
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.PlayingQueueMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SwipeActionHelper(
    private val context: Context,
    private val childFragmentManager: FragmentManager,
    private val iconScale: Double
) {
    fun getSwipeOptions(
        swipePreference: String,
        swipePlaylist: String,
        getVideoAt: (Int) -> StreamItem?
    ): SwipeOptions? {
        return when (swipePreference) {
            "addToPlaylist" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_playlist_add,
                iconScale = iconScale,
                onSwipeListener = { position ->
                    val video = getVideoAt(position) ?: return@SwipeOptions
                    if (swipePlaylist != "manual") {
                        CoroutineScope(Dispatchers.IO).launch {
                            runCatching {
                                PlaylistsHelper.addToPlaylist(swipePlaylist, video)
                                PlaylistsHelper.getPlaylist(swipePlaylist)
                            }.onSuccess {
                                context.toastFromMainDispatcher(
                                    context.getString(R.string.added_to_playlist).format(it.name),
                                    Toast.LENGTH_SHORT
                                )
                            }.onFailure {
                                context.toastFromMainDispatcher(
                                    context.getString(R.string.unknown_error),
                                    Toast.LENGTH_SHORT
                                )
                            }
                        }
                    } else {
                        AddToPlaylistDialog().apply {
                            arguments =
                                Bundle().apply { putParcelable(IntentData.videoInfo, video) }
                        }.show(
                            childFragmentManager,
                            AddToPlaylistDialog::class.java.name
                        )
                    }
                }
            )

            "download" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_download,
                iconScale = iconScale,
                onSwipeListener = { position ->
                    val video = getVideoAt(position) ?: return@SwipeOptions
                    val videoId = video.url?.toID() ?: return@SwipeOptions
                    DownloadHelper.startDownloadDialog(context, childFragmentManager, videoId)
                }
            )

            "playOnBackground" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_play,
                iconScale = iconScale,
                getDisableSwipe = getDisableSwipe@{ position ->
                    val video = getVideoAt(position) ?: return@getDisableSwipe true
                    isCurrentVideoPlaying(video)
                },
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener
                    val videoId = video.url?.toID() ?: return@onSwipeListener

                    NavigationHelper.navigateVideo(
                        context,
                        playerData = PlayerData(
                            videoId = videoId,
                            playlistId = null,
                        ),
                        audioOnlyPlayerRequested = true
                    )
                }
            )

            "share" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_share,
                iconScale = iconScale,
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener
                    val videoId = video.url?.toID() ?: return@onSwipeListener

                    ShareDialog().apply {
                        arguments = Bundle().apply {
                            putString(IntentData.id, videoId)
                            putParcelable(IntentData.shareObjectType, ShareObjectType.VIDEO)
                            putParcelable(
                                IntentData.shareData,
                                ShareData(currentVideo = video.title)
                            )
                        }
                    }.show(childFragmentManager, ShareDialog::class.java.name)
                }
            )

            "playNext" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_queue,
                iconScale = iconScale,
                getDisableSwipe = getDisableSwipe@{ position ->
                    val video = getVideoAt(position) ?: return@getDisableSwipe true
                    !isQueueActive() || isCurrentVideoPlaying(video)
                },
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener

                    PlayingQueue.addAsNext(video)
                    Toast.makeText(context, context.getString(R.string.play_next) + ": ${video.title}", Toast.LENGTH_SHORT).show()
                }
            )

            "addToQueue" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_queue,
                iconScale = iconScale,
                getDisableSwipe = getDisableSwipe@{ position ->
                    val video = getVideoAt(position) ?: return@getDisableSwipe true
                    !isQueueActive() || isCurrentVideoPlaying(video)
                },
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener

                    PlayingQueue.add(video)
                    Toast.makeText(context, context.getString(R.string.add_to_queue) + ": ${video.title}", Toast.LENGTH_SHORT).show()
                }
            )

            "markAsWatched" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_done,
                iconScale = iconScale,
                getDisableSwipe = getDisableSwipe@{ position ->
                    val video = getVideoAt(position) ?: return@getDisableSwipe true
                    val videoId = video.url?.toID() ?: return@getDisableSwipe true

                    val position = DatabaseHelper.getWatchPositionBlocking(videoId) ?: 0
                    val isCompleted = DatabaseHelper.isVideoWatched(position, video.duration ?: 0)

                    isCurrentVideoPlaying(video) || (hasWatchPositionEntry(video) && isCompleted)
                },
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener
                    val videoId = video.url?.toID() ?: return@onSwipeListener

                    val watchPosition = WatchPosition(videoId, Long.MAX_VALUE)
                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseHolder.Database.watchPositionDao().insert(watchPosition)

                        if (PlayerHelper.watchHistoryEnabled) {
                            DatabaseHelper.addToWatchHistory(video.toWatchHistoryItem(videoId))
                        }
                        context.toastFromMainDispatcher(context.getString(R.string.mark_as_watched) + ": ${video.title}", Toast.LENGTH_SHORT)
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
                }
            )

            "markAsUnwatched" -> SwipeOptions(
                clamp = true,
                icon = R.drawable.ic_rotating_circle,
                iconScale = iconScale,
                getDisableSwipe = getDisableSwipe@{ position ->
                    val video = getVideoAt(position) ?: return@getDisableSwipe true
                    val videoId = video.url?.toID() ?: return@getDisableSwipe true

                    val position = DatabaseHelper.getWatchPositionBlocking(videoId) ?: 0

                    isCurrentVideoPlaying(video) || (!hasWatchPositionEntry(video) && position == 0L)
                },
                onSwipeListener = onSwipeListener@{ position ->
                    val video = getVideoAt(position) ?: return@onSwipeListener
                    val videoId = video.url?.toID() ?: return@onSwipeListener

                    CoroutineScope(Dispatchers.IO).launch {
                        DatabaseHolder.Database.watchPositionDao().deleteByVideoId(videoId)
                        DatabaseHolder.Database.watchHistoryDao().deleteByVideoId(videoId)
                        context.toastFromMainDispatcher(context.getString(R.string.mark_as_unwatched) + ": ${video.title}", Toast.LENGTH_SHORT)
                    }
                }
            )

            else -> null
        }
    }

    private fun isCurrentVideoPlaying(video: StreamItem): Boolean =
        PlayingQueue.getCurrent()?.url?.toID() == video.url?.toID()

    private fun isQueueActive(): Boolean =
        PlayingQueue.isNotEmpty() && PlayingQueue.queueMode == PlayingQueueMode.ONLINE

    private fun hasWatchPositionEntry(video: StreamItem): Boolean {
        val videoId = video.url?.toID() ?: return false

        val watchHistoryEntry = runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.watchHistoryDao().findById(videoId)
        }

        return watchHistoryEntry != null
    }
}