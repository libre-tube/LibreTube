package com.github.libretube.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ChapterColumnBinding
import com.github.libretube.obj.ChapterSegment
import com.github.libretube.util.ConnectionHelper
import com.github.libretube.util.ThemeHelper
import com.google.android.exoplayer2.ExoPlayer

class ChaptersAdapter(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
) : RecyclerView.Adapter<ChaptersViewHolder>() {
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ChapterColumnBinding.inflate(layoutInflater, parent, false)
        return ChaptersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChaptersViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.binding.apply {
            ConnectionHelper.loadImage(chapter.image, chapterImage)
            chapterTitle.text = chapter.title

            if (selectedPosition == position) {
                // get the color for highlighted controls
                val color =
                    ThemeHelper.getThemeColor(root.context, android.R.attr.colorControlHighlight)
                chapterLL.setBackgroundColor(color)
            } else chapterLL.setBackgroundColor(Color.TRANSPARENT)
            root.setOnClickListener {
                updateSelectedPosition(position)
                val chapterStart = chapter.start!! * 1000 // s -> ms
                exoPlayer.seekTo(chapterStart)
            }
        }
    }

    fun updateSelectedPosition(newPosition: Int) {
        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    override fun getItemCount(): Int {
        return chapters.size
    }
}

class ChaptersViewHolder(val binding: ChapterColumnBinding) : RecyclerView.ViewHolder(binding.root)
