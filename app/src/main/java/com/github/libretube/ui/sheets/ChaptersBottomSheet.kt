package com.github.libretube.ui.sheets

import android.annotation.SuppressLint
import android.app.Dialog
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
import com.github.libretube.ui.extensions.onSystemInsets
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.ui.adapters.ChaptersAdapter
import com.github.libretube.ui.interfaces.BottomSheetListener
import com.github.libretube.ui.models.ChaptersViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChaptersBottomSheet : ExpandablePlayerSheet(R.layout.bottom_sheet) {
    private var _binding: BottomSheetBinding? = null
    private val binding get() = _binding!!

    private val chaptersViewModel: ChaptersViewModel by activityViewModels()
    private var duration = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        duration = requireArguments().getLong(IntentData.duration, 0L)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                behavior.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dismissAllowingStateLoss()
                                (activity as? BottomSheetListener)?.onBottomSheetDismissed()
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }
                })
            }
        }
        return dialog
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = BottomSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(context)
        val adapter =
            ChaptersAdapter(chaptersViewModel.chapters, duration) {
                setFragmentResult(SEEK_TO_POSITION_REQUEST_KEY, bundleOf(IntentData.currentPosition to it))
            }
        binding.optionsRecycler.adapter = adapter

        // add bottom padding to the list, to ensure that the last item is not overlapped by the system bars
        binding.optionsRecycler.onSystemInsets { v, systemInsets ->
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                systemInsets.bottom
            )
        }


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

    override fun onStart() {
        super.onStart()
        // remove internal padding from the bottomsheet
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
