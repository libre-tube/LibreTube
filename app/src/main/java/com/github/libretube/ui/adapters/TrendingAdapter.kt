package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.setFormattedDuration
import com.github.libretube.extensions.setWatchProgressLength
import com.github.libretube.extensions.toID
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.SubscriptionViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper

class TrendingAdapter(
    private val streamItems: List<com.github.libretube.api.obj.StreamItem>,
    private val childFragmentManager: FragmentManager,
    private val showAllAtOne: Boolean = true
) : RecyclerView.Adapter<SubscriptionViewHolder>() {

    var index = 10

    override fun getItemCount(): Int {
        return if (showAllAtOne) {
            streamItems.size
        } else if (index >= streamItems.size) {
            streamItems.size - 1
        } else {
            index
        }
    }

    fun updateItems() {
        val oldSize = index
        index += 10
        notifyItemRangeInserted(oldSize, index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val video = streamItems[position]

        // hide the item if there was an extractor error
        if (video.title == null) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        holder.binding.apply {
            textViewTitle.text = video.title
            textViewChannel.text =
                video.uploaderName + " • " +
                video.views.formatShort() + " " +
                root.context.getString(R.string.views_placeholder) +
                " • " + video.uploaded?.let { DateUtils.getRelativeTimeSpanString(it) }
            video.duration?.let { thumbnailDuration.setFormattedDuration(it) }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }
            ImageHelper.loadImage(video.thumbnail, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage)
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
