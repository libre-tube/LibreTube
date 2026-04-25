package com.github.libretube.repo

import com.github.libretube.LibreTubeApp
import com.github.libretube.R
import com.github.libretube.api.JsonHelper
import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.DeleteUserRequest
import com.github.libretube.api.obj.EditPlaylistBody
import com.github.libretube.api.obj.Login
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscribe
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.obj.Token
import com.github.libretube.extensions.toID
import com.github.libretube.obj.PipedImportPlaylist
import retrofit2.HttpException

class PipedUserDataRepository : UserDataRepository {
    override var requiresLogin: Boolean = true

    private fun Message.isOk() = this.message == "ok"

    override suspend fun register(username: String, password: String): String {
        return RetrofitInstance.pipedAuthApi.register(Login(username, password)).token!!
    }

    override suspend fun login(username: String, password: String): String {
        val token = try {
            RetrofitInstance.pipedAuthApi.login(Login(username, password))
        } catch (e: HttpException) {
            // properly forward the error message
            val errorMessage = e.response()?.errorBody()?.string()?.runCatching {
                JsonHelper.json.decodeFromString<Token>(this).error
            }?.getOrNull() ?: LibreTubeApp.instance.getString(R.string.server_error)
            throw Exception(errorMessage)
        }

        if (token.error != null) throw Exception(token.error)
        return token.token!!
    }

    override suspend fun deleteAccount(password: String) {
        RetrofitInstance.pipedAuthApi.deleteAccount(DeleteUserRequest(password))
    }

    override suspend fun getPlaylist(playlistId: String): Playlist {
        return RetrofitInstance.pipedAuthApi.getPlaylist(playlistId)
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return RetrofitInstance.pipedAuthApi.getUserPlaylists()
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        val playlist = EditPlaylistBody(playlistId, videoIds = videos.map { it.url!!.toID() })

        return RetrofitInstance.pipedAuthApi.addToPlaylist(playlist).isOk()
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val playlist = EditPlaylistBody(playlistId, newName = newName)

        return RetrofitInstance.pipedAuthApi.renamePlaylist( playlist).isOk()
    }

    override suspend fun changePlaylistDescription(
        playlistId: String,
        newDescription: String
    ): Boolean {
        val playlist = EditPlaylistBody(playlistId, description = newDescription)

        return RetrofitInstance.pipedAuthApi.changePlaylistDescription(playlist).isOk()
    }

    override suspend fun clonePlaylist(playlistId: String): String? {
        return RetrofitInstance.pipedAuthApi.clonePlaylist(
            EditPlaylistBody(playlistId)
        ).playlistId
    }

    override suspend fun removeFromPlaylist(playlistId: String, videoId: String, index: Int): Boolean {
        return RetrofitInstance.pipedAuthApi.removeFromPlaylist(
            EditPlaylistBody(playlistId = playlistId, index = index)
        ).isOk()
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = PlaylistsHelper.createPlaylist(playlist.name!!) ?: return
            val streams = playlist.videos.map { StreamItem(url = it) }
            PlaylistsHelper.addToPlaylist(playlistId, *streams.toTypedArray())
        }
    }

    override suspend fun createPlaylist(playlistName: String): String? {
        return RetrofitInstance.pipedAuthApi.createPlaylist(
            Playlists(name = playlistName)
        ).playlistId
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return runCatching {
            RetrofitInstance.pipedAuthApi.deletePlaylist(
                EditPlaylistBody(playlistId)
            ).isOk()
        }.getOrDefault(false)
    }

    override suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) {
        runCatching {
            RetrofitInstance.pipedAuthApi.subscribe(Subscribe(channelId))
        }
    }

    override suspend fun unsubscribe(channelId: String) {
        runCatching {
            RetrofitInstance.pipedAuthApi.unsubscribe(Subscribe(channelId))
        }
    }

    override suspend fun isSubscribed(channelId: String): Boolean? {
        return runCatching {
            RetrofitInstance.pipedAuthApi.isSubscribed(channelId)
        }.getOrNull()?.subscribed
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        RetrofitInstance.pipedAuthApi.importSubscriptions(false, newChannels)
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        return RetrofitInstance.pipedAuthApi.subscriptions()
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return getSubscriptions().map { it.url.toID() }
    }
}