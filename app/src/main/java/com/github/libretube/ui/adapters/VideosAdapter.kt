package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.databinding.AllCaughtUpRowBinding
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.extensions.ceilHalf
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
    private val forceMode: LayoutMode = LayoutMode.RESPECT_PREF
) : RecyclerView.Adapter<VideosViewHolder>() {

    private var visibleCount = minOf(10, streamItems.size)

    override fun getItemCount(): Int {
        return when {
            showAllAtOnce -> streamItems.size
            else -> minOf(streamItems.size, visibleCount)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (streamItems[position].type == CAUGHT_UP_STREAM_TYPE) CAUGHT_UP_TYPE else NORMAL_TYPE
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
        visibleCount--
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
                LayoutMode.TRENDING_ROW,
                LayoutMode.RELATED_COLUMN
            ) -> VideosViewHolder(
                TrendingRowBinding.inflate(layoutInflater, parent, false)
            )

            forceMode == LayoutMode.CHANNEL_ROW -> VideosViewHolder(
                VideoRowBinding.inflate(layoutInflater, parent, false)
            )

            PreferenceHelper.getBoolean(
                PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
                false
            ) -> VideosViewHolder(VideoRowBinding.inflate(layoutInflater, parent, false))

            else -> VideosViewHolder(TrendingRowBinding.inflate(layoutInflater, parent, false))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideosViewHolder, position: Int) {
        holder.trendingRowBinding?.thumbnail?.setImageDrawable(null)

        val video = streamItems[position]
        val videoId = video.url?.toID()

        videoId?.let {
            (holder.trendingRowBinding?.watchProgress ?: holder.videoRowBinding!!.watchProgress)
                .setWatchProgressLength(it, video.duration ?: 0L)
        }

        val context = (
            holder.videoRowBinding ?: holder.trendingRowBinding
                ?: holder.allCaughtUpBinding
            )!!.root.context
        val activity = (context as BaseActivity)
        val fragmentManager = activity.supportFragmentManager

        val uploadDateStr = video.uploaded.takeIf { it > 0 }
            ?.let { TextUtils.formatRelativeDate(context, it) }
            ?.toString()

        // Trending layout
        holder.trendingRowBinding?.apply {
            // set a fixed width for better visuals
            if (forceMode == LayoutMode.RELATED_COLUMN) {
                root.updateLayoutParams {
                    width = 250f.dpToPx()
                }
            }

            textViewTitle.text = video.title
            textViewChannel.text = if ((video.views ?: 0L) > 0L) {
                root.context.getString(
                    R.string.trending_views,
                    video.uploaderName,
                    video.views.formatShort(),
                    uploadDateStr
                )
            } else {
                "${video.uploaderName} ${TextUtils.SEPARATOR} $uploadDateStr"
            }
            video.duration?.let { thumbnailDuration.setFormattedDuration(it, video.isShort) }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }
            ImageHelper.loadImage(video.thumbnail, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.url)
            }

            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video)
                sheet.show(fragmentManager, VideosAdapter::class.java.name)
                true
            }
        }

        // Normal videos row layout
        holder.videoRowBinding?.apply {
            videoTitle.text = video.title

            videoInfo.text = root.context.getString(
                R.string.normal_views,
                video.views.formatShort(),
                uploadDateStr?.let { " ${TextUtils.SEPARATOR} $it" }
            )

            thumbnailDuration.text = video.duration?.let { DateUtils.formatElapsedTime(it) }

            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (forceMode != LayoutMode.CHANNEL_ROW) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
                channelName.text = video.uploaderName

                channelContainer.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
                }
            } else {
                channelImageContainer.isGone = true
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.url)
            }

            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    notifyItemChanged(position)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video)
                sheet.show(fragmentManager, VideosAdapter::class.java.name)
                true
            }
        }
    }

    companion object {
        enum class LayoutMode {
            RESPECT_PREF,
            TRENDING_ROW,
            VIDEO_ROW,
            CHANNEL_ROW,
            RELATED_COLUMN
        }

        fun getLayout(context: Context, gridItems: Int): LayoutManager {
            return if (PreferenceHelper.getBoolean(
                    PreferenceKeys.ALTERNATIVE_VIDEOS_LAYOUT,
                    false
                )
            ) {
                GridLayoutManager(context, gridItems.ceilHalf())
            } else {
                GridLayoutManager(context, gridItems)
            }
        }

        private const val NORMAL_TYPE = 0
        private const val CAUGHT_UP_TYPE = 1

        const val CAUGHT_UP_STREAM_TYPE = "caught"
    }
}
