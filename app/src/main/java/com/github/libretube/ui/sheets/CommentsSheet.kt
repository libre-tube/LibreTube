package com.github.libretube.ui.sheets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.adapters.CommentsAdapter

class CommentsSheet(
    private val videoId: String,
    private val comments: MutableList<Comment>,
    private var nextPage: String?,
    private val onMoreComments: (comments: List<Comment>, nextPage: String?) -> Unit
) : ExpandedBottomSheet() {
    private lateinit var binding: BottomSheetBinding

    private lateinit var commentsAdapter: CommentsAdapter
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.optionsRecycler.setItemViewCacheSize(20)

        binding.optionsRecycler.viewTreeObserver
            .addOnScrollChangedListener {
                if (!binding.optionsRecycler.canScrollVertically(1)) {
                    fetchNextComments()
                }
            }

        commentsAdapter = CommentsAdapter(videoId, comments) {
            dialog?.dismiss()
        }
        binding.optionsRecycler.adapter = commentsAdapter

        if (comments.isEmpty()) fetchComments()
    }

    private fun fetchComments() {
        lifecycleScope.launchWhenCreated {
            isLoading = true
            val response = try {
                RetrofitInstance.api.getComments(videoId)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
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
