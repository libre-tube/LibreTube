package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.AllCaughtUpRowBinding
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.VideosViewHolder
import com.github.libretube.util.TextUtils

class VideosAdapter(
    private val streamItems: MutableList<StreamItem>,
    private val showAllAtOnce: Boolean = true,
    private val forceMode: ForceMode = ForceMode.NONE
) : RecyclerView.Adapter<VideosViewHolder>() {

    private var visibleCount = minOf(10, streamItems.size)

    override fun getItemCount(): Int {
        return when {
            showAllAtOnce -> streamItems.size
            else -> minOf(streamItems.size, visibleCount)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (streamItems[position].type == "caught") CAUGHT_UP_TYPE else NORMAL_TYPE
    }

    fun updateItems() {
        val oldSize = visibleCount
        visibleCount += minOf(10, streamItems.size - oldSize)
        if (visibleCount == oldSize) return
        notifyItemRangeInserted(oldSize, visibleCount)
    }

    fun insertItems(newItems: List<StreamItem>) {
        val feedSize = streamItems.size
        streamItems.addAll(newItems)
        notifyItemRangeInserted(feedSize, newItems.size)
    }

    fun removeItemById(videoId: String) {
        val index = streamItems.indexOfFirst {
            it.url?.toID() == videoId
        }.takeIf { it > 0 } ?: return
        streamItems.removeAt(index)
        visibleCount -= 1
        notifyItemRemoved(index)
        notifyItemRangeChanged(index, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideosViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when {
            viewType == CAUGHT_UP_TYPE -> VideosViewHolder(
                AllCaughtUpRowBinding.inflate(layoutInflater, parent, false)
            )
            forceMode in listOf(
                ForceMode.TRENDING,
                ForceMode.RELATED,
                ForceMode.HOME
            ) -> VideosViewHolder(
                TrendingRowBinding.inflate(layoutInflater, parent, false)
            )
            forceMode == ForceMode.CHANNEL -> VideosViewHolder(
                VideoRowBinding.inflate(layoutInflater, parent, false)
            )
            PreferenceHelper.getBoolean(
                PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
                false
            ) -> VideosViewHolder(VideoRowBinding.inflate(layoutInflater, parent, false))
            else -> VideosViewHolder(TrendingRowBinding.inflate(layoutInflater, parent, false))
        }
    }

    private fun hideItemView(holder: VideosViewHolder) {
        holder.itemView.visibility = View.GONE
        holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideosViewHolder, position: Int) {
        val video = streamItems[position]

        val videoId = video.url?.toID()
        val videoName = video.title

        // hide the item if there was an extractor error
        if (video.title == null && video.type != "caught") {
            hideItemView(holder)
            return
        }

        videoId?.let {
            (holder.trendingRowBinding?.watchProgress ?: holder.videoRowBinding!!.watchProgress)
                .setWatchProgressLength(it, video.duration ?: 0L)
        }

        // Trending layout
        holder.trendingRowBinding?.apply {
            // set a fixed width for better visuals
            root.updateLayoutParams {
                when (forceMode) {
                    ForceMode.RELATED -> width = 210.dpToPx().toInt()
                    ForceMode.HOME -> width = 250.dpToPx().toInt()
                    else -> {}
                }
            }

            textViewTitle.text = video.title
            textViewChannel.text = root.context.getString(
                R.string.trending_views,
                video.uploaderName,
                video.views.formatShort(),
                video.uploaded?.let { TextUtils.formatRelativeDate(it) }
            )
            video.duration?.let { thumbnailDuration.setFormattedDuration(it, video.isShort) }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }
            ImageHelper.loadImage(video.thumbnail, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.url)
            }
            root.setOnLongClickListener {
                if (videoId == null || videoName == null) return@setOnLongClickListener true

                VideoOptionsBottomSheet(videoId, videoName)
                    .show(
                        (root.context as BaseActivity).supportFragmentManager,
                        VideoOptionsBottomSheet::class.java.name
                    )

                true
            }
        }

        // Normal videos row layout
        holder.videoRowBinding?.apply {
            videoTitle.text = video.title

            videoInfo.text = root.context.getString(
                R.string.normal_views,
                video.views.formatShort(),
                video.uploaded?.let { TextUtils.SEPARATOR + TextUtils.formatRelativeDate(it) }
            )

            thumbnailDuration.text = video.duration?.let { DateUtils.formatElapsedTime(it) }

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

            root.setOnLongClickListener {
                if (videoId == null || videoName == null) return@setOnLongClickListener true
                VideoOptionsBottomSheet(videoId, videoName)
                    .show(
                        (root.context as BaseActivity).supportFragmentManager,
                        VideoOptionsBottomSheet::class.java.name
                    )
                true
            }
        }
    }

    companion object {
        enum class ForceMode {
            NONE,
            TRENDING,
            ROW,
            CHANNEL,
            RELATED,
            HOME
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

        private const val NORMAL_TYPE = 0
        private const val CAUGHT_UP_TYPE = 1
    }
}
