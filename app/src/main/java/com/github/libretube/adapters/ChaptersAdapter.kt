package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ChapterColumnBinding
import com.github.libretube.obj.ChapterSegment
import com.github.libretube.util.ConnectionHelper
import com.google.android.exoplayer2.ExoPlayer

class ChaptersAdapter(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
) : RecyclerView.Adapter<ChaptersViewHolder>() {
    val TAG = "ChaptersAdapter"

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

            root.setOnClickListener {
                val chapterStart = chapter.start!! * 1000 // s -> ms
                exoPlayer.seekTo(chapterStart)
            }
        }
    }

    override fun getItemCount(): Int {
        return chapters.size
    }
}

class ChaptersViewHolder(val binding: ChapterColumnBinding) : RecyclerView.ViewHolder(binding.root)
