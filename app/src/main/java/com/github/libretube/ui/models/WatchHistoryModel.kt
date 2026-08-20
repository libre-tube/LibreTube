package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.dao.WatchHistoryPageItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchHistoryModel : ViewModel() {
    private val watchHistory = MutableLiveData<List<WatchHistoryPageItem>>()
    val filteredWatchHistory: LiveData<List<WatchHistoryPageItem>> = watchHistory

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
            reachedEnd = page.rows.size < HISTORY_PAGE_SIZE
            watchHistory.value = watchHistory.value.orEmpty().toMutableList().apply {
                addAll(page.rows)
            }
        }
    }

    fun refresh() {
        val pageSize = maxOf(watchHistory.value.orEmpty().size, HISTORY_PAGE_SIZE)
        fetchJob?.cancel()
        fetchJob = null
        filterGeneration++
        val generation = filterGeneration
        val requestedStatus = selectedStatusFilter

        fetchJob = viewModelScope.launch {
            val page = withContext(Dispatchers.IO) {
                DatabaseHelper.getWatchHistoryPage(
                    pageSize = pageSize,
                    statusFilter = requestedStatus
                )
            }
            if (generation != filterGeneration) return@launch

            cursor = page.nextCursor ?: Long.MAX_VALUE
            reachedEnd = page.rows.size < pageSize
            watchHistory.value = page.rows
        }
    }

    fun removeFromHistory(watchHistoryItem: WatchHistoryItem) =
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                DatabaseHolder.Database.watchHistoryDao().delete(watchHistoryItem)
            }

            watchHistory.value = watchHistory.value.orEmpty().filter { it.item != watchHistoryItem }
        }

    fun refreshItem(videoId: String) = viewModelScope.launch {
        val generation = filterGeneration
        val row = withContext(Dispatchers.IO) {
            DatabaseHolder.Database.watchHistoryDao().getPageItem(videoId)
        }
        if (generation != filterGeneration) return@launch

        val history = watchHistory.value.orEmpty()
        val index = history.indexOfFirst { it.item.videoId == videoId }
        if (index == -1) return@launch

        val refreshedRow = row?.takeIf {
            when (selectedStatusFilter) {
                0 -> true
                1 -> it.watchPosition?.let { position ->
                    !DatabaseHelper.isVideoWatched(position, it.item.duration)
                } ?: true
                2 -> it.watchPosition?.let { position ->
                    DatabaseHelper.isVideoWatched(position, it.item.duration)
                } ?: false
                else -> false
            }
        }

        watchHistory.value = history.toMutableList().apply {
            removeAt(index)
            if (refreshedRow != null) {
                add(if (refreshedRow.rowId > history.first().rowId) 0 else index, refreshedRow)
            }
        }
    }

    companion object {
        private const val HISTORY_PAGE_SIZE = 30
    }
}
