package com.github.libretube.ui.adapters

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.databinding.ChaptersRowBinding
import com.github.libretube.helpers.ImageHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.ui.viewholders.ChaptersViewHolder

class ChaptersAdapter(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
) : RecyclerView.Adapter<ChaptersViewHolder>() {
    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChaptersViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ChaptersRowBinding.inflate(layoutInflater, parent, false)
        return ChaptersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChaptersViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.binding.apply {
            if (chapter.drawable != null) {
                chapterImage.setImageDrawable(chapter.drawable)
            } else {
                ImageHelper.loadImage(chapter.image, chapterImage)
            }
            chapterTitle.text = chapter.title
            timeStamp.text = DateUtils.formatElapsedTime(chapter.start)

            val chapterEnd = chapters.getOrNull(position + 1)?.start
                ?: (exoPlayer.duration / 1000)
            val durationSpan = chapterEnd - chapter.start
            duration.text = root.context.getString(
                R.string.duration_span,
                DateUtils.formatElapsedTime(durationSpan)
            )

            val color = if (selectedPosition == position) {
                ThemeHelper.getThemeColor(root.context, android.R.attr.colorControlHighlight)
            } else {
                Color.TRANSPARENT
            }
            root.setBackgroundColor(color)

            root.setOnClickListener {
                updateSelectedPosition(position)
                val chapterStart = chapter.start * 1000 // s -> ms
                exoPlayer.seekTo(chapterStart)
            }
        }
    }

    fun updateSelectedPosition(newPosition: Int) {
        if (selectedPosition == newPosition) return

        val oldPosition = selectedPosition
        selectedPosition = newPosition
        notifyItemChanged(oldPosition)
        notifyItemChanged(newPosition)
    }

    override fun getItemCount() = chapters.size
}
