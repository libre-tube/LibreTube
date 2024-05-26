package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.models.ChaptersViewModel

class ChaptersBottomSheet : UndimmedBottomSheet() {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding!!

    private val chaptersViewModel: ChaptersViewModel by activityViewModels()
    private var duration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        duration = requireArguments().getLong(IntentData.duration, 0L)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter =
            ChaptersAdapter(chaptersViewModel.chapters, duration) {
                setFragmentResult(SEEK_TO_POSITION_REQUEST_KEY, bundleOf(IntentData.currentPosition to it))
            }
        binding.optionsRecycler.adapter = adapter

        binding.optionsRecycler.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    chaptersViewModel.currentChapterIndex.value?.let {
                        binding.optionsRecycler.scrollToPosition(it)
                    }

                    binding.optionsRecycler.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )

        chaptersViewModel.currentChapterIndex.observe(viewLifecycleOwner) { currentIndex ->
            if (_binding == null) return@observe

            adapter.updateSelectedPosition(currentIndex)
        }

        binding.bottomSheetTitle.text = context?.getString(R.string.chapters)
        binding.bottomSheetTitleLayout.isVisible = true

        chaptersViewModel.chaptersLiveData.observe(viewLifecycleOwner) {
            adapter.chapters = it
            adapter.notifyDataSetChanged()
        }
    }

    override fun getSheetMaxHeightPx() = chaptersViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val SEEK_TO_POSITION_REQUEST_KEY = "seek_to_position_request_key"
    }
}
