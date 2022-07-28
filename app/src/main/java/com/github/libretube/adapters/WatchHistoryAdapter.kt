package com.github.libretube.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.obj.WatchHistoryItem
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper

class WatchHistoryAdapter(
    private val watchHistory: MutableList<WatchHistoryItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<WatchHistoryViewHolder>() {
    private val TAG = "WatchHistoryAdapter"

    fun clear() {
        val size = watchHistory.size
        watchHistory.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val video = watchHistory[position]
        holder.binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            videoInfo.text = video.uploadDate
            thumbnailDuration.text = DateUtils.formatElapsedTime(video.duration?.toLong()!!)
            ConnectionHelper.loadImage(video.thumbnailUrl, thumbnail)
            ConnectionHelper.loadImage(video.uploaderAvatar, channelImage)

            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.videoId)
            }
            root.setOnLongClickListener {
                VideoOptionsDialog(video.videoId!!, root.context)
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return watchHistory.size
    }
}

class WatchHistoryViewHolder(val binding: VideoRowBinding) :
    RecyclerView.ViewHolder(binding.root)
