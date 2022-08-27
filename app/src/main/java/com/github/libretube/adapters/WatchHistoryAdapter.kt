package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.WatchHistoryRowBinding
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.extensions.setFormattedDuration
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.extensions.setWatchProgressLength

class WatchHistoryAdapter(
    private val watchHistory: MutableList<WatchHistoryItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<WatchHistoryViewHolder>() {

    fun removeFromWatchHistory(position: Int) {
        DatabaseHelper.removeFromWatchHistory(position)
        watchHistory.removeAt(position)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = WatchHistoryRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val video = watchHistory[position]
        holder.binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            videoInfo.text = video.uploadDate
            thumbnailDuration.setFormattedDuration(video.duration!!)
            ImageHelper.loadImage(video.thumbnailUrl, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage)

            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }

            deleteBTN.setOnClickListener {
                removeFromWatchHistory(position)
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.videoId)
            }
            root.setOnLongClickListener {
                VideoOptionsDialog(video.videoId)
                    .show(childFragmentManager, VideoOptionsDialog::class.java.name)
                true
            }

            watchProgress.setWatchProgressLength(video.videoId, video.duration)
        }
    }

    override fun getItemCount(): Int {
        return watchHistory.size
    }
}

class WatchHistoryViewHolder(val binding: WatchHistoryRowBinding) :
    RecyclerView.ViewHolder(binding.root)
