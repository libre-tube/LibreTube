package com.github.libretube.ui.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.enums.WatchHistoryStatus
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class WatchHistoryModel : ViewModel() {
    private val watchHistory = MutableLiveData<List<WatchHistoryEntry>>()
    val filteredWatchHistory: LiveData<List<WatchHistoryEntry>> = watchHistory

    private var cursor: Long? = Long.MAX_VALUE
    private var fetchJob: Job? = null
    private val downloadedVideoIds = mutableSetOf<String>()
    private val watchPositions = mutableMapOf<String, Long?>()

    private val selectedStatus = MutableStateFlow(
        WatchHistoryStatus.entries.getOrNull(
            PreferenceHelper.getInt(
                PreferenceKeys.SELECTED_HISTORY_STATUS_FILTER,
                WatchHistoryStatus.ALL.ordinal
            )
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
            val (watchHistoryItems, nextCursor) = UserDataRepositoryHelper.userDataRepository.getWatchHistory(
                pageSize = HISTORY_PAGE_SIZE,
                watchedState = selectedStatus.value,
                cursor = currentCursor
            )
            val downloaded = DatabaseHolder.Database.downloadDao()
                .areVideosDownloaded(watchHistoryItems.map { it.video.url!!.toID() })

            watchHistoryItems.forEachIndexed { index, item ->
                val videoId = item.video.url!!.toID()
                if (downloaded[index]) {
                    downloadedVideoIds += videoId
                } else {
                    downloadedVideoIds -= videoId
                }
                watchPositions[videoId] = item.metadata.positionMillis
            }
            cursor = nextCursor as? Long?
            watchHistory.value = watchHistory.value.orEmpty() + watchHistoryItems
        }
    }

    fun isVideoDownloaded(videoId: String) = videoId in downloadedVideoIds

    fun getWatchPosition(videoId: String) = watchPositions[videoId]

    fun onWatchStatusChanged(item: WatchHistoryEntry, isVideoWatched: Boolean) {
        val videoId = item.video.url!!.toID()

        if (isVideoWatched) {
            watchPositions[videoId] = Long.MAX_VALUE
        } else {
            watchPositions -= videoId
        }

        if (!isVideoWatched || selectedStatus.value.isWatched == false) {
            watchHistory.value = watchHistory.value.orEmpty() - item
        }
    }

    fun removeFromHistory(watchHistoryEntry: WatchHistoryEntry) =
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                UserDataRepositoryHelper.userDataRepository.removeFromWatchHistory(
                    watchHistoryEntry.metadata.videoId
                )

                watchHistory.postValue(
                    watchHistory.value.orEmpty() - watchHistoryEntry
                )
            }
        }

    companion object {
        private const val HISTORY_PAGE_SIZE = 10
    }
}