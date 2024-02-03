package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Comment

class CommentRepliesPagingSource(
    private val videoId: String,
    private val commentNextPage: String?,
) : PagingSource<String, Comment>() {
    override fun getRefreshKey(state: PagingState<String, Comment>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val key = params.key.orEmpty().ifEmpty { commentNextPage.orEmpty() }
            val result = RetrofitInstance.api.getCommentsNextPage(videoId, key)
            LoadResult.Page(result.comments, null, result.nextpage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
