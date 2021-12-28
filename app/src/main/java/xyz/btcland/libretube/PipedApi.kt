package xyz.btcland.libretube

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.btcland.libretube.obj.StreamItem
import xyz.btcland.libretube.obj.Streams
import xyz.btcland.libretube.obj.SearchResult

interface PipedApi {
    @GET("trending")
    suspend fun getTrending(@Query("region") region: String): List<StreamItem>

    @GET("streams/{videoId}")
    suspend fun getStreams(@Path("videoId") videoId: String): Streams

    @GET("search")
    suspend fun getSearchResults(
        @Query("q") searchQuery: String,
        @Query("filter") filter: String
    ): SearchResult

    @GET("suggestions")
    suspend fun getSuggestions(@Query("query") query: String): List<String>
}