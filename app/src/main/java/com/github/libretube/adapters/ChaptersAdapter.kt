package com.github.libretube.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.obj.ChapterSegment
import com.google.android.exoplayer2.ExoPlayer
import com.squareup.picasso.Picasso

class ChaptersAdapter(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
) : RecyclerView.Adapter<ChaptersViewHolder>() {
    val TAG = "ChaptersAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cell = layoutInflater.inflate(R.layout.chapter_column, parent, false)
        return ChaptersViewHolder(cell)
    }

    override fun onBindViewHolder(holder: ChaptersViewHolder, position: Int) {
        val chapter = chapters[position]
        val chapterImage = holder.v.findViewById<ImageView>(R.id.chapter_image)
        Picasso.get().load(chapter.image).fit().centerCrop().into(chapterImage)

        val chapterTitle = holder.v.findViewById<TextView>(R.id.chapter_title)
        chapterTitle.text = chapter.title

        holder.v.setOnClickListener {
            val chapterStart = chapter.start!!.toLong() * 1000 // s -> ms
            exoPlayer.seekTo(chapterStart)
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
