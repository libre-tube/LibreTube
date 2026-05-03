package com.github.libretube.ui.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchHistoryModel : ViewModel() {
    private val watchHistory = MutableLiveData<List<WatchHistoryEntry>>()

    private var currentPage = 1
    private var isLoading = false

    private val selectedStatus = MutableStateFlow(
        PreferenceHelper.getInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, 0)
    )

    val filteredWatchHistory =
        combine(watchHistory.asFlow(), selectedStatus) { history, _ -> history }
            .flowOn(Dispatchers.IO).map { history -> history.filter { it.shouldIncludeByFilters() } }
            .asLiveData()

    var selectedStatusFilter
        get() = selectedStatus.value
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, value)
            selectedStatus.value = value
        }

    private suspend fun WatchHistoryEntry.shouldIncludeByFilters(): Boolean {
        // no watch position filter
        if (selectedStatusFilter == 0) return true

        return when (selectedStatusFilter) {
            1 -> DatabaseHelper.filterByWatchStatus(this)
            2 -> DatabaseHelper.filterByWatchStatus(this, false)
            else -> throw IllegalArgumentException()
        }
    }

    fun fetchNextPage() = viewModelScope.launch(Dispatchers.IO) {
        if (isLoading) return@launch
        isLoading = true

        val newHistory = withContext(Dispatchers.IO) {
            try {
                UserDataRepositoryHelper.userDataRepository.getWatchHistory(currentPage)
            } catch (e: Exception) {
                // TODO: proper error handling
                return@withContext emptyList()
            }
        }

        isLoading = false
        currentPage++

        watchHistory.postValue(
            watchHistory.value.orEmpty().toMutableList().apply {
                addAll(newHistory)
            }
        )
    }

    fun removeFromHistory(watchHistoryEntry: WatchHistoryEntry) =
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                UserDataRepositoryHelper.userDataRepository.removeFromWatchHistory(watchHistoryEntry.metadata.videoId)

                watchHistory.postValue(
                    watchHistory.value.orEmpty().filter { it != watchHistoryEntry }
                )
            }
        }
}