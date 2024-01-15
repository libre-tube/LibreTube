package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys.HIDE_WATCHED_FROM_FEED
import com.github.libretube.constants.PreferenceKeys.SAVE_FEED
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.enums.ContentFilter
import com.github.libretube.helpers.LocaleHelper
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class HomeViewModel: ViewModel() {

    private val useSavedFeed get() = PreferenceHelper.getBoolean(SAVE_FEED, false)
    private val hideWatched get() = PreferenceHelper.getBoolean(HIDE_WATCHED_FROM_FEED, false)

    val trending: LiveData<List<StreamItem>?> get() = _trending
    private val _trending: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val feed: LiveData<List<StreamItem>?> get() = _feed
    private val _feed: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val bookmarks: LiveData<List<PlaylistBookmark>?> get() = _bookmarks
    private val _bookmarks: MutableLiveData<List<PlaylistBookmark>> = MutableLiveData(null)

    val playlists: LiveData<List<Playlists>?> get() = _playlists
    private val _playlists: MutableLiveData<List<Playlists>> = MutableLiveData(null)

    val continueWatching: LiveData<List<StreamItem>?> get() = _continueWatching
    private val _continueWatching: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)

    val loadedSuccessfully: LiveData<Boolean> get() = _loadedSuccessfully
    private val _loadedSuccessfully: MutableLiveData<Boolean> = MutableLiveData(false)

    private var loadHomeJob: Job? = null

    fun loadHomeFeed(
        context: Context,
        savedFeed: List<StreamItem>? = null,
        visibleItems: Set<String>,
        onUnusualLoadTime: () -> Unit
    ) {
        _isLoading.value = true

        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            val result = async {
                awaitAll(
                    async { if (visibleItems.contains(TRENDING)) loadTrending(context) },
                    async { if (visibleItems.contains(FEATURED)) loadFeed(savedFeed) },
                    async { if (visibleItems.contains(BOOKMARKS)) loadBookmarks() },
                    async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists() },
                    async { if (visibleItems.contains(WATCHING)) loadVideosToContinueWatching() }
                )
                _loadedSuccessfully.value = trending.value.isNullOrEmpty() == false
                _isLoading.value = false
            }

            withContext(Dispatchers.IO) {
                delay(UNUSUAL_LOAD_TIME_MS)
                if (result.isActive) {
                    onUnusualLoadTime.invoke()
                }
            }
        }
    }
    private suspend fun loadTrending(context: Context) {
        val region = LocaleHelper.getTrendingRegion(context)

        runSafely(
            onSuccess = { videos -> _trending.updateIfChanged(videos) },
            ioBlock = { RetrofitInstance.api.getTrending(region).deArrow().take(10) }
        )
    }

    private suspend fun loadFeed(savedFeed: List<StreamItem>? = null) {
        runSafely(
            onSuccess = { videos -> _feed.updateIfChanged(videos) },
            ioBlock = { tryLoadFeed(savedFeed) }
        )
    }

    private suspend fun loadBookmarks() {
        runSafely(
            onSuccess = { bookmarks -> _bookmarks.updateIfChanged(bookmarks) },
            ioBlock = { DatabaseHolder.Database.playlistBookmarkDao().getAll() }
        )
    }

    private suspend fun loadPlaylists() {
        runSafely(
            onSuccess = { playlists -> _playlists.updateIfChanged(playlists) },
            ioBlock = { PlaylistsHelper.getPlaylists().take(20) }
        )
    }

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return
        runSafely(
            onSuccess = { videos -> _continueWatching.updateIfChanged(videos) },
            ioBlock = ::loadWatchingFromDB
        )
    }

    private suspend fun loadWatchingFromDB(): List<StreamItem> {
        val videos = DatabaseHolder.Database.watchHistoryDao().getAll()
        return DatabaseHelper
            .filterUnwatched(videos.map { it.toStreamItem() })
            .reversed()
            .take(20)
    }

    private suspend fun tryLoadFeed(savedFeed: List<StreamItem>?): List<StreamItem> {
        val feed = if (useSavedFeed && !savedFeed.isNullOrEmpty()) {
            savedFeed
        } else {
            SubscriptionHelper.getFeed()
        }

        return if (hideWatched) feed.filterWatched() else feed
    }

    private suspend fun List<StreamItem>.filterWatched(): List<StreamItem> {
        val allowShorts = ContentFilter.SHORTS.isEnabled()
        val allowVideos = ContentFilter.VIDEOS.isEnabled()
        val allowAll = (!allowShorts && !allowVideos)

        val filteredFeed = this.filter {
            allowAll || (allowShorts && it.isShort) || (allowVideos && !it.isShort)
        }
        return runBlocking { DatabaseHelper.filterUnwatched(filteredFeed) }
    }

    private suspend fun <T> runSafely(
        onSuccess: (List<T>) -> Unit = { },
        ioBlock: suspend () -> List<T>,
    ) {
        withContext(Dispatchers.IO) {
            val result = runCatching { ioBlock.invoke() }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() } ?: return@withContext

            withContext(Dispatchers.Main) {
                if (result.isNotEmpty()) {
                    onSuccess.invoke(result)
                }
            }
        }
    }

    private fun <T> MutableLiveData<T>.updateIfChanged(newValue: T) {
        if (value != newValue) value = newValue
    }

    companion object {
        private const val UNUSUAL_LOAD_TIME_MS = 10000L
        private const val FEATURED = "featured"
        private const val WATCHING = "watching"
        private const val TRENDING = "trending"
        private const val BOOKMARKS = "bookmarks"
        private const val PLAYLISTS = "playlists"
    }
}