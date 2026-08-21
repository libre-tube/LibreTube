package com.github.libretube.api

import androidx.core.text.isDigitsOnly
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.repo.LocalPlaylistsRepository
import com.github.libretube.repo.PipedPlaylistRepository
import com.github.libretube.repo.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object PlaylistsHelper {
    internal const val MAX_CONCURRENT_IMPORT_CALLS = 5

    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()

    private val token get() = PreferenceHelper.getToken()
    val loggedIn: Boolean get() = token.isNotEmpty()

    private val playlistsRepository: PlaylistRepository
        get() = if (loggedIn) PipedPlaylistRepository() else LocalPlaylistsRepository()

    suspend fun getPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        playlistsRepository.getPlaylists().let(::sortPlaylists)
    }

    private fun sortPlaylists(playlists: List<Playlists>): List<Playlists> {
        val order = PreferenceHelper.getString(PreferenceKeys.PLAYLISTS_ORDER, "creation_date")
        return when (order) {
            "creation_date" -> playlists
            "creation_date_reversed" -> playlists.reversed()
            "alphabetic" -> playlists.sortedBy { it.name?.lowercase() }
            "alphabetic_reversed" -> playlists.sortedBy { it.name?.lowercase() }.reversed()
            else -> playlists
        }
    }

    suspend fun getPlaylist(playlistId: String): Playlist = when (getPlaylistType(playlistId)) {
        PlaylistType.PUBLIC -> MediaServiceRepository.instance.getPlaylist(playlistId)
        else -> playlistsRepository.getPlaylist(playlistId)
    }

    suspend fun getAllPlaylistsWithVideos(playlistIds: List<String>? = null): List<Playlist> =
        withContext(Dispatchers.IO) {
            (playlistIds ?: getPlaylists().map { it.id!! })
                .map { async { getPlaylist(it) } }
                .awaitAll()
        }

    suspend fun createPlaylist(playlistName: String) =
        playlistsRepository.createPlaylist(playlistName)

    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem) =
        withContext(Dispatchers.IO) {
            playlistsRepository.addToPlaylist(playlistId, *videos)
        }

    suspend fun renamePlaylist(playlistId: String, newName: String) =
        playlistsRepository.renamePlaylist(playlistId, newName)

    suspend fun changePlaylistDescription(playlistId: String, newDescription: String) =
        playlistsRepository.changePlaylistDescription(playlistId, newDescription)

    suspend fun removeFromPlaylist(playlistId: String, index: Int) =
        playlistsRepository.removeFromPlaylist(playlistId, index)

    suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) =
        playlistsRepository.importPlaylists(playlists)

    suspend fun clonePlaylist(playlistId: String) = playlistsRepository.clonePlaylist(playlistId)
    suspend fun deletePlaylist(playlistId: String) = playlistsRepository.deletePlaylist(playlistId)

    fun getPrivatePlaylistType(): PlaylistType =
        if (loggedIn) PlaylistType.PRIVATE else PlaylistType.LOCAL

    fun getPlaylistType(playlistId: String): PlaylistType = when {
        playlistId.isDigitsOnly() -> PlaylistType.LOCAL
        playlistId.matches(pipedPlaylistRegex) -> PlaylistType.PRIVATE
        else -> PlaylistType.PUBLIC
    }
}
