package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.ChannelRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.sheets.ChannelOptionsBottomSheet
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.SearchViewHolder
import com.github.libretube.util.TextUtils
import kotlinx.serialization.encodeToString

// TODO: Replace with SearchResultsAdapter when migrating the channel fragment to use Paging as well
class SearchChannelAdapter : ListAdapter<ContentItem, SearchViewHolder>(
    DiffUtilItemCallback(
        areItemsTheSame = { oldItem, newItem -> oldItem.url == newItem.url },
        areContentsTheSame = { _, _ -> true },
    )
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> SearchViewHolder(VideoRowBinding.inflate(layoutInflater, parent, false))
            1 -> SearchViewHolder(ChannelRowBinding.inflate(layoutInflater, parent, false))
            2 -> SearchViewHolder(PlaylistsRowBinding.inflate(layoutInflater, parent, false))
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val searchItem = currentList[position]

        val videoRowBinding = holder.videoRowBinding
        val channelRowBinding = holder.channelRowBinding
        val playlistRowBinding = holder.playlistRowBinding

        if (videoRowBinding != null) {
            bindVideo(searchItem, videoRowBinding, position)
        } else if (channelRowBinding != null) {
            bindChannel(searchItem, channelRowBinding)
        } else if (playlistRowBinding != null) {
            bindPlaylist(searchItem, playlistRowBinding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position].type) {
            StreamItem.TYPE_STREAM -> 0
            StreamItem.TYPE_CHANNEL -> 1
            StreamItem.TYPE_PLAYLIST -> 2
            else -> 3
        }
    }

    private fun bindVideo(item: ContentItem, binding: VideoRowBinding, position: Int) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, thumbnail)
            thumbnailDuration.setFormattedDuration(item.duration, item.isShort, item.uploaded)
            videoTitle.text = item.title
            videoInfo.text = TextUtils.formatViewsString(root.context, item.views, item.uploaded)

            channelContainer.isGone = true

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, item.url)
            }

            val videoId = item.url.toID()
            val activity = (root.context as BaseActivity)
            val fragmentManager = activity.supportFragmentManager
            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                val contentItemString = JsonHelper.json.encodeToString(item)
                val streamItem: StreamItem = JsonHelper.json.decodeFromString(contentItemString)
                sheet.arguments = bundleOf(IntentData.streamItem to streamItem)
                sheet.show(fragmentManager, SearchChannelAdapter::class.java.name)
                true
            }
            channelContainer.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.uploaderUrl)
            }
            watchProgress.setWatchProgressLength(videoId, item.duration)
        }
    }

    private fun bindChannel(item: ContentItem, binding: ChannelRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, searchChannelImage, true)
            searchChannelName.text = item.name

            val subscribers = item.subscribers.formatShort()
            searchViews.text = if (item.subscribers >= 0 && item.videos >= 0) {
                root.context.getString(R.string.subscriberAndVideoCounts, subscribers, item.videos)
            } else if (item.subscribers >= 0) {
                root.context.getString(R.string.subscribers, subscribers)
            } else if (item.videos >= 0) {
                root.context.getString(R.string.videoCount, item.videos)
            } else {
                ""
            }

            root.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.url)
            }

            var subscribed = false
            binding.searchSubButton.setupSubscriptionButton(item.url.toID(), item.name?.toID()) {
                subscribed = it
            }

            root.setOnLongClickListener {
                val channelOptionsSheet = ChannelOptionsBottomSheet()
                channelOptionsSheet.arguments = bundleOf(
                    IntentData.channelId to item.url.toID(),
                    IntentData.channelName to item.name,
                    IntentData.isSubscribed to subscribed
                )
                channelOptionsSheet.show((root.context as BaseActivity).supportFragmentManager)
                true
            }
        }
    }

    private fun bindPlaylist(item: ContentItem, binding: PlaylistsRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, playlistThumbnail)
            if (item.videos >= 0) videoCount.text = item.videos.toString()
            playlistTitle.text = item.name
            playlistDescription.text = item.uploaderName
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, item.url, PlaylistType.PUBLIC)
            }

            root.setOnLongClickListener {
                val playlistId = item.url.toID()
                val playlistName = item.name!!
                val sheet = PlaylistOptionsBottomSheet()
                sheet.arguments = bundleOf(
                    IntentData.playlistId to playlistId,
                    IntentData.playlistName to playlistName,
                    IntentData.playlistType to PlaylistType.PUBLIC
                )
                sheet.show(
                    (root.context as BaseActivity).supportFragmentManager,
                    PlaylistOptionsBottomSheet::class.java.name
                )
                true
            }
        }
    }
}
