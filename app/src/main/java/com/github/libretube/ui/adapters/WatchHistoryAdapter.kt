package com.github.libretube.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.WatchHistoryViewHolder
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class WatchHistoryAdapter(
    private val watchHistory: MutableList<WatchHistoryItem>
) :
    RecyclerView.Adapter<WatchHistoryViewHolder>() {

    private var visibleCount = minOf(10, watchHistory.size)

    override fun getItemCount() = visibleCount

    fun removeFromWatchHistory(position: Int) {
        val history = watchHistory[position]
        runBlocking(Dispatchers.IO) {
            DatabaseHolder.Database.watchHistoryDao().delete(history)
        }
        watchHistory.removeAt(position)
        visibleCount -= 1
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }

    fun showMoreItems() {
        val oldSize = visibleCount
        visibleCount += minOf(10, watchHistory.size - oldSize)
        if (visibleCount == oldSize) return
        notifyItemRangeInserted(oldSize, visibleCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchHistoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return WatchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WatchHistoryViewHolder, position: Int) {
        val video = watchHistory[position]
        holder.binding.apply {
            videoTitle.text = video.title
            channelName.text = video.uploader
            videoInfo.text = video.uploadDate?.let { TextUtils.localizeDate(it) }
            thumbnailDuration.setFormattedDuration(video.duration!!, null)
            ImageHelper.loadImage(video.thumbnailUrl, thumbnail)
            ImageHelper.loadImage(video.uploaderAvatar, channelImage)

            channelImage.setOnClickListener {
                NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
            }

            root.setOnClickListener {
                NavigationHelper.navigateVideo(root.context, video.videoId)
            }
            root.setOnLongClickListener {
                VideoOptionsBottomSheet(video.videoId, video.title!!) {
                    notifyItemChanged(position)
                }
                    .show(
                        (root.context as BaseActivity).supportFragmentManager,
                        VideoOptionsBottomSheet::class.java.name
                    )
                true
            }

            watchProgress.setWatchProgressLength(video.videoId, video.duration)
        }
    }
}
