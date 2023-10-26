package com.github.libretube.ui.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toastFromMainDispatcher
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet.Companion.VIDEO_OPTIONS_SHEET_REQUEST_KEY
import com.github.libretube.ui.viewholders.PlaylistViewHolder
import com.github.libretube.util.TextUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @param originalFeed original, unsorted feed, needed in order to delete the proper video from
 * playlists
 */
class PlaylistAdapter(
    private val originalFeed: MutableList<StreamItem>,
    private val sortedFeed: MutableList<StreamItem>,
    private val playlistId: String,
    private val playlistType: PlaylistType
) : RecyclerView.Adapter<PlaylistViewHolder>() {

    private var visibleCount = minOf(20, sortedFeed.size)

    override fun getItemCount(): Int {
        return when (playlistType) {
            PlaylistType.PUBLIC -> sortedFeed.size
            else -> minOf(visibleCount, sortedFeed.size)
        }
    }

    fun updateItems(newItems: List<StreamItem>) {
        val oldSize = sortedFeed.size
        sortedFeed.addAll(newItems)
        notifyItemRangeInserted(oldSize, sortedFeed.size)
    }

    fun showMoreItems() {
        val oldSize = visibleCount
        visibleCount += minOf(10, sortedFeed.size - oldSize)
        if (visibleCount == oldSize) return
        notifyItemRangeInserted(oldSize, visibleCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val streamItem = sortedFeed[position]
        holder.binding.apply {
            videoTitle.text = streamItem.title
            videoInfo.text = streamItem.uploaderName
            channelImage.isGone = true

            thumbnailDuration.setFormattedDuration(streamItem.duration!!, streamItem.isShort)
            ImageHelper.loadImage(streamItem.thumbnail, thumbnail)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, streamItem.url, playlistId)
            }
            val videoId = streamItem.url!!.toID()

            val activity = (root.context as BaseActivity)
            val fragmentManager = activity.supportFragmentManager
            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to streamItem)
                sheet.show(fragmentManager, VideoOptionsBottomSheet::class.java.name)
                true
            }

            if (!streamItem.uploaderUrl.isNullOrBlank()) {
                videoInfo.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, streamItem.uploaderUrl.toID())
                }
                // add some extra padding to make it easier to click
                val extraPadding = (3).dpToPx().toInt()
                videoInfo.updatePadding(top = extraPadding, bottom = extraPadding)
            }

            watchProgress.setWatchProgressLength(videoId, streamItem.duration)
        }
    }

    fun removeFromPlaylist(rootView: View, sortedFeedPosition: Int) {
        val video = sortedFeed[sortedFeedPosition]

        // get the index of the video in the playlist
        // could vary due to playlist sorting by the user
        val originalPlaylistPosition = originalFeed
            .indexOfFirst { it.url == video.url }
            .takeIf { it >= 0 } ?: return

        sortedFeed.removeAt(sortedFeedPosition)
        originalFeed.removeAt(originalPlaylistPosition)
        visibleCount--

        (rootView.context as Activity).runOnUiThread {
            notifyItemRemoved(sortedFeedPosition)
            notifyItemRangeChanged(sortedFeedPosition, itemCount)
        }
        val appContext = rootView.context.applicationContext

        // try to remove the video from the playlist and show an undo snackbar if successful
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    PlaylistsHelper.removeFromPlaylist(playlistId, originalPlaylistPosition)
                }

                val shortTitle = TextUtils.limitTextToLength(video.title.orEmpty(), 50)
                val snackBarText = rootView.context.getString(R.string.successfully_removed_from_playlist, shortTitle)
                Snackbar.make(rootView, snackBarText, Snackbar.LENGTH_LONG)
                    .setTextMaxLines(3)
                    .setAction(R.string.undo) {
                        reAddToPlaylist(appContext, video, sortedFeedPosition, originalPlaylistPosition)
                    }
                    .show()
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                appContext.toastFromMainDispatcher(R.string.unknown_error)
            }
        }
    }

    private fun reAddToPlaylist(
        context: Context,
        streamItem: StreamItem,
        sortedFeedPosition: Int,
        originalPlaylistPosition: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PlaylistsHelper.addToPlaylist(playlistId, streamItem)
                sortedFeed.add(sortedFeedPosition, streamItem)
                originalFeed.add(originalPlaylistPosition, streamItem)
                visibleCount++

                withContext(Dispatchers.Main) {
                    notifyItemInserted(sortedFeedPosition)
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                context.toastFromMainDispatcher(R.string.unknown_error)
            }
        }
    }
}
