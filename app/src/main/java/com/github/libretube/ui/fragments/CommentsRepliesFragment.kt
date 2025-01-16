package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.libretube.R
import com.github.libretube.api.obj.Comment
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.parcelable
import com.github.libretube.ui.adapters.CommentPagingAdapter
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.models.sources.CommentRepliesPagingSource
import com.github.libretube.ui.sheets.CommentsSheet
import kotlinx.coroutines.launch

class CommentsRepliesFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

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
        val arguments = requireArguments()
        val videoId = arguments.getString(IntentData.videoId, "")
        val comment = arguments.parcelable<Comment>(IntentData.comment)!!

        val binding = binding

        val commentsSheet = parentFragment as? CommentsSheet
        commentsSheet?.binding?.btnScrollToTop?.isGone = true

        val repliesAdapter = CommentPagingAdapter(
            null,
            videoId,
            requireArguments().getString(IntentData.channelAvatar) ?: return,
            isRepliesAdapter = true,
            handleLink = {
                setFragmentResult(CommentsSheet.HANDLE_LINK_REQUEST_KEY, bundleOf(IntentData.url to it))
            }
        ) {
            setFragmentResult(CommentsSheet.DISMISS_SHEET_REQUEST_KEY, bundleOf())
        }
        commentsSheet?.updateFragmentInfo(
            true,
            "${getString(R.string.replies)} (${comment.replyCount.formatShort()})"
        )

        binding.commentsRV.updatePadding(top = 0)

        val layoutManager = LinearLayoutManager(context)
        binding.commentsRV.layoutManager = layoutManager

        binding.commentsRV.adapter = repliesAdapter

        // init scroll position
        if (viewModel.currentRepliesPosition.value != null) {
            if (viewModel.currentRepliesPosition.value!! > POSITION_START) {
                layoutManager.scrollToPosition(viewModel.currentRepliesPosition.value!!)
            } else {
                layoutManager.scrollToPosition(POSITION_START)
            }
        }

        commentsSheet?.binding?.btnScrollToTop?.setOnClickListener {
            // scroll back to the top / first comment
            layoutManager.scrollToPosition(POSITION_START)
            viewModel.setRepliesPosition(POSITION_START)
        }

        binding.commentsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return

                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                // hide or show the scroll to top button
                commentsSheet?.binding?.btnScrollToTop?.isVisible = firstVisiblePosition != 0
                viewModel.setRepliesPosition(firstVisiblePosition)

                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        val commentRepliesFlow = Pager(PagingConfig(20, enablePlaceholders = false)) {
            CommentRepliesPagingSource(videoId, comment)
        }.flow

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repliesAdapter.loadStateFlow.collect {
                        binding.progress.isVisible = it.refresh is LoadState.Loading
                    }
                }

                launch {
                    commentRepliesFlow.collect {
                        repliesAdapter.submitData(it)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val POSITION_START = 0
    }
}
