package com.github.libretube.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.QueueRowBinding
import com.github.libretube.ui.viewholders.PlayingQueueViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.PlayingQueue
import com.github.libretube.util.ThemeHelper

class PlayingQueueAdapter : RecyclerView.Adapter<PlayingQueueViewHolder>() {
    private val currentIndex = PlayingQueue.currentIndex()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayingQueueViewHolder {
        val binding = QueueRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayingQueueViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return PlayingQueue.size()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PlayingQueueViewHolder, position: Int) {
        val streamItem = PlayingQueue.getStreams()[position]
        holder.binding.apply {
            ImageHelper.loadImage(streamItem.thumbnail, thumbnail)
            title.text = streamItem.title
            videoInfo.text = streamItem.uploaderName + "  •  " +
                DateUtils.formatElapsedTime(streamItem.duration ?: 0)

            root.setBackgroundColor(
                if (currentIndex == position) {
                    ThemeHelper.getThemeColor(root.context, android.R.attr.colorControlHighlight)
                } else {
                    Color.TRANSPARENT
                }
            )
        }
    }
}
