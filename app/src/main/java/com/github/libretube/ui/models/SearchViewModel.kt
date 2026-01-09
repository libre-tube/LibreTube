package com.github.libretube.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.SearchDataType
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.SearchDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.map

class SearchViewModel : ViewModel() {
    data class Result(
        val historyList: List<SearchDataItem>? = null,
        val suggestionList: List<SearchDataItem>? = null,
    )

    private val searchQuery = MutableStateFlow<String?>(null)
    private val sharingStarted = SharingStarted.WhileSubscribed(replayExpirationMillis = 0L)

    private val isSearchSuggestionEnabled = flow {
        emit(PreferenceHelper.getBoolean(PreferenceKeys.SEARCH_SUGGESTIONS, true))
    }
        .stateIn(viewModelScope, sharingStarted, true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchHistoryList = DatabaseHolder.Database.searchHistoryDao()
        .getAllNewestFirstFlow()
        .mapLatest { it.map { it.query } }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    /**
     * Emits a [Pair] of ([SearchDataType.HISTORY] and the results ([List]))
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredSearchHistory = combine(searchQuery, searchHistoryList) { query, list ->
        val result =
            if (query.isNullOrBlank()) list
            else list.take(MAX_FILTERED_SEARCH_HISTORY).filter { it.startsWith(query) }

        SearchDataType.HISTORY to result
    }

    /**
     * Emits a [Pair] of ([SearchDataType.SUGGESTION]  and the results ([List]))
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val onlineSearchSuggestions =
        combine(searchQuery, isSearchSuggestionEnabled) { query, suggestionsEnabled ->
            if (!suggestionsEnabled || query.isNullOrBlank()) {
                return@combine emptyList<String>()
            }
            try {
                MediaServiceRepository.instance.getSuggestions(query)
            } catch (e: Exception) {
                Log.e("failed to fetch suggestions", e.stackTraceToString())
                emptyList<String>()
            }
        }.mapLatest { SearchDataType.SUGGESTION to it }

    /**
     * Get filtered search history and search suggestions combined. Both the histories and
     * suggestions are being collected concurrently. Whichever completes the collection first,
     * it will emit ([Result]) immediately with the other list being `null`. Once both sides
     * complete their collections, it will emit another result ([Result]) with both list ready.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchSuggestions = merge(filteredSearchHistory, onlineSearchSuggestions)
        .runningFold(Result()) { prev, new ->
            when (new.first) {
                SearchDataType.HISTORY -> {
                    prev.copy(
                        historyList = new.second.map {
                            SearchDataItem(it, SearchDataType.HISTORY)
                        }
                    )
                }

                SearchDataType.SUGGESTION -> {
                    prev.copy(
                        suggestionList = new.second.map {
                            SearchDataItem(it, SearchDataType.SUGGESTION)
                        }
                    )
                }
            }
        }.flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    val shouldShowEmptyHistoryMessage = combine(
        searchHistoryList,
        searchQuery,
        isSearchSuggestionEnabled
    ) { histories, query, suggestionsEnabled ->
        histories.isEmpty() && (query.isNullOrBlank() || !suggestionsEnabled)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, sharingStarted, true)

    fun setQuery(query: String?) {
        this.searchQuery.value = query
    }

    companion object {
        private const val MAX_FILTERED_SEARCH_HISTORY = 5
    }
}