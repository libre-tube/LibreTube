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
import com.github.libretube.extensions.formatShort
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.sheets.CommentsSheet

class CommentsMainFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null

    private lateinit var commentsAdapter: CommentsAdapter

    private val viewModel: CommentsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = _binding ?: return
        val layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.layoutManager = layoutManager
        binding.commentsRV.setItemViewCacheSize(20)

        val commentsSheet = parentFragment as? CommentsSheet
        commentsSheet?.binding?.btnScrollToTop?.setOnClickListener {
            // scroll back to the top / first comment
            _binding?.commentsRV?.smoothScrollToPosition(0)
            viewModel.currentCommentsPosition = 0
        }

        binding.commentsRV.viewTreeObserver.addOnScrollChangedListener {
            val viewBinding = _binding ?: return@addOnScrollChangedListener
            // save the last scroll position to become used next time when the sheet is opened
            viewModel.currentCommentsPosition = layoutManager.findFirstVisibleItemPosition()

            // hide or show the scroll to top button
            commentsSheet?.binding?.btnScrollToTop?.isVisible = viewModel.currentCommentsPosition != 0

            if (!viewBinding.commentsRV.canScrollVertically(1)) {
                viewModel.fetchNextComments()
            }
        }
        commentsSheet?.updateFragmentInfo(false, getString(R.string.comments))

        commentsAdapter = CommentsAdapter(
            this,
            viewModel.videoId ?: return,
            viewModel.channelAvatar ?: return,
            viewModel.commentsPage.value?.comments.orEmpty().toMutableList(),
            handleLink = viewModel.handleLink
        ) {
            viewModel.commentsSheetDismiss?.invoke()
        }
        binding.commentsRV.adapter = commentsAdapter

        if (viewModel.commentsPage.value?.comments.isNullOrEmpty()) {
            viewModel.fetchComments()
        } else {
            binding.commentsRV.scrollToPosition(viewModel.currentCommentsPosition)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            _binding?.progress?.isVisible = it == true
        }

        // listen for new comments to be loaded
        viewModel.commentsPage.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            val viewBinding = _binding ?: return@observe

            if (it.disabled) {
                viewBinding.errorTV.isVisible = true
                return@observe
            }

            commentsSheet?.updateFragmentInfo(
                false,
                "${getString(R.string.comments)} (${it.commentCount.formatShort()})"
            )
            if (it.comments.isEmpty()) {
                viewBinding.errorTV.text = getString(R.string.no_comments_available)
                viewBinding.errorTV.isVisible = true
                return@observe
            }

            // sometimes the received comments have the same size as the existing ones
            // which causes comments.subList to throw InvalidArgumentException
            if (commentsAdapter.itemCount > it.comments.size) return@observe

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
