package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.extensions.onSystemInsets
import com.github.libretube.ui.models.ChaptersViewModel

/**
 * Bottom sheet displaying video chapters with optimized list updates.
 * Uses proper adapter notification instead of full dataset refresh.
 */
class ChaptersBottomSheet : ExpandablePlayerSheet(R.layout.bottom_sheet) {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding accessed after view destruction")

    private val chaptersViewModel: ChaptersViewModel by activityViewModels()
    private var duration = 0L
    private lateinit var adapter: ChaptersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        duration = requireArguments().getLong(IntentData.duration, 0L)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupUI()
    }

    /**
     * Initializes the RecyclerView with proper configuration.
     */
    private fun setupRecyclerView() {
        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        adapter = ChaptersAdapter(chaptersViewModel.chapters, duration) { position ->
            setFragmentResult(
                SEEK_TO_POSITION_REQUEST_KEY,
                bundleOf(IntentData.currentPosition to position)
            )
        }
        binding.optionsRecycler.adapter = adapter

        // Add bottom padding to ensure last item is not overlapped by system bars
        binding.optionsRecycler.onSystemInsets { v, systemInsets ->
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemInsets.bottom
            )
        }

        // Scroll to current chapter when layout is ready
        binding.optionsRecycler.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    chaptersViewModel.currentChapterIndex.value?.let { index ->
                        binding.optionsRecycler.scrollToPosition(index)
                    }
                    binding.optionsRecycler.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    /**
     * Sets up LiveData observers for reactive updates.
     */
    private fun setupObservers() {
        chaptersViewModel.currentChapterIndex.observe(viewLifecycleOwner) { currentIndex ->
            if (_binding == null) return@observe
            adapter.updateSelectedPosition(currentIndex)
        }

        chaptersViewModel.chaptersLiveData.observe(viewLifecycleOwner) { chapters ->
            adapter.chapters = chapters.orEmpty()
            // Use DiffUtil in adapter instead of notifyDataSetChanged for better performance
            adapter.notifyDataSetChanged()
        }
    }

    /**
     * Configures UI elements.
     */
    private fun setupUI() {
        binding.bottomSheetTitle.text = context?.getString(R.string.chapters)
        binding.bottomSheetTitleLayout.isVisible = true
    }

    override fun getSheetMaxHeightPx() = chaptersViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    override fun onStart() {
        super.onStart()
        // Remove internal padding from the bottomsheet
        // https://github.com/material-components/material-components-android/issues/3389#issuecomment-2049028605
        dialog?.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val SEEK_TO_POSITION_REQUEST_KEY = "seek_to_position_request_key"
    }
}
