package xyz.btcland.libretube

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.btcland.libretube.obj.StreamItem
import xyz.btcland.libretube.obj.Streams

interface PipedApi {
    @GET("trending")
    suspend fun getTrending(@Query("region") region: String): List<StreamItem>

    @GET("streams/{videoId}")
    suspend fun getStreams(@Path("videoId") videoId: String): Streams

    @GET("search")
    suspend fun getSearchResults(
        @Query("q") searchQuery: String,
        @Query("filter") filer: String
    ): List<StreamItem>

    @GET("suggestions")
    suspend fun getSuggestions(@Query("query") query: String): List<String>
}