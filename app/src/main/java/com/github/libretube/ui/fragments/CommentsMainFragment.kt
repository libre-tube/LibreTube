package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.ui.adapters.CommentPagingAdapter
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.sheets.CommentsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsMainFragment : Fragment(R.layout.fragment_comments) {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommentsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCommentsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.layoutManager = layoutManager
        binding.commentsRV.setItemViewCacheSize(20)

        val commentsSheet = parentFragment as? CommentsSheet
        commentsSheet?.binding?.btnScrollToTop?.setOnClickListener {
            // scroll back to the top / first comment
            _binding?.commentsRV?.smoothScrollToPosition(POSITION_START)
            viewModel.setCommentsPosition(POSITION_START)
        }

        binding.commentsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                // hide or show the scroll to top button
                commentsSheet?.binding?.btnScrollToTop?.isVisible = firstVisiblePosition != 0
                viewModel.setCommentsPosition(firstVisiblePosition)

                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        commentsSheet?.updateFragmentInfo(false, getString(R.string.comments))

        val commentPagingAdapter = CommentPagingAdapter(
            this,
            viewModel.videoIdLiveData.value ?: return,
            requireArguments().getString(IntentData.channelAvatar) ?: return,
            handleLink = {
                setFragmentResult(
                    CommentsSheet.HANDLE_LINK_REQUEST_KEY,
                    bundleOf(IntentData.url to it)
                )
            }
        ) {
            setFragmentResult(CommentsSheet.DISMISS_SHEET_REQUEST_KEY, bundleOf())
        }
        binding.commentsRV.adapter = commentPagingAdapter

        var scrollPositionRestoreRequired = viewModel.currentCommentsPosition.value == 0
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    commentPagingAdapter.loadStateFlow.collect {
                        binding.progress.isVisible = it.refresh is LoadState.Loading

                        if (!scrollPositionRestoreRequired && it.refresh is LoadState.NotLoading) {
                            viewModel.currentCommentsPosition.value?.let { position ->
                                scrollPositionRestoreRequired = false

                                withContext(Dispatchers.Main) {
                                    binding.commentsRV.scrollToPosition(position)
                                }
                            }
                        }

                        if (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && commentPagingAdapter.itemCount == 0) {
                            binding.errorTV.text = getString(R.string.no_comments_available)
                            binding.errorTV.isVisible = true
                            return@collect
                        }
                    }
                }

                launch {
                    viewModel.commentsFlow.collect {
                        commentPagingAdapter.submitData(it)
                    }
                }
            }
        }

        viewModel.commentCountLiveData.observe(viewLifecycleOwner) { commentCount ->
            if (commentCount == null) return@observe

            commentsSheet?.updateFragmentInfo(
                false,
                getString(R.string.comments_count, commentCount.formatShort())
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val POSITION_START = 0
    }
}
