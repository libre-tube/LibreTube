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

class TrendingAdapter(
    private val videoFeed: List<StreamItem>,
    private val childFragmentManager: FragmentManager
) : RecyclerView.Adapter<TrendingViewHolder>() {
    private val TAG = "TrendingAdapter"

    override fun getItemCount(): Int {
        return videoFeed.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = TrendingRowBinding.inflate(layoutInflater, parent, false)
        return TrendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrendingViewHolder, position: Int) {
        val trending = videoFeed[position]
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
            root.setOnLongClickListener {
                val videoId = trending.url!!.replace("/watch?v=", "")
                VideoOptionsDialog(videoId, root.context)
                    .show(childFragmentManager, "VideoOptionsDialog")
                true
            }
        }
    }
}

class TrendingViewHolder(val binding: TrendingRowBinding) : RecyclerView.ViewHolder(binding.root)
