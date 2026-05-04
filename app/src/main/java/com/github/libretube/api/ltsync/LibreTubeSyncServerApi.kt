package com.github.libretube.api.ltsync

import com.github.libretube.api.ltsync.obj.Channel
import com.github.libretube.api.ltsync.obj.CreatePlaylist
import com.github.libretube.api.ltsync.obj.CreateVideo
import com.github.libretube.api.ltsync.obj.DeleteUser
import com.github.libretube.api.ltsync.obj.ExtendedPublicPlaylist
import com.github.libretube.api.ltsync.obj.ExtendedSubscriptionGroup
import com.github.libretube.api.ltsync.obj.ExtendedWatchHistoryItem
import com.github.libretube.api.ltsync.obj.LoginResponse
import com.github.libretube.api.ltsync.obj.LoginUser
import com.github.libretube.api.ltsync.obj.Playlist
import com.github.libretube.api.ltsync.obj.PlaylistResponse
import com.github.libretube.api.ltsync.obj.RegisterUser
import com.github.libretube.api.ltsync.obj.SubscriptionGroup
import com.github.libretube.api.ltsync.obj.WatchHistoryItem
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Generated based on the server's OpenAPI spec, using https://openapi-generator.tech/docs/generators/kotlin/
// generate -i api-spec.yaml -g kotlin -o outdir --library jvm-retrofit2 --additional-properties=serializationLibrary=kotlinx_serialization

interface LibreTubeSyncServerApi {
    // workaround for https://stackoverflow.com/questions/37942474/delete-method-is-not-supportingnon-body-http-method-cannot-contain-body-or-t
    @HTTP(method = "DELETE",  path ="v1/account/delete", hasBody = true)
    suspend fun deleteAccount(@Body deleteUser: DeleteUser)

    @POST("v1/account/login")
    suspend fun loginAccount(@Body loginUser: LoginUser): LoginResponse

    @POST("v1/account/register")
    suspend fun registerAccount(@Body registerUser: RegisterUser): LoginResponse



    @DELETE("v1/playlists/{playlist_id}")
    suspend fun deletePlaylist(@Path("playlist_id") playlistId: String)

    @GET("v1/playlists/{playlist_id}")
    suspend fun getPlaylist(@Path("playlist_id") playlistId: String):  PlaylistResponse

    @GET("v1/playlists/")
    suspend fun getPlaylists(): List<Playlist>

    @POST("v1/playlists/{playlist_id}/videos")
    suspend fun addToPlaylist(@Path("playlist_id") playlistId: String, @Body createVideo: List<CreateVideo>)

    @POST("v1/playlists/")
    suspend fun createPlaylist(@Body createPlaylist: CreatePlaylist):  Playlist

    @DELETE("v1/playlists/{playlist_id}/videos/{video_id}")
    suspend fun removeFromPlaylist(@Path("playlist_id") playlistId: String, @Path("video_id") videoId: String)

    @PATCH("v1/playlists/{playlist_id}")
    suspend fun updatePlaylist(@Path("playlist_id") playlistId: String, @Body createPlaylist: CreatePlaylist): Playlist



    @GET("v1/subscriptions/")
    suspend fun getSubscriptions(): List<Channel>

    @GET("v1/subscriptions/{channel_id}")
    suspend fun getSubscription(@Path("channel_id") channelId: String): Channel

    @PUT("v1/subscriptions/")
    suspend fun subscribe(@Body channel: Channel)

    @DELETE("v1/subscriptions/{channel_id}")
    suspend fun unsubscribe(@Path("channel_id") channelId: String)



    @POST("v1/subscriptions/groups/")
    suspend fun createSubscriptionGroup(@Body subscriptionGroup: SubscriptionGroup): SubscriptionGroup

    @DELETE("v1/subscriptions/groups/{subscription_group_id}")
    suspend fun deleteSubscriptionGroup(@Path("subscription_group_id") subscriptionGroupId: String)

    @GET("v1/subscriptions/groups/{subscription_group_id}")
    suspend fun getSubscriptionGroup(@Path("subscription_group_id") subscriptionGroupId: String): ExtendedSubscriptionGroup

    @GET("v1/subscriptions/groups/")
    suspend fun getSubscriptionGroups(): List<ExtendedSubscriptionGroup>

    @PATCH("v1/subscriptions/groups/{subscription_group_id}")
    suspend fun updateSubscriptionGroup(@Path("subscription_group_id") subscriptionGroupId: String, @Body subscriptionGroup: SubscriptionGroup): SubscriptionGroup

    @DELETE("v1/subscriptions/groups/{subscription_group_id}/channels/{channel_id}")
    suspend fun removeFromSubscriptionGroup(@Path("subscription_group_id") subscriptionGroupId: String, @Path("channel_id") channelId: String)

    @PUT("v1/subscriptions/groups/{subscription_group_id}/channels/{channel_id}")
    suspend fun addToSubscriptionGroup(@Path("subscription_group_id") subscriptionGroupId: String, @Path("channel_id") channelId: String)



    @PUT("v1/watch_history/")
    suspend fun addToWatchHistory(@Body extendedWatchHistoryItem: ExtendedWatchHistoryItem): ExtendedWatchHistoryItem

    @PATCH("v1/watch_history/{video_id}")
    suspend fun updateWatchHistoryEntry(@Path("video_id") videoId: String, @Body watchHistoryItem: WatchHistoryItem)

    @GET("v1/watch_history/{video_id}")
    suspend fun getFromWatchHistory(@Path("video_id") videoId: String): ExtendedWatchHistoryItem

    @GET("v1/watch_history/")
    suspend fun getWatchHistory(
        @Query("page") page: Int,
    ): List<ExtendedWatchHistoryItem>

    @DELETE("v1/watch_history/{video_id}")
    suspend fun removeFromWatchHistory(@Path("video_id") videoId: String)

    @DELETE("v1/watch_history/")
    suspend fun clearWatchHistory()



    @GET("v1/playlist_bookmarks/")
    suspend fun getPlaylistBookmarks(): List<ExtendedPublicPlaylist>

    @GET("v1/playlist_bookmarks/{public_playlist_id}")
    suspend fun getPlaylistBookmark(@Path("public_playlist_id") publicPlaylistId: String): ExtendedPublicPlaylist

    @POST("v1/playlist_bookmarks/")
    suspend fun createPlaylistBookmark(@Body extendedPublicPlaylist: ExtendedPublicPlaylist): ExtendedPublicPlaylist

    @DELETE("v1/playlist_bookmarks/{public_playlist_id}")
    suspend fun deletePlaylistBookmark(@Path("public_playlist_id") publicPlaylistId: String)
}