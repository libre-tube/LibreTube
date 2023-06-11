package com.github.libretube.api

import androidx.core.text.isDigitsOnly
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.EditPlaylistBody
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.YOUTUBE_FRONTEND_URL
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.obj.FreeTubeImportPlaylist
import com.github.libretube.obj.FreeTubeVideo
import com.github.libretube.obj.PipedImportPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object PlaylistsHelper {
    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()

    private val token get() = PreferenceHelper.getToken()

    val loggedIn: Boolean get() = token.isNotEmpty()

    suspend fun getPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        if (loggedIn) {
            RetrofitInstance.authApi.getUserPlaylists(token)
        } else {
            DatabaseHolder.Database.localPlaylistsDao().getAll().reversed()
                .map {
                    Playlists(
                        id = it.playlist.id.toString(),
                        name = it.playlist.name,
                        shortDescription = it.playlist.description,
                        thumbnail = ProxyHelper.rewriteUrl(it.playlist.thumbnailUrl),
                        videos = it.videos.size.toLong(),
                    )
                }
        }
    }

    suspend fun getPlaylist(playlistId: String): Playlist {
        // load locally stored playlists with the auth api
        return when (getPrivatePlaylistType(playlistId)) {
            PlaylistType.PRIVATE -> RetrofitInstance.authApi.getPlaylist(playlistId)
            PlaylistType.PUBLIC -> RetrofitInstance.api.getPlaylist(playlistId)
            PlaylistType.LOCAL -> {
                val relation = DatabaseHolder.Database.localPlaylistsDao().getAll()
                    .first { it.playlist.id.toString() == playlistId }
                return Playlist(
                    name = relation.playlist.name,
                    description = relation.playlist.description,
                    thumbnailUrl = ProxyHelper.rewriteUrl(relation.playlist.thumbnailUrl),
                    videos = relation.videos.size,
                    relatedStreams = relation.videos.map { it.toStreamItem() },
                )
            }
        }
    }

    suspend fun createPlaylist(playlistName: String): String? {
        return if (!loggedIn) {
            val playlist = LocalPlaylist(name = playlistName, thumbnailUrl = "")
            DatabaseHolder.Database.localPlaylistsDao().createPlaylist(playlist).toString()
        } else {
            RetrofitInstance.authApi.createPlaylist(token, Playlists(name = playlistName)).playlistId
        }
    }

    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        if (!loggedIn) {
            val localPlaylist = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }

            for (video in videos) {
                val localPlaylistItem = video.toLocalPlaylistItem(playlistId)
                // avoid duplicated videos in a playlist
                DatabaseHolder.Database.localPlaylistsDao()
                    .deletePlaylistItemsByVideoId(playlistId, localPlaylistItem.videoId)

                // add the new video to the database
                DatabaseHolder.Database.localPlaylistsDao().addPlaylistVideo(localPlaylistItem)

                val playlist = localPlaylist.playlist
                if (playlist.thumbnailUrl.isEmpty()) {
                    // set the new playlist thumbnail URL
                    localPlaylistItem.thumbnailUrl?.let {
                        playlist.thumbnailUrl = it
                        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
                    }
                }
            }
            return true
        }

        val playlist = EditPlaylistBody(playlistId, videoIds = videos.map { it.url!!.toID() })
        return RetrofitInstance.authApi.addToPlaylist(token, playlist).message == "ok"
    }

    suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        return if (!loggedIn) {
            val playlist = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }.playlist
            playlist.name = newName
            DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
            true
        } else {
            val playlist = EditPlaylistBody(playlistId, newName = newName)
            RetrofitInstance.authApi.renamePlaylist(token, playlist).message == "ok"
        }
    }

    suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        return if (!loggedIn) {
            val playlist = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }.playlist
            playlist.description = newDescription
            DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
            true
        } else {
            val playlist = EditPlaylistBody(playlistId, description = newDescription)
            RetrofitInstance.authApi.changePlaylistDescription(token, playlist).message == "ok"
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        return if (!loggedIn) {
            val transaction = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }
            DatabaseHolder.Database.localPlaylistsDao().removePlaylistVideo(
                transaction.videos[index],
            )
            // set a new playlist thumbnail if the first video got removed
            if (index == 0) {
                transaction.playlist.thumbnailUrl = transaction.videos.getOrNull(1)?.thumbnailUrl ?: ""
            }
            DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(transaction.playlist)
            true
        } else {
            RetrofitInstance.authApi.removeFromPlaylist(
                PreferenceHelper.getToken(),
                EditPlaylistBody(playlistId = playlistId, index = index),
            ).message == "ok"
        }
    }

    suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) = withContext(Dispatchers.IO) {
        playlists.map { playlist ->
            val playlistId = createPlaylist(playlist.name!!)
            async {
                playlistId ?: return@async
                // if logged in, add the playlists by their ID via an api call
                if (loggedIn) {
                    addToPlaylist(
                        playlistId,
                        *playlist.videos.map {
                            StreamItem(url = it)
                        }.toTypedArray(),
                    )
                } else {
                    // if not logged in, all video information needs to become fetched manually
                    // Only do so with 20 videos at once to prevent performance issues
                    playlist.videos.mapIndexed { index, id -> id to index }
                        .groupBy { it.second % 20 }.forEach { (_, videos) ->
                            videos.map {
                                async {
                                    runCatching {
                                        val stream = RetrofitInstance.api.getStreams(it.first).toStreamItem(
                                            it.first,
                                        )
                                        addToPlaylist(playlistId, stream)
                                    }
                                }
                            }.awaitAll()
                        }
                }
            }
        }.awaitAll()
    }

    suspend fun exportPipedPlaylists(): List<PipedImportPlaylist> = withContext(Dispatchers.IO) {
        getPlaylists()
            .map { async { getPlaylist(it.id!!) } }
            .awaitAll()
            .map {
                val videos = it.relatedStreams.map { item ->
                    "$YOUTUBE_FRONTEND_URL/watch?v=${item.url!!.toID()}"
                }
                PipedImportPlaylist(it.name, "playlist", "private", videos)
            }
    }

    suspend fun exportFreeTubePlaylists(): List<FreeTubeImportPlaylist> =
        withContext(Dispatchers.IO) {
            getPlaylists()
                .map { async { getPlaylist(it.id!!) } }
                .awaitAll()
                .map {
                    val videos = it.relatedStreams.map { item ->
                        item.url.orEmpty().replace("$YOUTUBE_FRONTEND_URL/watch?v=${item.url}", "")
                    }.map { id ->
                        FreeTubeVideo(id, it.name.orEmpty(), "", "")
                    }
                    FreeTubeImportPlaylist(it.name.orEmpty(), videos)
                }
        }

    suspend fun clonePlaylist(playlistId: String): String? {
        if (!loggedIn) {
            val playlist = RetrofitInstance.api.getPlaylist(playlistId)
            val newPlaylist = createPlaylist(playlist.name ?: "Unknown name") ?: return null

            addToPlaylist(newPlaylist, *playlist.relatedStreams.toTypedArray())

            var nextPage = playlist.nextpage
            while (nextPage != null) {
                nextPage = runCatching {
                    RetrofitInstance.api.getPlaylistNextPage(playlistId, nextPage!!).apply {
                        addToPlaylist(newPlaylist, *relatedStreams.toTypedArray())
                    }.nextpage
                }.getOrNull()
            }
            return playlistId
        }

        return RetrofitInstance.authApi.clonePlaylist(token, EditPlaylistBody(playlistId)).playlistId
    }

    suspend fun deletePlaylist(playlistId: String, playlistType: PlaylistType): Boolean {
        if (playlistType == PlaylistType.LOCAL) {
            DatabaseHolder.Database.localPlaylistsDao().deletePlaylistById(playlistId)
            DatabaseHolder.Database.localPlaylistsDao().deletePlaylistItemsByPlaylistId(playlistId)
            return true
        }

        return runCatching {
            RetrofitInstance.authApi.deletePlaylist(
                PreferenceHelper.getToken(),
                EditPlaylistBody(playlistId),
            ).message == "ok"
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
