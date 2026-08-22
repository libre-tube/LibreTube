package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.enums.WatchHistoryStatus
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WatchHistoryModel : ViewModel() {
    private val watchHistory = MutableLiveData<List<WatchHistoryItem>>()
    val filteredWatchHistory: LiveData<List<WatchHistoryItem>> = watchHistory

    private var cursor: Long? = Long.MAX_VALUE
    private var fetchJob: Job? = null
    private val downloadedVideoIds = mutableSetOf<String>()
    private val watchPositions = mutableMapOf<String, Long?>()

    private val selectedStatus = MutableStateFlow(
        WatchHistoryStatus.entries.getOrNull(
            PreferenceHelper.getInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, WatchHistoryStatus.ALL.ordinal)
        ) ?: WatchHistoryStatus.ALL
    )

    var selectedStatusFilter
        get() = selectedStatus.value
        set(value) {
            PreferenceHelper.putInt(PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER, value.ordinal)
            selectedStatus.value = value
        }

    init {
        viewModelScope.launch {
            selectedStatus.collect {
                fetchJob?.cancel()
                cursor = Long.MAX_VALUE
                watchHistory.value = emptyList()
                fetchNextPage()
            }
        }
    }

    fun fetchNextPage() {
        val currentCursor = cursor ?: return
        if (fetchJob?.isActive == true) return

        fetchJob = viewModelScope.launch {
            val page = DatabaseHelper.getWatchHistoryPage(
                pageSize = HISTORY_PAGE_SIZE,
                statusFilter = selectedStatus.value,
                cursor = currentCursor
            )
            val downloaded = DatabaseHolder.Database.downloadDao()
                .areVideosDownloaded(page.items.map(WatchHistoryItem::videoId))

            page.rows.forEachIndexed { index, row ->
                val item = row.item
                if (downloaded[index]) downloadedVideoIds += item.videoId else downloadedVideoIds -= item.videoId
                watchPositions[item.videoId] = row.watchPosition
            }
            cursor = page.nextCursor
            watchHistory.value = watchHistory.value.orEmpty() + page.items
        }
    }

    fun isVideoDownloaded(videoId: String) = videoId in downloadedVideoIds

    fun getWatchPosition(videoId: String) = watchPositions[videoId]

    fun onWatchStatusChanged(item: WatchHistoryItem, isVideoWatched: Boolean) {
        if (isVideoWatched) {
            watchPositions[item.videoId] = Long.MAX_VALUE
        } else {
            watchPositions -= item.videoId
        }

        if (!isVideoWatched || selectedStatus.value.isWatched == false) {
            watchHistory.value = watchHistory.value.orEmpty() - item
        }
    }

    fun removeFromHistory(watchHistoryItem: WatchHistoryItem) =
        viewModelScope.launch {
            DatabaseHolder.Database.watchHistoryDao().delete(watchHistoryItem)
            watchHistory.value = watchHistory.value.orEmpty() - watchHistoryItem
        }

    companion object {
        private const val HISTORY_PAGE_SIZE = 10
    }
}
