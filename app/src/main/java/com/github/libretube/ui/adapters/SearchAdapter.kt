package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.databinding.ChannelRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.extensions.setupSubscriptionButton
import com.github.libretube.ui.sheets.ChannelOptionsBottomSheet
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.SearchViewHolder
import com.github.libretube.util.TextUtils

class SearchAdapter(
    private val searchItems: MutableList<ContentItem>
) :
    RecyclerView.Adapter<SearchViewHolder>() {

    fun updateItems(newItems: List<ContentItem>) {
        val searchItemsSize = searchItems.size
        searchItems.addAll(newItems)
        notifyItemRangeInserted(searchItemsSize, newItems.size)
    }

    override fun getItemCount(): Int {
        return searchItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> SearchViewHolder(
                VideoRowBinding.inflate(layoutInflater, parent, false)
            )
            1 -> SearchViewHolder(
                ChannelRowBinding.inflate(layoutInflater, parent, false)
            )
            2 -> SearchViewHolder(
                PlaylistsRowBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val searchItem = searchItems[position]

        val videoRowBinding = holder.videoRowBinding
        val channelRowBinding = holder.channelRowBinding
        val playlistRowBinding = holder.playlistRowBinding

        if (videoRowBinding != null) {
            bindWatch(searchItem, videoRowBinding)
        } else if (channelRowBinding != null) {
            bindChannel(searchItem, channelRowBinding)
        } else if (playlistRowBinding != null) bindPlaylist(searchItem, playlistRowBinding)
    }

    override fun getItemViewType(position: Int): Int {
        return when (searchItems[position].type) {
            "stream" -> 0
            "channel" -> 1
            "playlist" -> 2
            else -> 3
        }
    }

    private fun bindWatch(item: ContentItem, binding: VideoRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, thumbnail)
            thumbnailDuration.setFormattedDuration(item.duration, item.isShort)
            ImageHelper.loadImage(item.uploaderAvatar, channelImage)
            videoTitle.text = item.title
            val viewsString = if (item.views != -1L) item.views.formatShort() else ""
            val uploadDate = item.uploadedDate.orEmpty()
            videoInfo.text =
                if (viewsString.isNotEmpty() && uploadDate.isNotEmpty()) {
                    "$viewsString • $uploadDate"
                } else {
                    viewsString + uploadDate
                }
            channelName.text = item.uploaderName
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, item.url)
            }
            val videoId = item.url.toID()
            val videoName = item.title!!
            root.setOnLongClickListener {
                VideoOptionsBottomSheet(videoId, videoName)
                    .show(
                        (root.context as BaseActivity).supportFragmentManager,
                        VideoOptionsBottomSheet::class.java.name
                    )
                true
            }
            channelContainer.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.uploaderUrl)
            }
            watchProgress.setWatchProgressLength(videoId, item.duration)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindChannel(
        item: ContentItem,
        binding: ChannelRowBinding
    ) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, searchChannelImage)
            searchChannelName.text = item.name
            searchViews.text = root.context.getString(
                R.string.subscribers,
                item.subscribers.formatShort()
            ) + TextUtils.SEPARATOR + root.context.getString(
                R.string.videoCount,
                item.videos.toString()
            )
            root.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.url)
            }

            root.setOnLongClickListener {
                ChannelOptionsBottomSheet(item.url.toID(), item.name)
                    .show((root.context as BaseActivity).supportFragmentManager)
                true
            }

            binding.searchSubButton.setupSubscriptionButton(item.url.toID(), item.name?.toID())
        }
    }

    private fun bindPlaylist(
        item: ContentItem,
        binding: PlaylistsRowBinding
    ) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, playlistThumbnail)
            if (item.videos != -1L) videoCount.text = item.videos.toString()
            playlistTitle.text = item.name
            playlistDescription.text = item.uploaderName
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, item.url, PlaylistType.PUBLIC)
            }
            deletePlaylist.visibility = View.GONE
            root.setOnLongClickListener {
                val playlistId = item.url.toID()
                val playlistName = item.name!!
                PlaylistOptionsBottomSheet(playlistId, playlistName, PlaylistType.PUBLIC)
                    .show(
                        (root.context as BaseActivity).supportFragmentManager,
                        PlaylistOptionsBottomSheet::class.java.name
                    )
                true
            }
        }
    }
}
