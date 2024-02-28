package com.github.libretube.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.obj.Comment
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.parcelable
import com.github.libretube.ui.adapters.CommentPagingAdapter
import com.github.libretube.ui.models.CommentsViewModel
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
            viewModel.channelAvatar,
            comment,
            viewModel.handleLink
        ) {
            viewModel.commentsSheetDismiss?.invoke()
        }
        (parentFragment as CommentsSheet).updateFragmentInfo(
            true,
            "${getString(R.string.replies)} (${comment.replyCount.formatShort()})"
        )

        binding.commentsRV.updatePadding(top = 0)
        binding.commentsRV.layoutManager = LinearLayoutManager(context)
        binding.commentsRV.adapter = repliesAdapter

        viewModel.selectedCommentLiveData.postValue(comment.repliesPage)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    repliesAdapter.loadStateFlow.collect {
                        binding.progress.isVisible = it.refresh is LoadState.Loading
                    }
                }

                launch {
                    viewModel.commentRepliesFlow.collect {
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
}
