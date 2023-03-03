package com.github.libretube.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.constants.IntentData
import com.github.libretube.databinding.FragmentCommentsBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.adapters.CommentsAdapter
import com.github.libretube.ui.extensions.filterNonEmptyComments
import com.github.libretube.ui.models.CommentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsRepliesFragment : Fragment() {
    private lateinit var binding: FragmentCommentsBinding
    private lateinit var repliesPage: CommentsPage
    private lateinit var repliesAdapter: CommentsAdapter
    private val viewModel: CommentsViewModel by activityViewModels()

    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoId = arguments?.getString(IntentData.videoId) ?: ""
        val comment = JsonHelper.json.decodeFromString(
            Comment.serializer(),
            arguments?.getString(IntentData.comment)!!
        )

        repliesAdapter = CommentsAdapter(
            null,
            videoId,
            mutableListOf(comment),
            true,
            viewModel.handleLink
        ) {
            viewModel.commentsSheetDismiss?.invoke()
        }

        binding.commentsRV.updatePadding(top = 0)
        binding.commentsRV.layoutManager = LinearLayoutManager(context)
        binding.commentsRV.adapter = repliesAdapter

        binding.commentsRV.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.commentsRV.canScrollVertically(1) &&
                    ::repliesPage.isInitialized &&
                    repliesPage.nextpage != null
                ) {
                    fetchReplies(videoId, repliesPage.nextpage!!) {
                        repliesAdapter.updateItems(repliesPage.comments)
                    }
                }
            }

        loadInitialReplies(videoId, comment.repliesPage ?: "", repliesAdapter)
    }

    private fun loadInitialReplies(
        videoId: String,
        nextPage: String,
        repliesAdapter: CommentsAdapter
    ) {
        binding.progress.visibility = View.VISIBLE
        fetchReplies(videoId, nextPage) {
            repliesAdapter.updateItems(it.comments)
            binding.progress.visibility = View.GONE
        }
    }

    private fun fetchReplies(
        videoId: String,
        nextPage: String,
        onFinished: (CommentsPage) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (isLoading) return@launch
            isLoading = true
            repliesPage = try {
                RetrofitInstance.api.getCommentsNextPage(videoId, nextPage)
            } catch (e: Exception) {
                Log.e(TAG(), "IOException, you might not have internet connection")
                return@launch
            }
            repliesPage.comments = repliesPage.comments.filterNonEmptyComments()
            withContext(Dispatchers.Main) {
                onFinished.invoke(repliesPage)
            }
            isLoading = false
        }
    }
}
