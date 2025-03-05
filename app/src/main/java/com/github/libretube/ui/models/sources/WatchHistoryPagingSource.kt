package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchHistoryPagingSource(
    private val shouldIncludeItemPredicate: suspend (WatchHistoryItem) -> Boolean
): PagingSource<Int, WatchHistoryItem>() {
    override fun getRefreshKey(state: PagingState<Int, WatchHistoryItem>) = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WatchHistoryItem> {
        val newHistory = withContext(Dispatchers.IO) {
            DatabaseHelper.getWatchHistoryPage( params.key ?: 0, params.loadSize)
        }.filter { shouldIncludeItemPredicate(it) }

        return LoadResult.Page(newHistory, params.key ?: 0, params.key?.plus(1) ?: 0)
    }
}