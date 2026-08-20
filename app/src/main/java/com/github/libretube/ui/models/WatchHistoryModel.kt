package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchHistoryModel : ViewModel() {
    private val watchHistory = MutableLiveData<List<WatchHistoryItem>>()
    val filteredWatchHistory: LiveData<List<WatchHistoryItem>> = watchHistory

    private var cursor = Long.MAX_VALUE
    private var fetchJob: Job? = null
    private var reachedEnd = false
    private var filterGeneration = 0
    private var statusFilter =
        PreferenceHelper.getInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, 0)

    var selectedStatusFilter
        get() = statusFilter
        set(value) {
            if (statusFilter == value) return

            PreferenceHelper.putInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, value)
            statusFilter = value
            fetchJob?.cancel()
            fetchJob = null
            filterGeneration++
            cursor = Long.MAX_VALUE
            reachedEnd = false
            watchHistory.value = emptyList()
            fetchNextPage()
        }

    fun fetchNextPage() {
        if (fetchJob?.isActive == true || reachedEnd) return

        val requestedCursor = cursor
        val generation = filterGeneration
        val requestedStatus = selectedStatusFilter
        fetchJob = viewModelScope.launch {
            val page = withContext(Dispatchers.IO) {
                DatabaseHelper.getWatchHistoryPage(
                    pageSize = HISTORY_PAGE_SIZE,
                    statusFilter = requestedStatus,
                    cursor = requestedCursor
                )
            }

            if (generation != filterGeneration || requestedCursor != cursor) return@launch

            cursor = page.nextCursor ?: cursor
            reachedEnd = page.items.size < HISTORY_PAGE_SIZE
            watchHistory.value = watchHistory.value.orEmpty().toMutableList().apply {
                addAll(page.items)
            }
        }
    }

    fun removeFromHistory(watchHistoryItem: WatchHistoryItem) =
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                DatabaseHolder.Database.watchHistoryDao().delete(watchHistoryItem)
            }

            watchHistory.value = watchHistory.value.orEmpty().filter { it != watchHistoryItem }
        }

    companion object {
        private const val HISTORY_PAGE_SIZE = 10
    }
}