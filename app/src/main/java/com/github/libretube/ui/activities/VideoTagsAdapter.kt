package com.github.libretube.ui.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.VideoTagRowBinding
import com.github.libretube.ui.viewholders.VideoTagsViewHolder

class VideoTagsAdapter(private val tags: List<String>) :
    RecyclerView.Adapter<VideoTagsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoTagsViewHolder {
        val binding = VideoTagRowBinding.inflate(LayoutInflater.from(parent.context))
        return VideoTagsViewHolder(binding)
    }

    override fun getItemCount() = tags.size

    override fun onBindViewHolder(holder: VideoTagsViewHolder, position: Int) {
        val tag = tags[position]
        holder.binding.apply {
            tagText.text = tag
            root.setOnClickListener {
                val mainActivity = root.context as MainActivity
                mainActivity.searchView.setQuery(tag, true)
                // minimizes the player fragment to the mini player
                mainActivity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}