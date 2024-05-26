package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

class CommentsMainFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommentsViewModel by activityViewModels()

    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null

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

        val binding = binding
        val layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.layoutManager = layoutManager
        binding.commentsRV.setItemViewCacheSize(20)

        val commentsSheet = parentFragment as? CommentsSheet
        commentsSheet?.binding?.btnScrollToTop?.setOnClickListener {
            // scroll back to the top / first comment
            _binding?.commentsRV?.smoothScrollToPosition(POSITION_START)
            viewModel.setCommentsPosition(POSITION_START)
        }

        scrollListener = ViewTreeObserver.OnScrollChangedListener {
            // save the last scroll position to become used next time when the sheet is opened
            viewModel.setCommentsPosition(layoutManager.findFirstVisibleItemPosition())
            // hide or show the scroll to top button
            commentsSheet?.binding?.btnScrollToTop?.isVisible =
                viewModel.currentCommentsPosition.value != 0
        }
        binding.commentsRV.viewTreeObserver.addOnScrollChangedListener(scrollListener)

        commentsSheet?.updateFragmentInfo(false, getString(R.string.comments))

        val commentPagingAdapter = CommentPagingAdapter(
            this,
            viewModel.videoIdLiveData.value ?: return,
            requireArguments().getString(IntentData.channelAvatar) ?: return,
            handleLink = {
                setFragmentResult(CommentsSheet.HANDLE_LINK_REQUEST_KEY, bundleOf(IntentData.url to it))
            }
        ) {
            setFragmentResult(CommentsSheet.DISMISS_SHEET_REQUEST_KEY, bundleOf())
        }
        binding.commentsRV.adapter = commentPagingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            var restoredScrollPosition = false

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    commentPagingAdapter.loadStateFlow.collect {
                        binding.progress.isVisible = it.refresh is LoadState.Loading

                        if (!restoredScrollPosition && it.refresh is LoadState.NotLoading) {
                            viewModel.currentCommentsPosition.value?.let { position ->
                                withContext(Dispatchers.Main) {
                                    binding.commentsRV.scrollToPosition(position)
                                }
                            }
                            restoredScrollPosition = true
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

                        val commentCount = commentPagingAdapter.itemCount.toLong().formatShort()
                        commentsSheet?.updateFragmentInfo(
                            false,
                            getString(R.string.comments_count, commentCount)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.commentsRV.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        scrollListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val POSITION_START = 0
    }
}
