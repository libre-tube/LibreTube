package com.github.libretube.adapters

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.github.libretube.R
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
        var searchItemsSize = searchItems.size
        searchItems.addAll(newItems)
        notifyItemRangeInserted(searchItemsSize, newItems.size)
    }

    override fun getItemCount(): Int {
        return searchItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val layout = when (viewType) {
            0 -> R.layout.video_search_row
            1 -> R.layout.channel_search_row
            2 -> R.layout.playlist_search_row
            else -> throw IllegalArgumentException("Invalid type")
        }
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(layout, parent, false)
        return SearchViewHolder(cell, childFragmentManager)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(searchItems[position])
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            searchItems[position].url!!.startsWith("/watch", false) -> 0
            searchItems[position].url!!.startsWith("/channel", false) -> 1
            searchItems[position].url!!.startsWith("/playlist", false) -> 2
            else -> 3
        }
    }
}

class SearchViewHolder(
    private val v: View,
    private val childFragmentManager: FragmentManager
) : RecyclerView.ViewHolder(v) {

    private fun bindWatch(item: SearchItem) {
        val thumbnailImage = v.findViewById<ImageView>(R.id.search_thumbnail)
        Picasso.get().load(item.thumbnail).fit().centerCrop().into(thumbnailImage)
        val thumbnailDuration = v.findViewById<TextView>(R.id.search_thumbnail_duration)
        if (item.duration != -1L) {
            thumbnailDuration.text = DateUtils.formatElapsedTime(item.duration!!)
        } else {
            thumbnailDuration.text = v.context.getString(R.string.live)
            thumbnailDuration.setBackgroundColor(R.attr.colorPrimaryDark)
        }
        val channelImage = v.findViewById<ImageView>(R.id.search_channel_image)
        Picasso.get().load(item.uploaderAvatar).fit().centerCrop().into(channelImage)
        val title = v.findViewById<TextView>(R.id.search_description)
        title.text = item.title
        val views = v.findViewById<TextView>(R.id.search_views)
        val viewsString = if (item.views?.toInt() != -1) item.views.formatShort() else ""
        val uploadDate = if (item.uploadedDate != null) item.uploadedDate else ""
        views.text =
            if (viewsString != "" && uploadDate != "")
                "$viewsString • $uploadDate"
            else
                viewsString + uploadDate
        val channelName = v.findViewById<TextView>(R.id.search_channel_name)
        channelName.text = item.uploaderName
        v.setOnClickListener {
            var bundle = Bundle()
            bundle.putString("videoId", item.url!!.replace("/watch?v=", ""))
            var frag = PlayerFragment()
            frag.arguments = bundle
            val activity = v.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction()
                .remove(PlayerFragment())
                .commit()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, frag)
                .commitNow()
        }
        v.setOnLongClickListener {
            val videoId = item.url!!.replace("/watch?v=", "")
            VideoOptionsDialog(videoId, v.context)
                .show(childFragmentManager, VideoOptionsDialog.TAG)
            true
        }
        channelImage.setOnClickListener {
            val activity = v.context as MainActivity
            val bundle = bundleOf("channel_id" to item.uploaderUrl)
            activity.navController.navigate(R.id.channel, bundle)
        }
    }

    private fun bindChannel(item: SearchItem) {
        val channelImage = v.findViewById<ImageView>(R.id.search_channel_image)
        Picasso.get().load(item.thumbnail).fit().centerCrop().into(channelImage)
        val channelName = v.findViewById<TextView>(R.id.search_channel_name)
        channelName.text = item.name
        val channelViews = v.findViewById<TextView>(R.id.search_views)
        channelViews.text = v.context.getString(
            R.string.subscribers,
            item.subscribers.formatShort()
        ) + " • " + v.context.getString(R.string.videoCount, item.videos.toString())
        v.setOnClickListener {
            val activity = v.context as MainActivity
            val bundle = bundleOf("channel_id" to item.url)
            activity.navController.navigate(R.id.channel, bundle)
        }
        // todo sub button
    }

    private fun bindPlaylist(item: SearchItem) {
        val playlistImage = v.findViewById<ImageView>(R.id.search_thumbnail)
        Picasso.get().load(item.thumbnail).fit().centerCrop().into(playlistImage)
        val playlistNumber = v.findViewById<TextView>(R.id.search_playlist_number)
        if (item.videos?.toInt() != -1) playlistNumber.text = item.videos.toString()
        val playlistName = v.findViewById<TextView>(R.id.search_description)
        playlistName.text = item.name
        val playlistChannelName = v.findViewById<TextView>(R.id.search_name)
        playlistChannelName.text = item.uploaderName
        val playlistVideosNumber = v.findViewById<TextView>(R.id.search_playlist_videos)
        if (item.videos?.toInt() != -1)
            playlistVideosNumber.text =
                v.context.getString(R.string.videoCount, item.videos.toString())
        v.setOnClickListener {
            // playlist clicked
            val activity = v.context as MainActivity
            val bundle = bundleOf("playlist_id" to item.url)
            activity.navController.navigate(R.id.playlistFragment, bundle)
        }
    }

    fun bind(searchItem: SearchItem) {
        when {
            searchItem.url!!.startsWith("/watch", false) -> bindWatch(searchItem)
            searchItem.url!!.startsWith("/channel", false) -> bindChannel(searchItem)
            searchItem.url!!.startsWith("/playlist", false) -> bindPlaylist(searchItem)
            else -> {
            }
        }
    }
}
