package com.github.libretube.ui.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys.HIDE_WATCHED_FROM_FEED
import com.github.libretube.db.DatabaseHelper
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.enums.ContentFilter
import com.github.libretube.extensions.runSafely
import com.github.libretube.extensions.updateIfChanged
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
    private val hideWatched get() = PreferenceHelper.getBoolean(HIDE_WATCHED_FROM_FEED, false)

    val trending: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val feed: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val bookmarks: MutableLiveData<List<PlaylistBookmark>> = MutableLiveData(null)

    val playlists: MutableLiveData<List<Playlists>> = MutableLiveData(null)

    val continueWatching: MutableLiveData<List<StreamItem>> = MutableLiveData(null)

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)

    val loadedSuccessfully: MutableLiveData<Boolean> = MutableLiveData(false)

    private var loadHomeJob: Job? = null

    fun loadHomeFeed(
        context: Context,
        subscriptionsViewModel: SubscriptionsViewModel,
        visibleItems: Set<String>,
        onUnusualLoadTime: () -> Unit
    ) {
        isLoading.value = true

        loadHomeJob?.cancel()
        loadHomeJob = viewModelScope.launch {
            val result = async {
                awaitAll(
                    async { if (visibleItems.contains(TRENDING)) loadTrending(context) },
                    async { if (visibleItems.contains(FEATURED)) loadFeed(subscriptionsViewModel) },
                    async { if (visibleItems.contains(BOOKMARKS)) loadBookmarks() },
                    async { if (visibleItems.contains(PLAYLISTS)) loadPlaylists() },
                    async { if (visibleItems.contains(WATCHING)) loadVideosToContinueWatching() }
                )
                loadedSuccessfully.value = trending.value.isNullOrEmpty() == false
                isLoading.value = false
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
            onSuccess = { videos -> trending.updateIfChanged(videos) },
            ioBlock = { RetrofitInstance.api.getTrending(region).deArrow().take(10) }
        )
    }

    private suspend fun loadFeed(subscriptionsViewModel: SubscriptionsViewModel) {
        runSafely(
            onSuccess = { videos -> feed.updateIfChanged(videos) },
            ioBlock = { tryLoadFeed(subscriptionsViewModel) }
        )
    }

    private suspend fun loadBookmarks() {
        runSafely(
            onSuccess = { newBookmarks -> bookmarks.updateIfChanged(newBookmarks) },
            ioBlock = { DatabaseHolder.Database.playlistBookmarkDao().getAll() }
        )
    }

    private suspend fun loadPlaylists() {
        runSafely(
            onSuccess = { newPlaylists -> playlists.updateIfChanged(newPlaylists) },
            ioBlock = { PlaylistsHelper.getPlaylists().take(20) }
        )
    }

    private suspend fun loadVideosToContinueWatching() {
        if (!PlayerHelper.watchHistoryEnabled) return
        runSafely(
            onSuccess = { videos -> continueWatching.updateIfChanged(videos) },
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

    private suspend fun tryLoadFeed(subscriptionsViewModel: SubscriptionsViewModel): List<StreamItem> {
        subscriptionsViewModel.videoFeed.value?.let { return it }

        val feed = SubscriptionHelper.getFeed()
        subscriptionsViewModel.videoFeed.postValue(feed)

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

    companion object {
        private const val UNUSUAL_LOAD_TIME_MS = 10000L
        private const val FEATURED = "featured"
        private const val WATCHING = "watching"
        private const val TRENDING = "trending"
        private const val BOOKMARKS = "bookmarks"
        private const val PLAYLISTS = "playlists"
    }
}