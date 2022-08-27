package com.github.libretube.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.setWatchProgressLength
import com.github.libretube.extensions.toID
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class ChannelAdapter(
    private val videoFeed: MutableList<StreamItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<ChannelViewHolder>() {

    override fun getItemCount(): Int {
        return videoFeed.size
    }

    fun updateItems(newItems: List<StreamItem>) {
        val feedSize = videoFeed.size
        videoFeed.addAll(newItems)
        notifyItemRangeInserted(feedSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.binding.apply {
            videoTitle.text = trending.title
            videoInfo.text =
                trending.views.formatShort() + " • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            thumbnailDuration.text =
                DateUtils.formatElapsedTime(trending.duration!!)
            ImageHelper.loadImage(trending.thumbnail, thumbnail)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, trending.url)
            }
            val videoId = trending.url.toID()
            root.setOnLongClickListener {
                VideoOptionsDialog(videoId)
                    .show(childFragmentManager, VideoOptionsDialog::class.java.name)
                true
            }
            watchProgress.setWatchProgressLength(videoId, trending.duration!!)
        }
    }
}

class ChannelViewHolder(val binding: VideoRowBinding) : RecyclerView.ViewHolder(binding.root)
