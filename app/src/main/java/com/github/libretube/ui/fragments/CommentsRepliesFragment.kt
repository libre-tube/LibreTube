package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.formatShort
import com.github.libretube.extensions.parcelable
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.extensions.filterNonEmptyComments
import com.github.libretube.ui.models.CommentsViewModel
import com.github.libretube.ui.sheets.CommentsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsRepliesFragment : Fragment() {
    private var _binding: FragmentCommentsBinding? = null

    private lateinit var repliesPage: CommentsPage
    private lateinit var repliesAdapter: CommentsAdapter
    private val viewModel: CommentsViewModel by activityViewModels()

    private var isLoading = false

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
        val arguments = requireArguments()
        val videoId = arguments.getString(IntentData.videoId, "")
        val comment = arguments.parcelable<Comment>(IntentData.comment)!!

        val binding = _binding ?: return

        val commentsSheet = parentFragment as? CommentsSheet
        commentsSheet?.binding?.btnScrollToTop?.isGone = true

        repliesAdapter = CommentsAdapter(
            null,
            videoId,
            viewModel.channelAvatar,
            mutableListOf(comment),
            true,
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

        binding.commentsRV.viewTreeObserver.addOnScrollChangedListener {
            if (_binding?.commentsRV?.canScrollVertically(1) == false && ::repliesPage.isInitialized) {
                if (repliesPage.nextpage == null) return@addOnScrollChangedListener

                fetchReplies(videoId, repliesPage.nextpage!!)
            }
        }

        loadInitialReplies(videoId, comment.repliesPage.orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadInitialReplies(
        videoId: String,
        nextPage: String
    ) {
        _binding?.progress?.isVisible = true
        fetchReplies(videoId, nextPage)
    }

    private fun fetchReplies(videoId: String, nextPage: String) {
        _binding?.progress?.isVisible = true

        lifecycleScope.launch {
            if (isLoading) return@launch
            isLoading = true

            repliesPage = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getCommentsNextPage(videoId, nextPage)
                }
            } catch (e: Exception) {
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launch
            } finally {
                _binding?.progress?.isGone = true
            }
            repliesPage.comments = repliesPage.comments.filterNonEmptyComments()
            withContext(Dispatchers.Main) {
                repliesAdapter.updateItems(repliesPage.comments)
            }
            isLoading = false
        }
    }
}
