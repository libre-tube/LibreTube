package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.libretube.ui.models.sources.CommentPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest

class CommentsViewModel : ViewModel() {
    val videoIdLiveData = MutableLiveData<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsFlow = videoIdLiveData.asFlow()
        .flatMapLatest {
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                CommentPagingSource(it)
            }.flow
        }
        .cachedIn(viewModelScope)

    val commentSheetExpand = MutableLiveData<Boolean?>()

    private val _currentCommentsPosition = MutableLiveData(0)
    val currentCommentsPosition: LiveData<Int> = _currentCommentsPosition

    private val _currentRepliesPosition = MutableLiveData(0)
    val currentRepliesPosition: LiveData<Int> = _currentRepliesPosition

    fun setCommentSheetExpand(value: Boolean?) {
        if (commentSheetExpand.value != value) {
            commentSheetExpand.value = value
        }
    }

    fun reset() {
        setCommentSheetExpand(null)
        _currentCommentsPosition.value = 0
    }

    fun setCommentsPosition(position: Int) {
        if (position != currentCommentsPosition.value) {
            _currentCommentsPosition.postValue(position)
        }
    }

    fun setRepliesPosition(position: Int) {
        if (position != currentRepliesPosition.value) {
            _currentRepliesPosition.postValue(position)
        }
    }
}
