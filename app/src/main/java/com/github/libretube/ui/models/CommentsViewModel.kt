package com.github.libretube.ui.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.extensions.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommentsViewModel : ViewModel() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isLoading = false

    val commentsPage = MutableLiveData<CommentsPage?>()

    private var nextPage: String? = null

    var videoId: String? = null
    var maxHeight: Int = 0

    fun fetchComments() {
        videoId ?: return
        scope.launch {
            isLoading = true
            val response = try {
                RetrofitInstance.api.getComments(videoId!!)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            }
            nextPage = response.nextpage
            commentsPage.postValue(response)
            isLoading = false
        }
    }

    fun fetchNextComments() {
        if (isLoading || nextPage == null || videoId == null) return
        scope.launch {
            isLoading = true
            val response = try {
                RetrofitInstance.api.getCommentsNextPage(videoId!!, nextPage!!)
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            }
            val updatedPage = commentsPage.value?.apply {
                comments = comments.plus(response.comments).toMutableList()
            }
            nextPage = response.nextpage
            commentsPage.postValue(updatedPage)
            isLoading = false
        }
    }

    fun reset() {
        isLoading = false
        nextPage = null
        commentsPage.value = null
        videoId = null
    }
}
