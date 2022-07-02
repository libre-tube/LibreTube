package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.activities.MainActivity
import com.github.libretube.databinding.ChannelSearchRowBinding
import com.github.libretube.databinding.PlaylistSearchRowBinding
import com.github.libretube.databinding.VideoSearchRowBinding
import com.github.libretube.dialogs.PlaylistOptionsDialog
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.fragments.PlayerFragment
import com.github.libretube.obj.SearchItem
import com.github.libretube.util.formatShort
import com.squareup.picasso.Picasso

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
                VideoSearchRowBinding.inflate(layoutInflater, parent, false)
            )
            1 -> SearchViewHolder(
                ChannelSearchRowBinding.inflate(layoutInflater, parent, false)
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

        if (videoRowBinding != null) bindWatch(searchItem, videoRowBinding)
        else if (channelRowBinding != null) bindChannel(searchItem, channelRowBinding)
        else if (playlistRowBinding != null) bindPlaylist(searchItem, playlistRowBinding)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            searchItems[position].url!!.startsWith("/watch", false) -> 0
            searchItems[position].url!!.startsWith("/channel", false) -> 1
            searchItems[position].url!!.startsWith("/playlist", false) -> 2
            else -> 3
        }
    }

    private fun bindWatch(item: SearchItem, binding: VideoSearchRowBinding) {
        binding.apply {
            Picasso.get().load(item.thumbnail).fit().centerCrop().into(searchThumbnail)
            if (item.duration != -1L) {
                searchThumbnailDuration.text = DateUtils.formatElapsedTime(item.duration!!)
            } else {
                searchThumbnailDuration.text = root.context.getString(R.string.live)
                searchThumbnailDuration.setBackgroundColor(R.attr.colorPrimaryDark)
            }
            Picasso.get().load(item.uploaderAvatar).fit().centerCrop().into(searchChannelImage)
            searchDescription.text = item.title
            val viewsString = if (item.views?.toInt() != -1) item.views.formatShort() else ""
            val uploadDate = if (item.uploadedDate != null) item.uploadedDate else ""
            searchViews.text =
                if (viewsString != "" && uploadDate != "") {
                    "$viewsString • $uploadDate"
                } else {
                    viewsString + uploadDate
                }
            searchChannelName.text = item.uploaderName
            root.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("videoId", item.url!!.replace("/watch?v=", ""))
                val frag = PlayerFragment()
                frag.arguments = bundle
                val activity = root.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction()
                    .remove(PlayerFragment())
                    .commit()
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, frag)
                    .commitNow()
            }
            root.setOnLongClickListener {
                val videoId = item.url!!.replace("/watch?v=", "")
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, VideoOptionsDialog.TAG)
                true
            }
            searchChannelImage.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf("channel_id" to item.uploaderUrl)
                activity.navController.navigate(R.id.channelFragment, bundle)
            }
        }
    }

    private fun bindChannel(item: SearchItem, binding: ChannelSearchRowBinding) {
        binding.apply {
            Picasso.get().load(item.thumbnail).fit().centerCrop().into(searchChannelImage)
            searchChannelName.text = item.name
            searchViews.text = root.context.getString(
                R.string.subscribers,
                item.subscribers.formatShort()
            ) + " • " + root.context.getString(R.string.videoCount, item.videos.toString())
            root.setOnClickListener {
                val activity = root.context as MainActivity
                val bundle = bundleOf("channel_id" to item.url)
                activity.navController.navigate(R.id.channelFragment, bundle)
            }
        }
    }

    private fun bindPlaylist(item: SearchItem, binding: PlaylistSearchRowBinding) {
        binding.apply {
            Picasso.get().load(item.thumbnail).fit().centerCrop().into(searchThumbnail)
            if (item.videos?.toInt() != -1) searchPlaylistNumber.text = item.videos.toString()
            searchDescription.text = item.name
            searchName.text = item.uploaderName
            if (item.videos?.toInt() != -1) {
                searchPlaylistNumber.text =
                    root.context.getString(R.string.videoCount, item.videos.toString())
            }
            root.setOnClickListener {
                // playlist clicked
                val activity = root.context as MainActivity
                val bundle = bundleOf("playlist_id" to item.url)
                activity.navController.navigate(R.id.playlistFragment, bundle)
            }
            root.setOnLongClickListener {
                val playlistId = item.url!!.replace("/playlist?list=", "")
                PlaylistOptionsDialog(playlistId, root.context)
                    .show(childFragmentManager, "PlaylistOptionsDialog")
                true
            }
        }
    }
}

class SearchViewHolder : RecyclerView.ViewHolder {
    var videoRowBinding: VideoSearchRowBinding? = null
    var channelRowBinding: ChannelSearchRowBinding? = null
    var playlistRowBinding: PlaylistSearchRowBinding? = null

    constructor(binding: VideoSearchRowBinding) : super(binding.root) {
        videoRowBinding = binding
    }

    constructor(binding: ChannelSearchRowBinding) : super(binding.root) {
        channelRowBinding = binding
    }

    constructor(binding: PlaylistSearchRowBinding) : super(binding.root) {
        playlistRowBinding = binding
    }
}
