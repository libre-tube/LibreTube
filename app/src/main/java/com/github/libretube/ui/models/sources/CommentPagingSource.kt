package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment

class CommentPagingSource(
    private val videoId: String,
    private val onCommentCount: (Long) -> Unit
) : PagingSource<String, Comment>() {
    override fun getRefreshKey(state: PagingState<String, Comment>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val result = params.key?.let {
                RetrofitInstance.api.getCommentsNextPage(videoId, it)
            } ?: RetrofitInstance.api.getComments(videoId)

            if (result.commentCount > 0) onCommentCount(result.commentCount)

            LoadResult.Page(result.comments, null, result.nextpage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
