package com.github.libretube.api

import android.content.Context
import android.util.Log
import com.github.libretube.R
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.PlaylistId
import com.github.libretube.api.obj.Playlists
import com.github.libretube.constants.YOUTUBE_FRONTEND_URL
import com.github.libretube.db.DatabaseHolder
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.enums.PlaylistType
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toLocalPlaylistItem
import com.github.libretube.extensions.toStreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.obj.ImportPlaylist
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ProxyHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException

object PlaylistsHelper {
    private val pipedPlaylistRegex = "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()

    val token get() = PreferenceHelper.getToken()

    private fun loggedIn() = token != ""

    suspend fun getPlaylists(): List<Playlists> {
        if (loggedIn()) return RetrofitInstance.authApi.getUserPlaylists(token)

        val localPlaylists = awaitQuery {
            DatabaseHolder.Database.localPlaylistsDao().getAll()
        }
        val playlists = mutableListOf<Playlists>()
        localPlaylists.forEach {
            playlists.add(
                Playlists(
                    id = it.playlist.id.toString(),
                    name = it.playlist.name,
                    thumbnail = ProxyHelper.rewriteUrl(it.playlist.thumbnailUrl),
                    videos = it.videos.size.toLong()
                )
            )
        }
        return playlists
    }

    suspend fun getPlaylist(playlistId: String): Playlist {
        // load locally stored playlists with the auth api
        return when (getPlaylistType()) {
            PlaylistType.PRIVATE -> RetrofitInstance.authApi.getPlaylist(playlistId)
            PlaylistType.PUBLIC -> RetrofitInstance.api.getPlaylist(playlistId)
            PlaylistType.LOCAL -> {
                val relation = awaitQuery {
                    DatabaseHolder.Database.localPlaylistsDao().getAll()
                }.first { it.playlist.id.toString() == playlistId }
                return Playlist(
                    name = relation.playlist.name,
                    thumbnailUrl = ProxyHelper.rewriteUrl(relation.playlist.thumbnailUrl),
                    videos = relation.videos.size,
                    relatedStreams = relation.videos.map { it.toStreamItem() }
                )
            }
        }
    }

    suspend fun createPlaylist(
        playlistName: String,
        appContext: Context
    ): String? {
        if (!loggedIn()) {
            awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().createPlaylist(
                    LocalPlaylist(
                        name = playlistName,
                        thumbnailUrl = ""
                    )
                )
            }
            return awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().getAll()
            }.last().playlist.id.toString()
        }
        val response = try {
            RetrofitInstance.authApi.createPlaylist(
                token,
                Playlists(name = playlistName)
            )
        } catch (e: IOException) {
            appContext.toastFromMainThread(R.string.unknown_error)
            return null
        } catch (e: HttpException) {
            Log.e(TAG(), e.toString())
            appContext.toastFromMainThread(R.string.server_error)
            return null
        }
        if (response.playlistId != null) {
            appContext.toastFromMainThread(R.string.playlistCreated)
            return response.playlistId!!
        }
        return null
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean {
        if (!loggedIn()) {
            val localPlaylistItem = RetrofitInstance.api.getStreams(videoId).toLocalPlaylistItem(playlistId, videoId)
            awaitQuery {
                // avoid duplicated videos in a playlist
                DatabaseHolder.Database.localPlaylistsDao().deletePlaylistItemsByVideoId(playlistId, videoId)

                // add the new video to the database
                DatabaseHolder.Database.localPlaylistsDao().addPlaylistVideo(localPlaylistItem)
                val localPlaylist = DatabaseHolder.Database.localPlaylistsDao().getAll()
                    .first { it.playlist.id.toString() == playlistId }

                if (localPlaylist.playlist.thumbnailUrl == "") {
                    // set the new playlist thumbnail URL
                    localPlaylistItem.thumbnailUrl?.let {
                        localPlaylist.playlist.thumbnailUrl = it
                        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(localPlaylist.playlist)
                    }
                }
            }
            return true
        }

        return RetrofitInstance.authApi.addToPlaylist(
            token,
            PlaylistId(playlistId, videoId)
        ).message == "ok"
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        if (!loggedIn()) {
            val playlist = awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().getAll()
            }.first { it.playlist.id.toString() == playlistId }.playlist
            playlist.name = newName
            awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
            }
            return
        }

        RetrofitInstance.authApi.renamePlaylist(
            token,
            PlaylistId(
                playlistId = playlistId,
                newName = newName
            )
        )
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int) {
        if (!loggedIn()) {
            val transaction = awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().getAll()
            }.first { it.playlist.id.toString() == playlistId }
            awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().removePlaylistVideo(transaction.videos[index])
            }
            if (transaction.videos.size > 1) return
            // remove thumbnail if playlist now empty
            awaitQuery {
                transaction.playlist.thumbnailUrl = ""
                DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(transaction.playlist)
            }
            return
        }

        RetrofitInstance.authApi.removeFromPlaylist(
            PreferenceHelper.getToken(),
            PlaylistId(
                playlistId = playlistId,
                index = index
            )
        )
    }

    suspend fun importPlaylists(appContext: Context, playlists: List<ImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = createPlaylist(playlist.name!!, appContext) ?: continue
            runBlocking {
                val tasks = playlist.videos.map { videoId ->
                    async { addToPlaylist(playlistId, videoId.substringAfter("=")) }
                }
                tasks.forEach {
                    it.await()
                }
            }
        }
    }

    suspend fun exportPlaylists(): List<ImportPlaylist> {
        val playlists = getPlaylists()
        val importLists = mutableListOf<ImportPlaylist>()
        runBlocking {
            val tasks = playlists.map {
                async {
                    val list = getPlaylist(it.id!!)
                    importLists.add(
                        ImportPlaylist(
                            name = list.name,
                            type = "playlist",
                            visibility = "private",
                            videos = list.relatedStreams.orEmpty().map {
                                YOUTUBE_FRONTEND_URL + it.url!!.toID()
                            }
                        )
                    )
                }
            }
            tasks.forEach {
                it.await()
            }
        }
        return importLists
    }

    fun getPlaylistType(): PlaylistType {
        return if (loggedIn()) PlaylistType.PRIVATE else PlaylistType.LOCAL
    }

    fun getPlaylistType(playlistId: String): PlaylistType {
        if (playlistId.all { it.isDigit() }) return PlaylistType.LOCAL
        if (playlistId.matches(pipedPlaylistRegex)) return PlaylistType.PRIVATE
        return PlaylistType.PUBLIC
    }
}
