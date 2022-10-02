package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
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
        val trending = streamItems[position]
        holder.binding.apply {
            textViewTitle.text = trending.title
            textViewChannel.text =
                trending.uploaderName + " • " +
                trending.views.formatShort() + " views • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            thumbnailDuration.setFormattedDuration(trending.duration!!)
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, trending.uploaderUrl)
            }
            ImageHelper.loadImage(trending.thumbnail, thumbnail)
            ImageHelper.loadImage(trending.uploaderAvatar, channelImage)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, trending.url)
            }
            val videoId = trending.url!!.toID()
            root.setOnLongClickListener {
                VideoOptionsBottomSheet(videoId)
                    .show(childFragmentManager, VideoOptionsBottomSheet::class.java.name)
                true
            }
            watchProgress.setWatchProgressLength(videoId, trending.duration!!)
        }
    }
}
