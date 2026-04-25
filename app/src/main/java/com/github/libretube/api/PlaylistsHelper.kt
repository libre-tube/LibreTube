package com.github.libretube.api

import androidx.core.text.isDigitsOnly
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.enums.PlaylistType
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.repo.LocalUserDataRepository
import com.github.libretube.repo.UserDataRepository
import com.github.libretube.repo.UserDataRepositoryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object PlaylistsHelper {
    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()
    const val MAX_CONCURRENT_IMPORT_CALLS = 5

    @Suppress("DEPRECATION")
    private val userDataRepository: UserDataRepository get() = UserDataRepositoryHelper.userDataRepository

    suspend fun getPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        val playlists = userDataRepository.getPlaylists()
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
        return when (getPlaylistType(playlistId)) {
            PlaylistType.PUBLIC -> MediaServiceRepository.instance.getPlaylist(playlistId)
            else -> userDataRepository.getPlaylist(playlistId)
        }
    }

    suspend fun getAllPlaylistsWithVideos(playlistIds: List<String>? = null): List<Playlist> {
        return withContext(Dispatchers.IO) {
            (playlistIds ?: getPlaylists().map { it.id!! })
                .map { async { getPlaylist(it) } }
                .awaitAll()
        }
    }

    suspend fun createPlaylist(playlistName: String) =
        userDataRepository.createPlaylist(playlistName)

    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem) =
        withContext(Dispatchers.IO) {
            userDataRepository.addToPlaylist(playlistId, *videos)
        }

    suspend fun renamePlaylist(playlistId: String, newName: String) =
        userDataRepository.renamePlaylist(playlistId, newName)

    suspend fun changePlaylistDescription(playlistId: String, newDescription: String) =
        userDataRepository.changePlaylistDescription(playlistId, newDescription)

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, index: Int) =
        userDataRepository.removeFromPlaylist(playlistId, videoId, index)

    suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) =
        userDataRepository.importPlaylists(playlists)

    suspend fun clonePlaylist(playlistId: String) = userDataRepository.clonePlaylist(playlistId)
    suspend fun deletePlaylist(playlistId: String) = userDataRepository.deletePlaylist(playlistId)

    // TODO: getPrivatePlaylistType should not be needed, refactor so that the app only needs to
    // distinguish between private and public playlists and accesses all private playlists the same
    // way via the PlaylistsHelper
    fun getPrivatePlaylistType(): PlaylistType {
        return if (userDataRepository is LocalUserDataRepository) PlaylistType.LOCAL else PlaylistType.PRIVATE
    }

    // TODO: this method shouldn't be needed, instead the correct playlist type should be forwarded
    // everywhere where it's needed. this way here is too error-prone, the regex might break easily
    fun getPlaylistType(playlistId: String): PlaylistType {
        return if (playlistId.isDigitsOnly()) {
            PlaylistType.LOCAL
        } else if (playlistId.matches(pipedPlaylistRegex)) {
            PlaylistType.PRIVATE
        } else {
            PlaylistType.PUBLIC
        }
    }
}
