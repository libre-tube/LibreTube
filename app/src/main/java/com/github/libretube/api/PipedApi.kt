package com.github.libretube.api

import com.github.libretube.api.obj.Channel
import com.github.libretube.api.obj.CommentsPage
import com.github.libretube.api.obj.DeleteUserRequest
import com.github.libretube.api.obj.Login
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.SearchResult
import com.github.libretube.api.obj.Segments
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subscribe
import com.github.libretube.api.obj.Subscribed
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.obj.Token
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PipedApi {
    @GET("trending")
    suspend fun getTrending(@Query("region") region: String): List<com.github.libretube.api.obj.StreamItem>

    @GET("streams/{videoId}")
    suspend fun getStreams(@Path("videoId") videoId: String): com.github.libretube.api.obj.Streams

    @GET("comments/{videoId}")
    suspend fun getComments(@Path("videoId") videoId: String): com.github.libretube.api.obj.CommentsPage

    @GET("sponsors/{videoId}")
    suspend fun getSegments(
        @Path("videoId") videoId: String,
        @Query("category") category: String
    ): com.github.libretube.api.obj.Segments

    @GET("nextpage/comments/{videoId}")
    suspend fun getCommentsNextPage(
        @Path("videoId") videoId: String,
        @Query("nextpage") nextPage: String
    ): com.github.libretube.api.obj.CommentsPage

    @GET("search")
    suspend fun getSearchResults(
        @Query("q") searchQuery: String,
        @Query("filter") filter: String
    ): com.github.libretube.api.obj.SearchResult

    @GET("nextpage/search")
    suspend fun getSearchResultsNextPage(
        @Query("q") searchQuery: String,
        @Query("filter") filter: String,
        @Query("nextpage") nextPage: String
    ): com.github.libretube.api.obj.SearchResult

    @GET("suggestions")
    suspend fun getSuggestions(@Query("query") query: String): List<String>

    @GET("channel/{channelId}")
    suspend fun getChannel(@Path("channelId") channelId: String): com.github.libretube.api.obj.Channel

    @GET("user/{name}")
    suspend fun getChannelByName(@Path("name") channelName: String): com.github.libretube.api.obj.Channel

    @GET("nextpage/channel/{channelId}")
    suspend fun getChannelNextPage(
        @Path("channelId") channelId: String,
        @Query("nextpage") nextPage: String
    ): com.github.libretube.api.obj.Channel

    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(@Path("playlistId") playlistId: String): com.github.libretube.api.obj.Playlist

    @GET("nextpage/playlists/{playlistId}")
    suspend fun getPlaylistNextPage(
        @Path("playlistId") playlistId: String,
        @Query("nextpage") nextPage: String
    ): com.github.libretube.api.obj.Playlist

    @POST("login")
    suspend fun login(@Body login: com.github.libretube.api.obj.Login): com.github.libretube.api.obj.Token

    @POST("register")
    suspend fun register(@Body login: com.github.libretube.api.obj.Login): com.github.libretube.api.obj.Token

    @POST("user/delete")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Body password: com.github.libretube.api.obj.DeleteUserRequest
    )

    @GET("feed")
    suspend fun getFeed(@Query("authToken") token: String?): List<com.github.libretube.api.obj.StreamItem>

    @GET("feed/unauthenticated")
    suspend fun getUnauthenticatedFeed(@Query("channels") channels: String): List<com.github.libretube.api.obj.StreamItem>

    @GET("subscribed")
    suspend fun isSubscribed(
        @Query("channelId") channelId: String,
        @Header("Authorization") token: String
    ): com.github.libretube.api.obj.Subscribed

    @GET("subscriptions")
    suspend fun subscriptions(@Header("Authorization") token: String): List<com.github.libretube.api.obj.Subscription>

    @GET("subscriptions/unauthenticated")
    suspend fun unauthenticatedSubscriptions(@Query("channels") channels: String): List<com.github.libretube.api.obj.Subscription>

    @POST("subscribe")
    suspend fun subscribe(
        @Header("Authorization") token: String,
        @Body subscribe: com.github.libretube.api.obj.Subscribe
    ): com.github.libretube.api.obj.Message

    @POST("unsubscribe")
    suspend fun unsubscribe(
        @Header("Authorization") token: String,
        @Body subscribe: com.github.libretube.api.obj.Subscribe
    ): com.github.libretube.api.obj.Message

    @POST("import")
    suspend fun importSubscriptions(
        @Query("override") override: Boolean,
        @Header("Authorization") token: String,
        @Body channels: List<String>
    ): com.github.libretube.api.obj.Message

    @POST("import/playlist")
    suspend fun importPlaylist(
        @Header("Authorization") token: String,
        @Body playlistId: com.github.libretube.api.obj.PlaylistId
    ): com.github.libretube.api.obj.Message

    @GET("user/playlists")
    suspend fun playlists(@Header("Authorization") token: String): List<com.github.libretube.api.obj.Playlists>

    @POST("user/playlists/rename")
    suspend fun renamePlaylist(
        @Header("Authorization") token: String,
        @Body playlistId: com.github.libretube.api.obj.PlaylistId
    )

    @POST("user/playlists/delete")
    suspend fun deletePlaylist(
        @Header("Authorization") token: String,
        @Body playlistId: com.github.libretube.api.obj.PlaylistId
    ): com.github.libretube.api.obj.Message

    @POST("user/playlists/create")
    suspend fun createPlaylist(
        @Header("Authorization") token: String,
        @Body name: com.github.libretube.api.obj.Playlists
    ): com.github.libretube.api.obj.PlaylistId

    @POST("user/playlists/add")
    suspend fun addToPlaylist(
        @Header("Authorization") token: String,
        @Body playlistId: com.github.libretube.api.obj.PlaylistId
    ): com.github.libretube.api.obj.Message

    @POST("user/playlists/remove")
    suspend fun removeFromPlaylist(
        @Header("Authorization") token: String,
        @Body playlistId: com.github.libretube.api.obj.PlaylistId
    ): com.github.libretube.api.obj.Message
}
