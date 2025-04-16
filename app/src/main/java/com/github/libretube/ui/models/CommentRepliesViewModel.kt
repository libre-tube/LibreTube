package com.github.libretube.ui.models

import android.content.Context
import androidx.core.text.parseAsHtml
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.libretube.api.obj.Comment
import com.github.libretube.constants.IntentData
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.ui.models.sources.CommentRepliesPagingSource

class CommentRepliesViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val videoId = savedStateHandle.get<String>(IntentData.videoId)!!
    private val comment = savedStateHandle.get<Comment>(IntentData.comment)!!

    val commentRepliesLiveData = Pager(PagingConfig(20, enablePlaceholders = false)) {
        CommentRepliesPagingSource(videoId, comment)
    }
        .liveData
        .cachedIn(viewModelScope)

    fun saveToClipboard(context: Context) {
        ClipboardHelper.save(
            context,
            text = comment.commentText.orEmpty().parseAsHtml().toString(),
            notify = true
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CommentRepliesViewModel(
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}