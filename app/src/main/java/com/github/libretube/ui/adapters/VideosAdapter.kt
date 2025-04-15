package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.VideoRowBinding
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.NavigationHelper
import com.github.libretube.ui.adapters.callbacks.DiffUtilItemCallback
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.extensions.setFormattedDuration
import com.github.libretube.ui.extensions.setWatchProgressLength
import com.github.libretube.ui.sheets.VideoOptionsBottomSheet
import com.github.libretube.ui.viewholders.VideosViewHolder
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideosAdapter(
    private val showChannelInfo: Boolean = true
) : ListAdapter<StreamItem, VideosViewHolder>(DiffUtilItemCallback()) {

    fun insertItems(newItems: List<StreamItem>) {
        val updatedList = currentList.toMutableList().also {
            it.addAll(newItems)
        }

        submitList(updatedList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideosViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = VideoRowBinding.inflate(layoutInflater, parent, false)
        return VideosViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: VideosViewHolder, position: Int) {
        val video = getItem(holder.bindingAdapterPosition)
        val videoId = video.url.orEmpty().toID()

        val context = holder.binding.root.context
        val activity = (context as BaseActivity)
        val fragmentManager = activity.supportFragmentManager

        with(holder.binding) {
            videoTitle.text = video.title
            videoInfo.text = TextUtils.formatViewsString(root.context, video.views ?: -1, video.uploaded)

            video.duration?.let { thumbnailDuration.setFormattedDuration(it, video.isShort, video.uploaded) }
            watchProgress.setWatchProgressLength(videoId, video.duration ?: 0L)
            ImageHelper.loadImage(video.thumbnail, thumbnail)

            if (showChannelInfo) {
                ImageHelper.loadImage(video.uploaderAvatar, channelImage, true)
                channelName.text = video.uploaderName

                channelContainer.setOnClickListener {
                    NavigationHelper.navigateChannel(root.context, video.uploaderUrl)
                }
            } else {
                channelImageContainer.isGone = true
            }

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
                sheet.show(fragmentManager, VideosAdapter::class.java.name)
                true
            }

            CoroutineScope(Dispatchers.IO).launch {
                val isDownloaded =
                    DatabaseHolder.Database.downloadDao().exists(videoId)

                withContext(Dispatchers.Main) {
                    downloadBadge.isVisible = isDownloaded
                }
            }
        }
    }
}
