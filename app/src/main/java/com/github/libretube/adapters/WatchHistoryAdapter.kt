package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.WatchHistoryRowBinding
import com.github.libretube.obj.WatchHistoryItem
import com.squareup.picasso.Picasso

class WatchHistoryAdapter(
    private val watchHistory: List<WatchHistoryItem>
) :
    RecyclerView.Adapter<WatchHistoryViewHolder>() {
    private val TAG = "WatchHistoryAdapter"
    private lateinit var binding: WatchHistoryRowBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = WatchHistoryRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val video = watchHistory[position]
        binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            Picasso.get().load(video.thumbnailUrl).into(thumbnail)
            Picasso.get().load(video.uploaderAvatar).into(channelImage)
        }
    }

    override fun getItemCount(): Int {
        return watchHistory.size
    }
}

class WatchHistoryViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
