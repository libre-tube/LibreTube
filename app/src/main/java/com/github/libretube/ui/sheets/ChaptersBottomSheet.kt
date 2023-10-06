package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.models.PlayerViewModel

class ChaptersBottomSheet : UndimmedBottomSheet() {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    private val updatePosition = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            handler.postDelayed(this, 200)

            val player = playerViewModel.player ?: return
            val currentIndex = PlayerHelper.getCurrentChapterIndex(
                player.currentPosition,
                playerViewModel.chapters
            ) ?: return

            val adapter = binding.optionsRecycler.adapter as ChaptersAdapter
            adapter.updateSelectedPosition(currentIndex)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter = ChaptersAdapter(playerViewModel.chapters, playerViewModel.player?.duration ?: 0) {
            playerViewModel.player?.seekTo(it)
        }
        binding.optionsRecycler.adapter = adapter

        binding.bottomSheetTitle.text = context?.getString(R.string.chapters)
        binding.bottomSheetTitleLayout.isVisible = true

        playerViewModel.chaptersLiveData.observe(viewLifecycleOwner) {
            adapter.chapters = it
            adapter.notifyDataSetChanged()
        }

        updatePosition.run()
    }

    override fun getSheetMaxHeightPx() = playerViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
