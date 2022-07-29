package com.github.libretube.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.dialogs.VideoOptionsDialog
import com.github.libretube.obj.StreamItem
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.formatShort
import com.github.libretube.util.setWatchProgressLength

class TrendingAdapter(
    private val streamItems: List<StreamItem>,
    private val childFragmentManager: FragmentManager,
    private val showAllAtOne: Boolean = true
) : RecyclerView.Adapter<SubscriptionViewHolder>() {
    private val TAG = "TrendingAdapter"

    var index = 10

    override fun getItemCount(): Int {
        return if (showAllAtOne) streamItems.size
        else index
    }

    fun updateItems() {
        index += 10
        if (index > streamItems.size) {
            index = streamItems.size
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return SubscriptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val trending = streamItems[position]
        holder.binding.apply {
            textViewTitle.text = trending.title
            textViewChannel.text =
                trending.uploaderName + " • " +
                trending.views.formatShort() + " • " +
                DateUtils.getRelativeTimeSpanString(trending.uploaded!!)
            if (trending.duration != -1L) {
                thumbnailDuration.text = DateUtils.formatElapsedTime(trending.duration!!)
            } else {
                thumbnailDuration.text = root.context.getString(R.string.live)
                thumbnailDuration.setBackgroundColor(R.attr.colorPrimaryDark)
            }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, trending.uploaderUrl)
            }
            ConnectionHelper.loadImage(trending.thumbnail, thumbnail)
            ConnectionHelper.loadImage(trending.uploaderAvatar, channelImage)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, trending.url)
            }
            val videoId = trending.url!!.replace("/watch?v=", "")
            root.setOnLongClickListener {
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }
            watchProgress.setWatchProgressLength(videoId, trending.duration!!)
        }
    }
}

class SubscriptionViewHolder(val binding: TrendingRowBinding) :
    RecyclerView.ViewHolder(binding.root)
