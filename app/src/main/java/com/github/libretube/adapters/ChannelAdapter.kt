package com.github.libretube.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.VideoChannelRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.formatShort

class ChannelAdapter(
    private val videoFeed: MutableList<StreamItem>,
    private val childFragmentManager: FragmentManager
) :
    RecyclerView.Adapter<ChannelViewHolder>() {

    override fun getItemCount(): Int {
        return videoFeed.size
    }

    fun updateItems(newItems: List<StreamItem>) {
        videoFeed.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoChannelRowBinding.inflate(layoutInflater, parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val trending = videoFeed[position]
        holder.binding.apply {
            channelDescription.text = trending.title
            channelViews.text =
                trending.views.formatShort() + " • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            channelDuration.text =
                DateUtils.formatElapsedTime(trending.duration!!)
            ConnectionHelper.loadImage(trending.thumbnail, channelThumbnail)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, trending.url)
            }
            root.setOnLongClickListener {
                val videoId = trending.url!!.replace("/watch?v=", "")
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }
        }
    }
}

class ChannelViewHolder(val binding: VideoChannelRowBinding) : RecyclerView.ViewHolder(binding.root)
