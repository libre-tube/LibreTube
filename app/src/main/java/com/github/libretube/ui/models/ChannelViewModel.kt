package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.libretube.api.obj.ChannelTab
import com.github.libretube.api.obj.ContentItem
import com.github.libretube.extensions.updateIfChanged
import com.github.libretube.ui.models.sources.ChannelTabPagingSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ChannelViewModel : ViewModel() {
    private val tabs = MutableStateFlow<Map<ChannelTab, String?>>(emptyMap())
    private val tabsSortingOptions = MutableLiveData<Map<ChannelTab, Map<String, String>>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun tabData(
        tab: ChannelTab,
    ): Flow<PagingData<ContentItem>> {
        return tabs
            .map { it[tab] }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                Pager(
                    config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                    initialKey = key
                ) {
                    ChannelTabPagingSource(tab) { channelTabs ->
                        tabsSortingOptions.updateIfChanged(
                            tabsSortingOptions.value!! + (tab to channelTabs))
                    }
                }.flow
            }
            .cachedIn(viewModelScope)
    }

    /**
     * Returns the available sorting options for the given tab.
     */
    fun sortingOptions(tab: ChannelTab): LiveData<Map<String, String>?> =
        tabsSortingOptions.map { it[tab] }

    /**
     * Sets the sortingKey for the given tab.
     */
    fun selectSort(tab: ChannelTab, sortKey: String) {
        tabs.update { current -> current + (tab to sortKey) }
    }
}