package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.EditPlaylistBody
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist

private fun Message.isOk() = this.message == "ok"

class PipedPlaylistRepository : PlaylistRepository {

    private val token get() = PreferenceHelper.getToken()

    override suspend fun getPlaylist(playlistId: String): Playlist {
        return RetrofitInstance.authApi.getPlaylist(playlistId)
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return RetrofitInstance.authApi.getUserPlaylists(token)
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        val body = EditPlaylistBody(playlistId, videoIds = videos.map { it.url!!.toID() })
        return RetrofitInstance.authApi.addToPlaylist(token, body).isOk()
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val body = EditPlaylistBody(playlistId, newName = newName)
        return RetrofitInstance.authApi.renamePlaylist(token, body).isOk()
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        val body = EditPlaylistBody(playlistId, description = newDescription)
        return RetrofitInstance.authApi.changePlaylistDescription(token, body).isOk()
    }

    override suspend fun clonePlaylist(playlistId: String): String? {
        return RetrofitInstance.authApi.clonePlaylist(token, EditPlaylistBody(playlistId)).playlistId
    }

    override suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        val body = EditPlaylistBody(playlistId = playlistId, index = index)
        return RetrofitInstance.authApi.removeFromPlaylist(token, body).isOk()
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = createPlaylist(playlist.name!!) ?: return
            val streams = playlist.videos.map { StreamItem(url = it) }
            addToPlaylist(playlistId, *streams.toTypedArray())
        }
    }

    override suspend fun createPlaylist(playlistName: String): String? {
        return RetrofitInstance.authApi.createPlaylist(token, Playlists(name = playlistName)).playlistId
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return runCatching {
            RetrofitInstance.authApi.deletePlaylist(token, EditPlaylistBody(playlistId)).isOk()
        }.getOrDefault(false)
    }
}
