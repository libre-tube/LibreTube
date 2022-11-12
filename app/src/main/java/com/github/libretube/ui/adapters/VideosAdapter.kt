package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.VideosViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.NavigationHelper
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.TextUtils

class VideosAdapter(
    private val streamItems: MutableList<StreamItem>,
    private val childFragmentManager: FragmentManager,
    private val showAllAtOnce: Boolean = true,
    private val forceMode: ForceMode = ForceMode.NONE
) : RecyclerView.Adapter<VideosViewHolder>() {

    var index = 10

    override fun getItemCount(): Int {
        return when {
            showAllAtOnce -> streamItems.size
            index >= streamItems.size -> streamItems.size - 1
            else -> index
        }
    }

    fun updateItems() {
        val oldSize = index
        index += 10
        notifyItemRangeInserted(oldSize, index)
    }

    fun insertItems(newItems: List<StreamItem>) {
        val feedSize = streamItems.size
        streamItems.addAll(newItems)
        notifyItemRangeInserted(feedSize, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideosViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when {
            forceMode == ForceMode.TRENDING -> VideosViewHolder(TrendingRowBinding.inflate(layoutInflater, parent, false))
            forceMode == ForceMode.CHANNEL -> VideosViewHolder(VideoRowBinding.inflate(layoutInflater, parent, false))
            PreferenceHelper.getBoolean(
                PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
                false
            ) -> VideosViewHolder(VideoRowBinding.inflate(layoutInflater, parent, false))
            else -> VideosViewHolder(TrendingRowBinding.inflate(layoutInflater, parent, false))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideosViewHolder, position: Int) {
        val video = streamItems[position]

        // hide the item if there was an extractor error
        if (video.title == null) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        // Trending layout
        holder.trendingRowBinding?.apply {
            textViewTitle.text = video.title
            textViewChannel.text =
                video.uploaderName + TextUtils.SEPARATOR +
                video.views.formatShort() + " " +
                root.context.getString(R.string.views_placeholder) +
                TextUtils.SEPARATOR + video.uploaded?.let { DateUtils.getRelativeTimeSpanString(it) }
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

        // Normal videos row layout
        holder.videoRowBinding?.apply {
            videoTitle.text = video.title

            videoInfo.text =
                video.views.formatShort() + " " +
                root.context.getString(R.string.views_placeholder) +
                TextUtils.SEPARATOR + video.uploaded?.let { DateUtils.getRelativeTimeSpanString(it) }

            thumbnailDuration.text =
                video.duration?.let { DateUtils.formatElapsedTime(it) }

            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (forceMode != ForceMode.CHANNEL) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage)
                channelName.text = video.uploaderName

                channelContainer.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
                }
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

    companion object {
        enum class ForceMode {
            NONE,
            TRENDING,
            ROW,
            CHANNEL
        }

        fun getLayout(context: Context): LayoutManager {
            return if (PreferenceHelper.getBoolean(
                    PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
                    false
                )
            ) {
                LinearLayoutManager(context)
            } else {
                GridLayoutManager(
                    context,
                    PreferenceHelper.getString(
                        PreferenceKeys.GRID_COLUMNS,
                        context.resources.getInteger(R.integer.grid_items).toString()
                    ).toInt()
                )
            }
        }
    }
}
