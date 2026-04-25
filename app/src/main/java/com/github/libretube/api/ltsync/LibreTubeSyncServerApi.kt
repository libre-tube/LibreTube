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
    fun deleteAccount(@Body deleteUser: DeleteUser)

    @POST("account/login")
    fun loginAccount(@Body loginUser: LoginUser): LoginResponse

    @POST("account/register")
    fun registerAccount(@Body registerUser: RegisterUser): LoginResponse

    @DELETE("playlists/{playlist_id}")
    fun deletePlaylist(@Path("playlist_id") playlistId: kotlin.String)

    @GET("playlists/{playlist_id}")
    fun getPlaylist(@Path("playlist_id") playlistId: kotlin.String):  PlaylistResponse

    @GET("playlists/")
    fun getPlaylists(): List<Playlist>

    @POST("playlists/{playlist_id}/videos")
    fun addToPlaylist(@Path("playlist_id") playlistId: String, @Body createVideo: CreateVideo)

    @POST("playlists/")
    fun createPlaylist(@Body createPlaylist: CreatePlaylist):  Playlist

    @DELETE("playlists/{playlist_id}/videos/{video_id}")
    fun removeFromPlaylist(@Path("playlist_id") playlistId: String, @Path("video_id") videoId: String)

    @PATCH("playlists/{playlist_id}")
    fun updatePlaylist(@Path("playlist_id") playlistId: kotlin.String, @Body createPlaylist: CreatePlaylist): Playlist

    @GET("subscriptions/")
    fun getSubscriptions(): List<Channel>

    @GET("subscriptions/{channel_id}")
    fun getSubscription(@Path("channel_id") channelId: String): Channel

    @PUT("subscriptions/")
    fun subscribe(@Body channel: Channel)

    @DELETE("subscriptions/{channel_id}")
    fun unsubscribe(@Path("channel_id") channelId: String)
}