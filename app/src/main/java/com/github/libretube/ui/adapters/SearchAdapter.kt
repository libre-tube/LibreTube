package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.databinding.ChannelRowBinding
import com.github.libretube.databinding.PlaylistsRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.setFormattedDuration
import com.github.libretube.extensions.setWatchProgressLength
import com.github.libretube.extensions.toID
import com.github.libretube.ui.sheets.PlaylistOptionsBottomSheet
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.SearchViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchAdapter(
    private val searchItems: MutableList<ContentItem>,
    private val childFragmentManager: FragmentManager
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
        return when {
            searchItems[position].url!!.startsWith("/watch", false) -> 0
            searchItems[position].url!!.startsWith("/channel", false) -> 1
            searchItems[position].url!!.startsWith("/playlist", false) -> 2
            else -> 3
        }
    }

    private fun bindWatch(item: ContentItem, binding: VideoRowBinding) {
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
            val videoName = item.title!!
            root.setOnLongClickListener {
                VideoOptionsBottomSheet(videoId, videoName)
                    .show(childFragmentManager, VideoOptionsBottomSheet::class.java.name)
                true
            }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, item.uploaderUrl)
            }
            watchProgress.setWatchProgressLength(videoId, item.duration!!)
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

    private fun bindPlaylist(
        item: ContentItem,
        binding: PlaylistsRowBinding
    ) {
        binding.apply {
            ImageHelper.loadImage(item.thumbnail, playlistThumbnail)
            if (item.videos?.toInt() != -1) videoCount.text = item.videos.toString()
            playlistDescription.text = item.name
            playlistTitle.text = item.uploaderName
            root.setOnClickListener {
                NavigationHelper.navigatePlaylist(root.context, item.url, false)
            }
            deletePlaylist.visibility = View.GONE
            root.setOnLongClickListener {
                val playlistId = item.url!!.toID()
                val playlistName = item.name!!
                PlaylistOptionsBottomSheet(playlistId, playlistName, false)
                    .show(childFragmentManager, PlaylistOptionsBottomSheet::class.java.name)
                true
            }
        }
    }
}
