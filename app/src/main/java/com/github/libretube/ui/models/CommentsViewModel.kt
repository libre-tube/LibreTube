package com.github.libretube.ui.models

import android.content.Context
import androidx.core.text.parseAsHtml
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.libretube.api.obj.Comment
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.ui.models.sources.CommentPagingSource

class CommentsViewModel : ViewModel() {

    private var lastOpenedCommentRepliesId: String? = null
    val videoIdLiveData = MutableLiveData<String>()

    val commentsLiveData = videoIdLiveData.switchMap {
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            CommentPagingSource(it) {
                _commentCountLiveData.updateIfChanged(it)
            }
        }.liveData
    }
        .cachedIn(viewModelScope)

    private val _commentCountLiveData = MutableLiveData<Long>()
    val commentCountLiveData: LiveData<Long> = _commentCountLiveData

    private val _currentCommentsPosition = MutableLiveData(0)
    val currentCommentsPosition: LiveData<Int> = _currentCommentsPosition

    private val _currentRepliesPosition = MutableLiveData(0)
    val currentRepliesPosition: LiveData<Int> = _currentRepliesPosition

    fun reset() {
        _currentCommentsPosition.value = 0
    }

    fun setCommentsPosition(position: Int) {
        if (position != currentCommentsPosition.value) {
            _currentCommentsPosition.value = position
        }
    }

    fun setRepliesPosition(position: Int) {
        if (position != currentRepliesPosition.value) {
            _currentRepliesPosition.value = position
        }
    }

    fun setLastOpenedCommentRepliesId(id: String) {
        if (lastOpenedCommentRepliesId != id) {
            _currentRepliesPosition.value = 0
            lastOpenedCommentRepliesId = id
        }
    }

    fun saveToClipboard(context: Context, comment: Comment) {
        ClipboardHelper.save(
            context,
            text = comment.commentText.orEmpty().parseAsHtml().toString(),
            notify = true
        )
    }
}
