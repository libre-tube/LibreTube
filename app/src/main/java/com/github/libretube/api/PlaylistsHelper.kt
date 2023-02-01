package com.github.libretube.api

import android.content.Context
import android.util.Log
import androidx.core.text.isDigitsOnly
import com.github.libretube.R
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.constants.YOUTUBE_FRONTEND_URL
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toLocalPlaylistItem
import com.github.libretube.extensions.toStreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.helpers.ProxyHelper
import com.github.libretube.obj.ImportPlaylist
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import retrofit2.HttpException

object PlaylistsHelper {
    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()

    private val token get() = PreferenceHelper.getToken()

    val loggedIn: Boolean get() = token.isNotEmpty()

    suspend fun getPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        if (loggedIn) {
            RetrofitInstance.authApi.getUserPlaylists(token)
        } else {
            DatabaseHolder.Database.localPlaylistsDao().getAll()
                .map {
                    Playlists(
                        id = it.playlist.id.toString(),
                        name = it.playlist.name,
                        thumbnail = ProxyHelper.rewriteUrl(it.playlist.thumbnailUrl),
                        videos = it.videos.size.toLong()
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
                    thumbnailUrl = ProxyHelper.rewriteUrl(relation.playlist.thumbnailUrl),
                    videos = relation.videos.size,
                    relatedStreams = relation.videos.map { it.toStreamItem() }
                )
            }
        }
    }

    suspend fun createPlaylist(playlistName: String, appContext: Context?): String? {
        if (!loggedIn) {
            val playlist = LocalPlaylist(name = playlistName, thumbnailUrl = "")
            DatabaseHolder.Database.localPlaylistsDao().createPlaylist(playlist)
            return DatabaseHolder.Database.localPlaylistsDao().getAll()
                .last().playlist.id.toString()
        } else {
            return try {
                RetrofitInstance.authApi.createPlaylist(token, Playlists(name = playlistName))
            } catch (e: IOException) {
                appContext?.toastFromMainThread(R.string.unknown_error)
                return null
            } catch (e: HttpException) {
                Log.e(TAG(), e.toString())
                appContext?.toastFromMainThread(R.string.server_error)
                return null
            }.playlistId.also {
                appContext?.toastFromMainThread(R.string.playlistCreated)
            }
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
                if (playlist.thumbnailUrl == "") {
                    // set the new playlist thumbnail URL
                    localPlaylistItem.thumbnailUrl?.let {
                        playlist.thumbnailUrl = it
                        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
                    }
                }
            }
            return true
        }

        val playlist = PlaylistId(playlistId, videoIds = videos.map { it.url!!.toID() })
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
            val playlist = PlaylistId(playlistId, newName = newName)
            RetrofitInstance.authApi.renamePlaylist(token, playlist).playlistId != null
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        return if (!loggedIn) {
            val transaction = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }
            DatabaseHolder.Database.localPlaylistsDao().removePlaylistVideo(
                transaction.videos[index]
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
                PlaylistId(playlistId = playlistId, index = index)
            ).message == "ok"
        }
    }

    suspend fun importPlaylists(playlists: List<ImportPlaylist>) = withContext(Dispatchers.IO) {
        playlists.map { playlist ->
            val playlistId = createPlaylist(playlist.name!!, null)
            async {
                playlistId ?: return@async
                // if logged in, add the playlists by their ID via an api call
                if (loggedIn) {
                    addToPlaylist(
                        playlistId,
                        *playlist.videos.map {
                            StreamItem(url = it)
                        }.toTypedArray()
                    )
                } else {
                    // if not logged in, all video information needs to become fetched manually
                    runCatching {
                        val streamItems = playlist.videos.map {
                            async {
                                try {
                                    RetrofitInstance.api.getStreams(it).toStreamItem(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                            .awaitAll()
                            .filterNotNull()

                        addToPlaylist(playlistId, *streamItems.toTypedArray())
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun exportPlaylists(): List<ImportPlaylist> = withContext(Dispatchers.IO) {
        getPlaylists()
            .map { async { getPlaylist(it.id!!) } }
            .awaitAll()
            .map {
                val videos = it.relatedStreams.map { item ->
                    "$YOUTUBE_FRONTEND_URL/watch?v=${item.url!!.toID()}"
                }
                ImportPlaylist(it.name, "playlist", "private", videos)
            }
    }

    suspend fun clonePlaylist(context: Context, playlistId: String): String? {
        val appContext = context.applicationContext
        if (!loggedIn) {
            val playlist = try {
                RetrofitInstance.api.getPlaylist(playlistId)
            } catch (e: Exception) {
                appContext.toastFromMainThread(R.string.server_error)
                return null
            }
            val newPlaylist = createPlaylist(playlist.name ?: "Unknown name", appContext) ?: return null

            addToPlaylist(newPlaylist, *playlist.relatedStreams.toTypedArray())

            var nextPage = playlist.nextpage
            while (nextPage != null) {
                nextPage = try {
                    RetrofitInstance.api.getPlaylistNextPage(playlistId, nextPage).apply {
                        addToPlaylist(newPlaylist, *relatedStreams.toTypedArray())
                    }.nextpage
                } catch (e: Exception) {
                    break
                }
            }
            return playlistId
        }

        return runCatching {
            RetrofitInstance.authApi.clonePlaylist(token, PlaylistId(playlistId))
        }.getOrNull()?.playlistId
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
