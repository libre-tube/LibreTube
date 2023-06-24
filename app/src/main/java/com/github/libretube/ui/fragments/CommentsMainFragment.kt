package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.sheets.CommentsSheet

class CommentsMainFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var commentsAdapter: CommentsAdapter

    private val viewModel: CommentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = _binding ?: return
        val layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.layoutManager = layoutManager
        binding.commentsRV.setItemViewCacheSize(20)

        binding.commentsRV.viewTreeObserver.addOnScrollChangedListener {
            val viewBinding = _binding ?: return@addOnScrollChangedListener
            // save the last scroll position to become used next time when the sheet is opened
            viewModel.currentCommentsPosition = layoutManager.findFirstVisibleItemPosition()

            // hide or show the scroll to top button
            val commentsSheetBinding = (parentFragment as? CommentsSheet)?.binding
            commentsSheetBinding?.btnScrollToTop?.isVisible = viewModel.currentCommentsPosition != 0
            commentsSheetBinding?.btnScrollToTop?.setOnClickListener {
                // scroll back to the top / first comment
                viewBinding.commentsRV.smoothScrollToPosition(0)
                viewModel.currentCommentsPosition = 0
            }

            if (!viewBinding.commentsRV.canScrollVertically(1)) {
                viewModel.fetchNextComments()
            }
        }

        commentsAdapter = CommentsAdapter(
            this,
            viewModel.videoId!!,
            viewModel.commentsPage.value?.comments.orEmpty().toMutableList(),
            handleLink = viewModel.handleLink
        ) {
            viewModel.commentsSheetDismiss?.invoke()
        }
        binding.commentsRV.adapter = commentsAdapter

        if (viewModel.commentsPage.value?.comments.orEmpty().isEmpty()) {
            binding.progress.visibility = View.VISIBLE
            viewModel.fetchComments()
        } else {
            binding.commentsRV.scrollToPosition(viewModel.currentCommentsPosition)
        }

        // listen for new comments to be loaded
        viewModel.commentsPage.observe(viewLifecycleOwner) {
            it ?: return@observe
            binding.progress.visibility = View.GONE
            if (it.disabled) {
                binding.errorTV.visibility = View.VISIBLE
                return@observe
            }
            if (it.comments.isEmpty()) {
                binding.errorTV.text = getString(R.string.no_comments_available)
                binding.errorTV.visibility = View.VISIBLE
                return@observe
            }
            commentsAdapter.updateItems(
                // only add the new comments to the recycler view
                it.comments.subList(commentsAdapter.itemCount, it.comments.size)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
