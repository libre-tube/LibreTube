package com.github.libretube.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.MainActivity
import com.squareup.picasso.Picasso
import com.github.libretube.PlayerFragment
import com.github.libretube.R
import com.github.libretube.obj.SearchItem
import com.github.libretube.formatShort

class SearchAdapter(private val searchItems: List<SearchItem>) :
    RecyclerView.Adapter<CustomViewHolder1>() {
    override fun getItemCount(): Int {
        return searchItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder1 {
        val layout = when (viewType) {
            0 -> R.layout.video_search_row
            1 -> R.layout.channel_search_row
            2 -> R.layout.playlist_search_row
            else -> throw IllegalArgumentException("Invalid type")
        }
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(layout, parent, false)
        return CustomViewHolder1(cell)
    }

    override fun onBindViewHolder(holder: CustomViewHolder1, position: Int) {
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

class CustomViewHolder1(private val view: View) : RecyclerView.ViewHolder(view) {

    private fun bindWatch(item: SearchItem) {
        val thumbnailImage = view.findViewById<ImageView>(R.id.search_thumbnail)
        val channelImage = view.findViewById<ImageView>(R.id.search_channel_image)
        val title = view.findViewById<TextView>(R.id.search_description)
        val views = view.findViewById<TextView>(R.id.search_views)
        val channelName = view.findViewById<TextView>(R.id.search_channel_name)

        Picasso.get().load(item.thumbnail).into(thumbnailImage)
        Picasso.get().load(item.uploaderAvatar).into(channelImage)
        title.text = item.title
        views.text = item.views.formatShort() + " • " + item.uploadedDate
        channelName.text = item.uploaderName

        view.setOnClickListener {
            val bundle = Bundle()
            val frag = PlayerFragment()
            val activity = view.context as AppCompatActivity

            frag.arguments = bundle
            bundle.putString("videoId", item.url!!.replace("/watch?v=", ""))
            activity.supportFragmentManager.beginTransaction()
                .remove(PlayerFragment())
                .commit()
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.container, frag)
                .commitNow()
        }
        channelImage.setOnClickListener {
            val activity = view.context as MainActivity
            val bundle = bundleOf("channel_id" to item.uploaderUrl)
            activity.navController.navigate(R.id.channel, bundle)
        }
    }

    private fun bindChannel(item: SearchItem) {
        val channelImage = view.findViewById<ImageView>(R.id.search_channel_image)
        val channelName = view.findViewById<TextView>(R.id.search_channel_name)
        val channelViews = view.findViewById<TextView>(R.id.search_views)

        Picasso.get().load(item.thumbnail).into(channelImage)
        channelName.text = item.name
        channelViews.text =
            item.subscribers.formatShort() + " subscribers • " + item.videos + " videos"
        view.setOnClickListener {
            val activity = view.context as MainActivity
            val bundle = bundleOf("channel_id" to item.url)
            activity.navController.navigate(R.id.channel, bundle)
        }
        //todo sub button
    }

    private fun bindPlaylist(item: SearchItem) {
        val playlistImage = view.findViewById<ImageView>(R.id.search_thumbnail)
        val playlistNumber = view.findViewById<TextView>(R.id.search_playlist_number)
        val playlistName = view.findViewById<TextView>(R.id.search_description)
        val playlistChannelName = view.findViewById<TextView>(R.id.search_name)
        val playlistVideosNumber = view.findViewById<TextView>(R.id.search_playlist_videos)

        playlistNumber.text = item.videos.toString()
        Picasso.get().load(item.thumbnail).into(playlistImage)
        playlistName.text = item.name
        playlistChannelName.text = item.uploaderName
        playlistVideosNumber.text = item.videos.toString() + " videos"

        view.setOnClickListener {
            //playlist clicked
            val activity = view.context as MainActivity
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
                // no op
            }
        }
    }
}
