package com.github.libretube.ui.sheets

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.ThemeHelper
import com.github.libretube.ui.adapters.ChaptersAdapter

class ChaptersBottomSheet(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
): ExpandedBottomSheet() {
    private lateinit var binding: BottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        binding.optionsRecycler.adapter = ChaptersAdapter(chapters, exoPlayer)

        binding.bottomSheetTitle.text = context?.getString(R.string.chapters)
        binding.bottomSheetTitleLayout.isVisible = true

        val handler = Handler(Looper.getMainLooper())
        val highlightColor =
            ThemeHelper.getThemeColor(requireContext(), android.R.attr.colorControlHighlight)

        val updatePosition = Runnable {
            // scroll to the current playing index in the chapter
            val currentPosition =
                PlayerHelper.getCurrentChapterIndex(exoPlayer, chapters) ?: return@Runnable
            binding.optionsRecycler.smoothScrollToPosition(currentPosition)

            val children = binding.optionsRecycler.children.toList()
            // reset the background colors of all chapters
            children.forEach { it.setBackgroundColor(Color.TRANSPARENT) }
            // highlight the current chapter
            children.getOrNull(currentPosition)?.setBackgroundColor(highlightColor)
        }

        updatePosition.run()
        handler.postDelayed(updatePosition, 200)
    }
}