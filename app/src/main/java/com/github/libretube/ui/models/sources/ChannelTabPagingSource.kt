package com.github.libretube.ui.models.sources

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelTabPagingSource(
    private val tab: ChannelTab
): PagingSource<String, ContentItem>() {
    override fun getRefreshKey(state: PagingState<String, ContentItem>) = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ContentItem> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                MediaServiceRepository.instance.getChannelTab(tab.data, params.key).apply {
                    content = content.deArrow()
                }
            }
            LoadResult.Page(resp.content, null, resp.nextpage)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}