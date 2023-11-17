package com.github.libretube.ui.models

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.extensions.TAG
import com.github.libretube.ui.extensions.filterNonEmptyComments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsViewModel : ViewModel() {
    val commentsPage = MutableLiveData<CommentsPage?>()
    val commentSheetExpand = MutableLiveData<Boolean?>()

    var videoId: String? = null
    var channelAvatar: String? = null
    var handleLink: ((url: String) -> Unit)? = null

    private var nextPage: String? = null
    var isLoading = MutableLiveData<Boolean>()
    var currentCommentsPosition = 0
    var commentsSheetDismiss: (() -> Unit)? = null

    fun setCommentSheetExpand(value: Boolean?) {
        if (commentSheetExpand.value != value) {
            commentSheetExpand.value = value
        }
    }

    fun fetchComments() {
        val videoId = videoId ?: return

        isLoading.value = true

        viewModelScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getComments(videoId)
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            } finally {
                isLoading.value = false
            }

            nextPage = response.nextpage
            response.comments = response.comments.filterNonEmptyComments()
            commentsPage.postValue(response)
        }
    }

    fun fetchNextComments() {
        if (isLoading.value == true || nextPage == null || videoId == null) return

        isLoading.value = true

        viewModelScope.launch {
            val response = try {
                withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getCommentsNextPage(videoId!!, nextPage!!)
                }
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            } finally {
                isLoading.value = false
            }

            val updatedPage = commentsPage.value?.apply {
                comments += response.comments
                    .filterNonEmptyComments()
                    .filter { comment -> comments.none { it.commentId == comment.commentId } }
            }

            nextPage = response.nextpage
            commentsPage.postValue(updatedPage)
        }
    }

    fun reset() {
        isLoading.value = false
        nextPage = null
        commentsPage.value = null
        videoId = null
        setCommentSheetExpand(null)
        currentCommentsPosition = 0
    }
}
