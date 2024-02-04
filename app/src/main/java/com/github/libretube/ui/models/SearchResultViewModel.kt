package com.github.libretube.ui.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.github.libretube.ui.dialogs.ShareDialog
import com.github.libretube.ui.fragments.SearchResultFragmentArgs
import com.github.libretube.ui.models.sources.SearchPagingSource
import com.github.libretube.util.TextUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class SearchResultViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val args = SearchResultFragmentArgs.fromSavedStateHandle(savedStateHandle)

    // parse search URLs from YouTube entered in the search bar
    private val searchQuery = args.query.toHttpUrlOrNull()?.let {
        val videoId = TextUtils.getVideoIdFromUrl(it.toString()) ?: args.query
        "${ShareDialog.YOUTUBE_FRONTEND_URL}/watch?v=$videoId"
    } ?: args.query

    private val filterMutableData = MutableStateFlow("all")

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsFlow = filterMutableData.flatMapLatest {
        Pager(
            PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { SearchPagingSource(searchQuery, it) }
        ).flow
    }
        .cachedIn(viewModelScope)

    fun setFilter(filter: String) {
        filterMutableData.value = filter
    }
}
