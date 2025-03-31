package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentPagingSource(
    private val videoId: String,
    private val onCommentCount: (Long) -> Unit
) : PagingSource<String, Comment>() {
    override fun getRefreshKey(state: PagingState<String, Comment>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Comment> {
        return try {
            val result = withContext(Dispatchers.IO) {
                params.key?.let {
                    MediaServiceRepository.instance.getCommentsNextPage(videoId, it)
                } ?: MediaServiceRepository.instance.getComments(videoId).also {
                    // avoid negative comment counts, i.e. because they're disabled
                    withContext(Dispatchers.Main) {
                        onCommentCount(maxOf(0, it.commentCount))
                    }
                }
            }

            LoadResult.Page(result.comments, null, result.nextpage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
