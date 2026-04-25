package com.github.libretube.api.ltsync

import com.github.libretube.api.ltsync.obj.Channel
import com.github.libretube.api.ltsync.obj.CreatePlaylist
import com.github.libretube.api.ltsync.obj.CreateVideo
import com.github.libretube.api.ltsync.obj.DeleteUser
import com.github.libretube.api.ltsync.obj.LoginResponse
import com.github.libretube.api.ltsync.obj.LoginUser
import com.github.libretube.api.ltsync.obj.Playlist
import com.github.libretube.api.ltsync.obj.PlaylistResponse
import com.github.libretube.api.ltsync.obj.RegisterUser
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Generated based on the server's OpenAPI spec, using https://openapi-generator.tech/docs/generators/kotlin/

interface LibreTubeSyncServerApi {
    // workaround for https://stackoverflow.com/questions/37942474/delete-method-is-not-supportingnon-body-http-method-cannot-contain-body-or-t
    @HTTP(method = "DELETE",  path ="account/delete", hasBody = true)
    suspend fun deleteAccount(@Body deleteUser: DeleteUser)

    @POST("account/login")
    suspend fun loginAccount(@Body loginUser: LoginUser): LoginResponse

    @POST("account/register")
    suspend fun registerAccount(@Body registerUser: RegisterUser): LoginResponse

    @DELETE("playlists/{playlist_id}")
    suspend fun deletePlaylist(@Path("playlist_id") playlistId: String)

    @GET("playlists/{playlist_id}")
    suspend fun getPlaylist(@Path("playlist_id") playlistId: String):  PlaylistResponse

    @GET("playlists/")
    suspend fun getPlaylists(): List<Playlist>

    @POST("playlists/{playlist_id}/videos")
    suspend fun addToPlaylist(@Path("playlist_id") playlistId: String, @Body createVideo: List<CreateVideo>)

    @POST("playlists/")
    suspend fun createPlaylist(@Body createPlaylist: CreatePlaylist):  Playlist

    @DELETE("playlists/{playlist_id}/videos/{video_id}")
    suspend fun removeFromPlaylist(@Path("playlist_id") playlistId: String, @Path("video_id") videoId: String)

    @PATCH("playlists/{playlist_id}")
    suspend fun updatePlaylist(@Path("playlist_id") playlistId: String, @Body createPlaylist: CreatePlaylist): Playlist

    @GET("subscriptions/")
    suspend fun getSubscriptions(): List<Channel>

    @GET("subscriptions/{channel_id}")
    suspend fun getSubscription(@Path("channel_id") channelId: String): Channel

    @PUT("subscriptions/")
    suspend fun subscribe(@Body channel: Channel)

    @DELETE("subscriptions/{channel_id}")
    suspend fun unsubscribe(@Path("channel_id") channelId: String)
}