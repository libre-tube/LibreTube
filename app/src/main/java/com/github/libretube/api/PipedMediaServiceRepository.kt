package com.github.libretube.api

import android.util.Log
import com.github.libretube.api.RetrofitInstance.PIPED_API_URL
import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

open class PipedMediaServiceRepository : MediaServiceRepository {

    companion object {
        private const val TAG = "PipedMediaService"

        val apiUrl get() = PreferenceHelper.getString(PreferenceKeys.FETCH_INSTANCE, PIPED_API_URL)

        private val api by resettableLazy(RetrofitInstance.apiLazyMgr) {
            RetrofitInstance.buildRetrofitInstance<PipedApi>(apiUrl)
        }
    }

    override fun getTrendingCategories(): List<TrendingCategory> = emptyList()

    override suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem> =
        apiCallWithFallback { api.getTrending(region) }

    override suspend fun getStreams(videoId: String): Streams {
        return apiCallWithFallback {
            api.getStreams(videoId).also {
                it.isShort = it.videoStreams.firstOrNull()?.let { stream ->
                    (stream.height ?: 0) > (stream.width ?: 0)
                } ?: false
            }
        }
    }

    override suspend fun getComments(videoId: String): CommentsPage =
        apiCallWithFallback { api.getComments(videoId) }

    override suspend fun getSegments(
        videoId: String,
        category: List<String>,
        actionType: List<String>?
    ): SegmentData = apiCallWithFallback {
        api.getSegments(
            videoId,
            JsonHelper.json.encodeToString(category),
            JsonHelper.json.encodeToString(actionType)
        )
    }

    override suspend fun getDeArrowContent(videoId: String): DeArrowContent? =
        apiCallWithFallback { api.getDeArrowContent(videoId)[videoId] }

    override suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage =
        apiCallWithFallback { api.getCommentsNextPage(videoId, nextPage) }

    override suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult =
        apiCallWithFallback { api.getSearchResults(searchQuery, filter) }

    override suspend fun getSearchResultsNextPage(
        searchQuery: String,
        filter: String,
        nextPage: String
    ): SearchResult = apiCallWithFallback { api.getSearchResultsNextPage(searchQuery, filter, nextPage) }

    override suspend fun getSuggestions(query: String): List<String> =
        apiCallWithFallback { api.getSuggestions(query) }

    override suspend fun getChannel(channelId: String): Channel =
        apiCallWithFallback { api.getChannel(channelId) }

    override suspend fun getChannelTab(data: String, nextPage: String?): ChannelTabResponse =
        apiCallWithFallback { api.getChannelTab(data, nextPage) }

    override suspend fun getChannelByName(channelName: String): Channel =
        apiCallWithFallback { api.getChannelByName(channelName) }

    override suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel =
        apiCallWithFallback { api.getChannelNextPage(channelId, nextPage) }

    override suspend fun getPlaylist(playlistId: String): Playlist =
        apiCallWithFallback { api.getPlaylist(playlistId) }

    override suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist =
        apiCallWithFallback { api.getPlaylistNextPage(playlistId, nextPage) }

    /**
     * Execute an API call with automatic instance fallback on failure.
     *
     * On failure:
     * 1. Attempts to find a working fallback instance
     * 2. Retries the request once on the fallback instance
     * 3. If still fails, throws the original exception
     */
    private suspend fun <T> apiCallWithFallback(call: suspend () -> T): T {
        return try {
            call()
        } catch (e: IOException) {
            Log.w(TAG, "Network error on current instance, attempting fallback: ${e.message}")
            handleInstanceFailure<T>(e)
        } catch (e: HttpException) {
            if (e.code() in 500..599 || e.code() == 429) {
                Log.w(TAG, "Server error (${e.code()}) on current instance, attempting fallback")
                handleInstanceFailure<T>(e)
            } else {
                throw parseHttpError(e)
            }
        }
    }

    private suspend fun <T> handleInstanceFailure(originalException: Exception): T {
        val failedInstance = apiUrl
        val fallbackInstance = InstanceFallbackManager.onInstanceFailed(failedInstance)

        if (fallbackInstance != null) {
            Log.i(TAG, "Retrying on fallback instance: $fallbackInstance")
            // Small delay before retrying
            delay(500)
            // InstanceFallbackManager already updated the preference and reset the lazy manager
            // Throw InstanceSwitchedException so the caller can retry with the new instance
            throw InstanceSwitchedException(fallbackInstance, originalException)
        }

        throw originalException
    }

    private fun parseHttpError(e: HttpException): Exception {
        val errorMessage = e.response()?.errorBody()?.string()?.runCatching {
            JsonHelper.json.decodeFromString<Message>(this).message
        }?.getOrNull()

        return when (e.code()) {
            429 -> Exception("Too many requests. Please try again later.")
            in 500..599 -> Exception(errorMessage ?: "Server error. Try another instance.")
            else -> Exception(errorMessage ?: "Request failed (${e.code()})")
        }
    }
}

/**
 * Exception thrown when an instance switch is needed.
 * Carries the new instance URL so the caller can retry.
 */
class InstanceSwitchedException(
    val newInstanceUrl: String,
    cause: Throwable
) : Exception("Instance switched to $newInstanceUrl", cause)