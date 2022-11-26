package com.github.libretube.ui.sheets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.libretube.R
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.databinding.BottomSheetBinding
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.adapters.CommentsAdapter
import retrofit2.HttpException
import java.io.IOException

class CommentsSheet(
    private val videoId: String
) : ExpandedBottomSheet() {
    private lateinit var binding: BottomSheetBinding

    private var commentsAdapter: CommentsAdapter? = null
    private var nextPage: String? = null
    private var isLoading = true

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

        fetchComments()
    }

    private fun fetchComments() {
        lifecycleScope.launchWhenCreated {
            val commentsResponse = try {
                RetrofitInstance.api.getComments(videoId)
            } catch (e: IOException) {
                println(e)
                Log.e(TAG(), "IOException, you might not have internet connection")
                Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                return@launchWhenCreated
            } catch (e: HttpException) {
                Log.e(TAG(), "HttpException, unexpected response")
                return@launchWhenCreated
            }
            commentsAdapter = CommentsAdapter(videoId, commentsResponse.comments)
            binding.optionsRecycler.adapter = commentsAdapter
            nextPage = commentsResponse.nextpage
            isLoading = false
        }
    }

    private fun fetchNextComments() {
        lifecycleScope.launchWhenCreated {
            if (!isLoading) {
                isLoading = true
                val response = try {
                    RetrofitInstance.api.getCommentsNextPage(videoId, nextPage!!)
                } catch (e: IOException) {
                    println(e)
                    Log.e(TAG(), "IOException, you might not have internet connection")
                    return@launchWhenCreated
                } catch (e: HttpException) {
                    Log.e(TAG(), "HttpException, unexpected response," + e.response())
                    return@launchWhenCreated
                }
                nextPage = response.nextpage
                commentsAdapter?.updateItems(response.comments)
                isLoading = false
            }
        }
    }
}
