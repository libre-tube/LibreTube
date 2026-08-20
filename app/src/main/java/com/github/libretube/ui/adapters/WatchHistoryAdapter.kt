package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.db.dao.WatchHistoryPageItem
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.parcelable.PlayerData
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.WatchHistoryViewHolder
import com.github.libretube.util.TextUtils

class WatchHistoryAdapter(
    private val onWatchStatusChanged: (String) -> Unit
) :
    ListAdapter<WatchHistoryPageItem, WatchHistoryViewHolder>(
        DiffUtilItemCallback(areItemsTheSame = { old, new -> old.item.videoId == new.item.videoId })
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val pageItem = getItem(position)
        val video = pageItem.item
        holder.binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            videoInfo.text =
                video.uploadDate?.takeIf { !video.isLive }?.let { TextUtils.localizeDate(it) }
            ImageHelper.loadImage(video.thumbnailUrl, thumbnail)

            if (video.duration != null) {
                // we pass in 0 for the uploadDate, as a future video cannot be watched already
                thumbnailDuration.setFormattedDuration(video.duration, null, 0)
                thumbnailDurationCard.isVisible = true
            } else {
                thumbnailDurationCard.isGone = true
            }

            if (video.uploaderAvatar != null) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
                channelImageContainer.isVisible = true
            } else {
                channelImageContainer.isGone = true
            }

            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, PlayerData(video.videoId))
            }

            val activity = (root.context as BaseActivity)
            val fragmentManager = activity.supportFragmentManager
            root.setOnLongClickListener {
                fragmentManager.setFragmentResultListener(
                    VideoOptionsBottomSheet.VIDEO_OPTIONS_SHEET_REQUEST_KEY,
                    activity
                ) { _, _ ->
                    onWatchStatusChanged(video.videoId)
                }
                val sheet = VideoOptionsBottomSheet()
                sheet.arguments = bundleOf(IntentData.streamItem to video.toStreamItem())
                sheet.show(fragmentManager, WatchHistoryAdapter::class.java.name)
                true
            }

            if (video.duration != null) {
                watchProgress.setWatchProgressLength(pageItem.watchPosition, video.duration)
            } else watchProgress.isGone = true
            downloadBadge.isVisible = pageItem.isDownloaded
        }
    }
}
