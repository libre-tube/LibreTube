package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.libretube.ui.models.sources.CommentPagingSource
import com.github.libretube.ui.models.sources.CommentRepliesPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class CommentsViewModel : ViewModel() {
    val videoIdLiveData = MutableLiveData<String>()
    val selectedCommentLiveData = MutableLiveData<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsFlow = videoIdLiveData.asFlow()
        .flatMapLatest {
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                CommentPagingSource(it)
            }.flow
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentRepliesFlow = videoIdLiveData.asFlow()
        .combine(selectedCommentLiveData.asFlow()) { videoId, comment -> videoId to comment }
        .flatMapLatest { (videoId, commentPage) ->
            Pager(PagingConfig(20, enablePlaceholders = false)) {
                CommentRepliesPagingSource(videoId, commentPage)
            }.flow
        }
        .cachedIn(viewModelScope)

    val commentSheetExpand = MutableLiveData<Boolean?>()

    var channelAvatar: String? = null
    var handleLink: ((url: String) -> Unit)? = null

    var currentCommentsPosition = 0
    var commentsSheetDismiss: (() -> Unit)? = null

    fun setCommentSheetExpand(value: Boolean?) {
        if (commentSheetExpand.value != value) {
            commentSheetExpand.value = value
        }
    }

    fun reset() {
        setCommentSheetExpand(null)
        currentCommentsPosition = 0
    }
}
