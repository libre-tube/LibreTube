package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ContentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchPagingSource(
    private val searchQuery: String,
    private val searchFilter: String,
    private val onSearchSuggestion: (Pair<String, Boolean>?) -> Unit
) : PagingSource<String, ContentItem>() {
    override fun getRefreshKey(state: PagingState<String, ContentItem>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ContentItem> {
        return try {
            val result = withContext(Dispatchers.IO) {
                params.key?.let {
                    MediaServiceRepository.instance.getSearchResultsNextPage(
                        searchQuery, searchFilter, it
                    )
                } ?: MediaServiceRepository.instance.getSearchResults(searchQuery, searchFilter)
                    .also {
                        if (it.suggestion.isNullOrEmpty()) onSearchSuggestion(null)
                        else onSearchSuggestion(it.suggestion to it.corrected)
                    }
            }

            LoadResult.Page(result.items, null, result.nextpage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
