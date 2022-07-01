package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.databinding.ChapterColumnBinding
import com.github.libretube.obj.ChapterSegment
import com.google.android.exoplayer2.ExoPlayer
import com.squareup.picasso.Picasso

class ChaptersAdapter(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
) : RecyclerView.Adapter<ChaptersViewHolder>() {
    val TAG = "ChaptersAdapter"
    private lateinit var binding: ChapterColumnBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = ChapterColumnBinding.inflate(layoutInflater, parent, false)
        return ChaptersViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ChaptersViewHolder, position: Int) {
        val chapter = chapters[position]
        binding.apply {
            Picasso.get().load(chapter.image).fit().centerCrop().into(chapterImage)
            chapterTitle.text = chapter.title

            root.setOnClickListener {
                val chapterStart = chapter.start!!.toLong() * 1000 // s -> ms
                exoPlayer.seekTo(chapterStart)
            }
        }
    }

    override fun getItemCount(): Int {
        return chapters.size
    }
}

class ChaptersViewHolder(val v: View) : RecyclerView.ViewHolder(v) {
    init {
    }
}
