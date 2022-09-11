package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.databinding.ChannelRowBinding
import com.github.libretube.databinding.PlaylistSearchRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.sheets.PlaylistOptionsDialog
import com.github.libretube.sheets.VideoOptionsBottomSheet
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.setFormattedDuration
import com.github.libretube.extensions.setWatchProgressLength
import com.github.libretube.extensions.toID
import com.github.libretube.obj.SearchItem
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchAdapter(
    private val searchItems: MutableList<SearchItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<SearchViewHolder>() {

    fun updateItems(newItems: List<SearchItem>) {
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
                PlaylistSearchRowBinding.inflate(layoutInflater, parent, false)
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
        return when {
            searchItems[position].url!!.startsWith("/watch", false) -> 0
            searchItems[position].url!!.startsWith("/channel", false) -> 1
            searchItems[position].url!!.startsWith("/playlist", false) -> 2
            else -> 3
        }
    }

    private fun bindWatch(item: SearchItem, binding: VideoRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, thumbnail)
            thumbnailDuration.setFormattedDuration(item.duration!!)
            ImageHelper.loadImage(item.uploaderAvatar, channelImage)
            videoTitle.text = item.title
            val viewsString = if (item.views?.toInt() != -1) item.views.formatShort() else ""
            val uploadDate = if (item.uploadedDate != null) item.uploadedDate else ""
            videoInfo.text =
                if (viewsString != "" && uploadDate != "") {
                    "$viewsString • $uploadDate"
                } else {
                    viewsString + uploadDate
                }
            channelName.text = item.uploaderName
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, item.url)
            }
            val videoId = item.url!!.toID()
            root.setOnLongClickListener {
                VideoOptionsBottomSheet(videoId)
                    .show(childFragmentManager, VideoOptionsBottomSheet::class.java.name)
                true
            }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.uploaderUrl)
            }
            watchProgress.setWatchProgressLength(videoId, item.duration!!)
        }
    }

    private fun bindChannel(item: SearchItem, binding: ChannelRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, searchChannelImage)
            searchChannelName.text = item.name
            searchViews.text = root.context.getString(
                R.string.subscribers,
                item.subscribers.formatShort()
            ) + " • " + root.context.getString(R.string.videoCount, item.videos.toString())
            root.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.url)
            }
            val channelId = item.url!!.toID()

            isSubscribed(channelId, binding)
        }
    }

    private fun isSubscribed(channelId: String, binding: ChannelRowBinding) {
        // check whether the user subscribed to the channel
        CoroutineScope(Dispatchers.Main).launch {
            var isSubscribed = SubscriptionHelper.isSubscribed(channelId)

            // if subscribed change text to unsubscribe
            if (isSubscribed == true) {
                binding.searchSubButton.text = binding.root.context.getString(R.string.unsubscribe)
            }

            // make sub button visible and set the on click listeners to (un)subscribe
            if (isSubscribed == null) return@launch
            binding.searchSubButton.visibility = View.VISIBLE

            binding.searchSubButton.setOnClickListener {
                if (isSubscribed == false) {
                    SubscriptionHelper.subscribe(channelId)
                    binding.searchSubButton.text =
                        binding.root.context.getString(R.string.unsubscribe)
                    isSubscribed = true
                } else {
                    SubscriptionHelper.unsubscribe(channelId)
                    binding.searchSubButton.text =
                        binding.root.context.getString(R.string.subscribe)
                    isSubscribed = false
                }
            }
        }
    }

    private fun bindPlaylist(item: SearchItem, binding: PlaylistSearchRowBinding) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, searchThumbnail)
            if (item.videos?.toInt() != -1) searchPlaylistNumber.text = item.videos.toString()
            searchDescription.text = item.name
            searchName.text = item.uploaderName
            if (item.videos?.toInt() != -1) {
                searchPlaylistVideos.text =
                    root.context.getString(R.string.videoCount, item.videos.toString())
            }
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, item.url, false)
            }
            root.setOnLongClickListener {
                val playlistId = item.url!!.toID()
                PlaylistOptionsDialog(playlistId, false)
                    .show(childFragmentManager, PlaylistOptionsDialog::class.java.name)
                true
            }
        }
    }
}

class SearchViewHolder : RecyclerView.ViewHolder {
    var videoRowBinding: VideoRowBinding? = null
    var channelRowBinding: ChannelRowBinding? = null
    var playlistRowBinding: PlaylistSearchRowBinding? = null

    constructor(binding: VideoRowBinding) : super(binding.root) {
        videoRowBinding = binding
    }

    constructor(binding: ChannelRowBinding) : super(binding.root) {
        channelRowBinding = binding
    }

    constructor(binding: PlaylistSearchRowBinding) : super(binding.root) {
        playlistRowBinding = binding
    }
}
