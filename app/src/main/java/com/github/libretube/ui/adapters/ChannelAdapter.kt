package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.setWatchProgressLength
import com.github.libretube.extensions.toID
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.ChannelViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class ChannelAdapter(
    private val videoFeed: MutableList<StreamItem>,
    private val childFragmentManager: FragmentManager,
    private val showChannelInfo: Boolean = false
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val video = videoFeed[position]

        // hide the item if there was an extractor error
        if (video.title == null) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        holder.binding.apply {
            videoTitle.text = video.title

            videoInfo.text =
                video.views.formatShort() + " " +
                root.context.getString(R.string.views_placeholder) +
                " • " + video.uploaded?.let { DateUtils.getRelativeTimeSpanString(it) }

            thumbnailDuration.text =
                video.duration?.let { DateUtils.formatElapsedTime(it) }

            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (showChannelInfo) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage)
                channelName.text = video.uploaderName
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.url)
            }

            val videoId = video.url?.toID()
            val videoName = video.title
            root.setOnLongClickListener {
                if (videoId == null || videoName == null) return@setOnLongClickListener true
                VideoOptionsBottomSheet(videoId, videoName)
                    .show(childFragmentManager, VideoOptionsBottomSheet::class.java.name)

                true
            }

            if (videoId != null) {
                watchProgress.setWatchProgressLength(videoId, video.duration ?: 0L)
            }
        }
    }
}
