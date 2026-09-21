package com.github.libretube.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.ui.models.sources.ChannelTabPagingSource
import kotlinx.coroutines.flow.Flow

class ChannelViewModel : ViewModel() {
    private val pagingFlows = mutableMapOf<String, Flow<PagingData<ContentItem>>>()

    fun tabData(tab: ChannelTab): Flow<PagingData<ContentItem>> =
        pagingFlows.getOrPut(tab.name) {
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
                ChannelTabPagingSource(tab)
            }.flow.cachedIn(viewModelScope)
        }
}