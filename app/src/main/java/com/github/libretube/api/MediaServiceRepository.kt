package com.github.libretube.api

import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.ChannelTabResponse
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.DeArrowContent
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.SegmentData
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.extensions.sha256Sum
import com.github.libretube.helpers.PlayerHelper

interface MediaServiceRepository {
    fun getTrendingCategories(): List<TrendingCategory>

    suspend fun getTrending(region: String, category: TrendingCategory): List<StreamItem>
    suspend fun getStreams(videoId: String): Streams
    suspend fun getComments(videoId: String): CommentsPage
    suspend fun getSegments(
        videoId: String,
        category: List<String>,
        actionType: List<String>? = null
    ): SegmentData

    suspend fun getVideoLabels(videoId: String
    ): SegmentData? = RetrofitInstance.externalApi.getVideoLabels(
        // use hashed video id for privacy
        // https://wiki.sponsor.ajay.app/w/API_Docs/Draft#GET_/api/videoLabels/:sha256HashPrefix
        videoId.sha256Sum().substring(0, 4),
    ).firstOrNull { it.videoID == videoId }

    suspend fun getDeArrowContent(videoIds: String): Map<String, DeArrowContent>
    suspend fun getCommentsNextPage(videoId: String, nextPage: String): CommentsPage
    suspend fun getSearchResults(searchQuery: String, filter: String): SearchResult
    suspend fun getSearchResultsNextPage(
        searchQuery: String,
        filter: String,
        nextPage: String
    ): SearchResult

    suspend fun getSuggestions(query: String): List<String>
    suspend fun getChannel(channelId: String): Channel
    suspend fun getChannelTab(data: String, nextPage: String? = null): ChannelTabResponse
    suspend fun getChannelByName(channelName: String): Channel
    suspend fun getChannelNextPage(channelId: String, nextPage: String): Channel
    suspend fun getPlaylist(playlistId: String): Playlist
    suspend fun getPlaylistNextPage(playlistId: String, nextPage: String): Playlist

    companion object {
        val instance: MediaServiceRepository
            get() = when {
                PlayerHelper.fullLocalMode -> NewPipeMediaServiceRepository()
                PlayerHelper.localStreamExtraction -> LocalStreamsExtractionPipedMediaServiceRepository()
                else -> PipedMediaServiceRepository()
            }
    }
}

enum class TrendingCategory {
    TRENDING,
    GAMING,
    PODCASTS,
    TRAILERS,
    MUSIC,
    LIVE
}