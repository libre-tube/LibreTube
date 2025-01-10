package com.github.libretube.repo

import com.github.libretube.api.PlaylistsHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.EditPlaylistBody
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist

class PipedPlaylistRepository: PlaylistRepository {
    private fun Message.isOk() = this.message == "ok"
    private val token get() = PreferenceHelper.getToken()

    override suspend fun getPlaylist(playlistId: String): Playlist {
        return RetrofitInstance.authApi.getPlaylist(playlistId)
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return RetrofitInstance.authApi.getUserPlaylists(token)
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        val playlist = EditPlaylistBody(playlistId, videoIds = videos.map { it.url!!.toID() })

        return RetrofitInstance.authApi.addToPlaylist(token, playlist).isOk()
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val playlist = EditPlaylistBody(playlistId, newName = newName)

        return RetrofitInstance.authApi.renamePlaylist(token, playlist).isOk()
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        val playlist = EditPlaylistBody(playlistId, description = newDescription)

        return RetrofitInstance.authApi.changePlaylistDescription(token, playlist).isOk()
    }

    override suspend fun clonePlaylist(playlistId: String): String? {
        return RetrofitInstance.authApi.clonePlaylist(
            token,
            EditPlaylistBody(playlistId)
        ).playlistId
    }

    override suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        return RetrofitInstance.authApi.removeFromPlaylist(
            PreferenceHelper.getToken(),
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
        return RetrofitInstance.authApi.createPlaylist(
            token,
            Playlists(name = playlistName)
        ).playlistId
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return runCatching {
            RetrofitInstance.authApi.deletePlaylist(
                PreferenceHelper.getToken(),
                EditPlaylistBody(playlistId)
            ).isOk()
        }.getOrDefault(false)
    }
}