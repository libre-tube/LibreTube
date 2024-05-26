package com.github.libretube.ui.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.fragment.app.setFragmentResult
import com.github.libretube.R
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.ui.fragments.CommentsMainFragment
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.PlayerViewModel

class CommentsSheet : UndimmedBottomSheet() {
    private var _binding: CommentsSheetBinding? = null
    val binding get() = _binding!!

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val commentsViewModel: CommentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CommentsSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding

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

        commentsViewModel.setCommentSheetExpand(true)
        commentsViewModel.commentSheetExpand.observe(viewLifecycleOwner) {
            when (it) {
                true -> expand()
                false -> expand(true)
                else -> dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getSheetMaxHeightPx() = playerViewModel.maxSheetHeightPx

    override fun getDragHandle() = binding.dragHandle

    override fun getBottomSheet() = binding.standardBottomSheet

    fun updateFragmentInfo(showBackButton: Boolean, title: String) {
        binding.btnBack.isVisible = showBackButton
        binding.commentsTitle.text = title
    }

    companion object {
        const val HANDLE_LINK_REQUEST_KEY = "handle_link_request_key"
        const val DISMISS_SHEET_REQUEST_KEY = "dismiss_sheet_request_key"
    }
}
