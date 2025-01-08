package com.github.libretube.api

import androidx.core.text.isDigitsOnly
import com.github.libretube.api.obj.EditPlaylistBody
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object PlaylistsHelper {
    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()
    const val MAX_CONCURRENT_IMPORT_CALLS = 5

    private val token get() = PreferenceHelper.getToken()
    val loggedIn: Boolean get() = token.isNotEmpty()
    private fun Message.isOk() = this.message == "ok"

    suspend fun getPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        val playlists = if (loggedIn) {
            RetrofitInstance.authApi.getUserPlaylists(token)
        } else {
            LocalPlaylistsRepository.getPlaylists()
        }
        sortPlaylists(playlists)
    }

    private fun sortPlaylists(playlists: List<Playlists>): List<Playlists> {
        return when (
            PreferenceHelper.getString(PreferenceKeys.PLAYLISTS_ORDER, "creation_date")
        ) {
            "creation_date" -> playlists
            "creation_date_reversed" -> playlists.reversed()
            "alphabetic" -> playlists.sortedBy { it.name?.lowercase() }
            "alphabetic_reversed" -> playlists.sortedBy { it.name?.lowercase() }
                .reversed()

            else -> playlists
        }
    }

    suspend fun getPlaylist(playlistId: String): Playlist {
        // load locally stored playlists with the auth api
        return when (getPrivatePlaylistType(playlistId)) {
            PlaylistType.PRIVATE -> RetrofitInstance.authApi.getPlaylist(playlistId)
            PlaylistType.PUBLIC -> RetrofitInstance.api.getPlaylist(playlistId)
            PlaylistType.LOCAL -> LocalPlaylistsRepository.getPlaylist(playlistId)
        }.apply {
            relatedStreams = relatedStreams.deArrow()
        }
    }

    suspend fun createPlaylist(playlistName: String): String? {
        return if (!loggedIn) {
            LocalPlaylistsRepository.createPlaylist(playlistName)
        } else {
            RetrofitInstance.authApi.createPlaylist(
                token,
                Playlists(name = playlistName)
            ).playlistId
        }
    }

    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        if (!loggedIn) {
            LocalPlaylistsRepository.addToPlaylist(playlistId, *videos)
            return true
        }

        val playlist = EditPlaylistBody(playlistId, videoIds = videos.map { it.url!!.toID() })
        return RetrofitInstance.authApi.addToPlaylist(token, playlist).isOk()
    }

    suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        if (!loggedIn) {
            LocalPlaylistsRepository.renamePlaylist(playlistId, newName)
            return true
        }

        val playlist = EditPlaylistBody(playlistId, newName = newName)
        return RetrofitInstance.authApi.renamePlaylist(token, playlist).isOk()
    }

    suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        if (!loggedIn) {
            LocalPlaylistsRepository.changePlaylistDescription(playlistId, newDescription)
            return true
        }

        val playlist = EditPlaylistBody(playlistId, description = newDescription)
        return RetrofitInstance.authApi.changePlaylistDescription(token, playlist).isOk()
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        if (!loggedIn) {
            LocalPlaylistsRepository.removeFromPlaylist(playlistId, index)
            return true
        }

        return RetrofitInstance.authApi.removeFromPlaylist(
            PreferenceHelper.getToken(),
            EditPlaylistBody(playlistId = playlistId, index = index)
        ).isOk()
    }

    suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) =
        withContext(Dispatchers.IO) {
            if (!loggedIn) return@withContext LocalPlaylistsRepository.importPlaylists(playlists)

            for (playlist in playlists) {
                val playlistId = createPlaylist(playlist.name!!) ?: return@withContext
                val streams = playlist.videos.map { StreamItem(url = it) }
                addToPlaylist(playlistId, *streams.toTypedArray())
            }
        }

    suspend fun getAllPlaylistsWithVideos(playlistIds: List<String>? = null): List<Playlist> =
        withContext(Dispatchers.IO) {
            (playlistIds ?: getPlaylists().map { it.id!! })
                .map { async { getPlaylist(it) } }
                .awaitAll()
        }

    suspend fun clonePlaylist(playlistId: String): String? {
        if (!loggedIn) {
            return LocalPlaylistsRepository.clonePlaylist(playlistId)
        }

        return RetrofitInstance.authApi.clonePlaylist(
            token,
            EditPlaylistBody(playlistId)
        ).playlistId
    }

    suspend fun deletePlaylist(playlistId: String, playlistType: PlaylistType): Boolean {
        if (playlistType == PlaylistType.LOCAL) {
            LocalPlaylistsRepository.deletePlaylist(playlistId)
            return true
        }

        return runCatching {
            RetrofitInstance.authApi.deletePlaylist(
                PreferenceHelper.getToken(),
                EditPlaylistBody(playlistId)
            ).isOk()
        }.getOrDefault(false)
    }

    fun getPrivatePlaylistType(): PlaylistType {
        return if (loggedIn) PlaylistType.PRIVATE else PlaylistType.LOCAL
    }

    private fun getPrivatePlaylistType(playlistId: String): PlaylistType {
        return if (playlistId.isDigitsOnly()) {
            PlaylistType.LOCAL
        } else if (playlistId.matches(pipedPlaylistRegex)) {
            PlaylistType.PRIVATE
        } else {
            PlaylistType.PUBLIC
        }
    }
}
