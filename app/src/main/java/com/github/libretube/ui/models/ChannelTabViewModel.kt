package com.github.libretube.ui.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.constants.IntentData
import com.github.libretube.ui.models.sources.ChannelTabPagingSource
import kotlinx.coroutines.flow.Flow

class ChannelTabViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val tab: ChannelTab = requireNotNull(savedStateHandle.get<ChannelTab>(IntentData.tabData))

    val pagingData: Flow<PagingData<ContentItem>> =
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            ChannelTabPagingSource(tab)
        }.flow.cachedIn(viewModelScope)
}
