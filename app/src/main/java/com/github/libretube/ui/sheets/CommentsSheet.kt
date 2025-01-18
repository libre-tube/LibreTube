package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.ui.fragments.CommentsMainFragment
import com.github.libretube.ui.models.CommonPlayerViewModel

class CommentsSheet : ExpandablePlayerSheet(R.layout.comments_sheet) {
    private var _binding: CommentsSheetBinding? = null
    val binding get() = _binding!!

    private val commonPlayerViewModel: CommonPlayerViewModel by activityViewModels()

    private val backPressedCallback by lazy(LazyThreadSafetyMode.NONE) {
        (dialog as ComponentDialog?)?.onBackPressedDispatcher?.addCallback(owner = viewLifecycleOwner, enabled = false) {
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStack()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = CommentsSheetBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.setFragmentResultListener(DISMISS_SHEET_REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            dismiss()
        }

        // forward requests to open links to the parent fragment
        childFragmentManager.setFragmentResultListener(HANDLE_LINK_REQUEST_KEY, viewLifecycleOwner) { key, result ->
            parentFragment?.setFragmentResult(key, result)
        }

        binding.btnBack.setOnClickListener {
            if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStack()
            }
        }

        binding.btnClose.setOnClickListener { dismiss() }

        childFragmentManager.commit {
            replace<CommentsMainFragment>(R.id.commentFragContainer, args = arguments)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getSheetMaxHeightPx() = commonPlayerViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    fun updateFragmentInfo(showBackButton: Boolean, title: String) {
        binding.btnBack.isVisible = showBackButton
        binding.commentsTitle.text = title
        backPressedCallback?.isEnabled = showBackButton
    }

    companion object {
        const val HANDLE_LINK_REQUEST_KEY = "handle_link_request_key"
        const val DISMISS_SHEET_REQUEST_KEY = "dismiss_sheet_request_key"
    }
}
