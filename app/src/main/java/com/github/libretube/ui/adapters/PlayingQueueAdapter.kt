package com.github.libretube.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.QueueRowBinding
import com.github.libretube.ui.viewholders.PlayingQueueViewHolder
import com.github.libretube.util.ImageHelper
import com.github.libretube.util.PlayingQueue

class PlayingQueueAdapter() : RecyclerView.Adapter<PlayingQueueViewHolder>() {
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

    override fun onBindViewHolder(holder: PlayingQueueViewHolder, position: Int) {
        val streamItem = PlayingQueue.getStreams()[position]
        holder.binding.apply {
            ImageHelper.loadImage(streamItem.thumbnail, thumbnail)
            title.text = streamItem.title
            uploader.text = streamItem.uploaderName
            duration.text = streamItem.duration?.let {
                DateUtils.formatElapsedTime(it)
            }
        }
    }
}
