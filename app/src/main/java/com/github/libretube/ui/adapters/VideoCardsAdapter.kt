package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.AllCaughtUpRowBinding
import com.github.libretube.databinding.TrendingRowBinding
import com.github.libretube.extensions.dpToPx
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.VideoCardsViewHolder
import com.github.libretube.util.TextUtils

class VideoCardsAdapter(private val columnWidthDp: Float? = null) :
    ListAdapter<StreamItem, VideoCardsViewHolder>(DiffUtilItemCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].type == CAUGHT_UP_STREAM_TYPE) CAUGHT_UP_TYPE else NORMAL_TYPE
    }

    fun removeItemById(videoId: String) {
        val index = currentList.indexOfFirst {
            it.url?.toID() == videoId
        }.takeIf { it > 0 } ?: return
        val updatedList = currentList.toMutableList().also {
            it.removeAt(index)
        }

        submitList(updatedList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoCardsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when {
            viewType == CAUGHT_UP_TYPE -> VideoCardsViewHolder(
                AllCaughtUpRowBinding.inflate(layoutInflater, parent, false)
            )

            else -> VideoCardsViewHolder(
                TrendingRowBinding.inflate(layoutInflater, parent, false)
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideoCardsViewHolder, position: Int) {
        val video = getItem(holder.bindingAdapterPosition)
        val videoId = video.url.orEmpty().toID()

        val context = (holder.trendingRowBinding ?: holder.allCaughtUpBinding)!!.root.context
        val activity = (context as BaseActivity)
        val fragmentManager = activity.supportFragmentManager

        holder.trendingRowBinding?.apply {
            // set a fixed width for better visuals
            if (columnWidthDp != null) {
                root.updateLayoutParams {
                    width = columnWidthDp.dpToPx()
                }
            }
            watchProgress.setWatchProgressLength(videoId, video.duration ?: 0L)

            textViewTitle.text = video.title
            textViewChannel.text = TextUtils.formatViewsString(
                root.context,
                video.views ?: -1,
                video.uploaded,
                video.uploaderName
            )

            video.duration?.let {
                thumbnailDuration.setFormattedDuration(
                    it,
                    video.isShort,
                    video.uploaded
                )
            }
            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }
            ImageHelper.loadImage(video.thumbnail, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, videoId)
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
                sheet.show(fragmentManager, VideoCardsAdapter::class.java.name)
                true
            }
        }
    }

    companion object {
        private const val NORMAL_TYPE = 0
        private const val CAUGHT_UP_TYPE = 1

        const val CAUGHT_UP_STREAM_TYPE = "caught"
    }
}
