package com.github.libretube.ui.sheets

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.adapters.ChaptersAdapter

class ChaptersBottomSheet(
    private val chapters: List<ChapterSegment>,
    private val exoPlayer: ExoPlayer
): ExpandedBottomSheet() {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter = ChaptersAdapter(chapters, exoPlayer)
        binding.optionsRecycler.adapter = adapter

        binding.bottomSheetTitle.text = context?.getString(R.string.chapters)
        binding.bottomSheetTitleLayout.isVisible = true

        val handler = Handler(Looper.getMainLooper())

        val updatePosition = object: Runnable {
            override fun run() {
                if (_binding == null) return
                handler.postDelayed(this, 200)
                val currentIndex = PlayerHelper.getCurrentChapterIndex(exoPlayer, chapters) ?: return
                adapter.updateSelectedPosition(currentIndex)
            }
        }
        updatePosition.run()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}