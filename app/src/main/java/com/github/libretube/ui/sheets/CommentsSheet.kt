package com.github.libretube.ui.sheets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment
import com.github.libretube.databinding.CommentsSheetBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.adapters.CommentsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentsSheet(
    private val videoId: String,
    private val comments: List<Comment>,
    private var nextPage: String?,
    private val onMoreComments: (comments: List<Comment>, nextPage: String?) -> Unit
) : ExpandedBottomSheet() {
    private lateinit var binding: CommentsSheetBinding

    private lateinit var commentsAdapter: CommentsAdapter
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CommentsSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.commentsRV.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRV.setItemViewCacheSize(20)

        binding.commentsRV.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.commentsRV.canScrollVertically(1)) {
                    fetchNextComments()
                }
            }

        commentsAdapter = CommentsAdapter(videoId, comments.toMutableList()) {
            dialog?.dismiss()
        }
        binding.commentsRV.adapter = commentsAdapter

        if (comments.isEmpty()) fetchComments()
    }

    private fun fetchComments() {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launchWhenCreated {
            isLoading = true
            val response = try {
                RetrofitInstance.api.getComments(videoId)
            } catch (e: Exception) {
                return@launchWhenCreated
            }
            binding.progress.visibility = View.GONE
            if (response.disabled == true) {
                withContext(Dispatchers.Main) {
                    binding.errorTV.visibility = View.VISIBLE
                }
                return@launchWhenCreated
            }
            if (response.comments.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.errorTV.text = getString(R.string.no_comments_available)
                    binding.errorTV.visibility = View.VISIBLE
                }
                return@launchWhenCreated
            }
            commentsAdapter.updateItems(response.comments)
            nextPage = response.nextpage
            onMoreComments.invoke(response.comments, response.nextpage)
            isLoading = false
        }
    }

    private fun fetchNextComments() {
        if (isLoading || nextPage == null) return
        lifecycleScope.launchWhenCreated {
            isLoading = true
            val response = try {
                RetrofitInstance.api.getCommentsNextPage(videoId, nextPage!!)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launchWhenCreated
            }
            nextPage = response.nextpage
            commentsAdapter.updateItems(response.comments)
            onMoreComments.invoke(response.comments, response.nextpage)
            isLoading = false
        }
    }
}
