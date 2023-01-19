package com.github.libretube.api

import android.content.Context
import android.util.Log
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
import com.github.libretube.extensions.awaitQuery
import com.github.libretube.extensions.toID
import com.github.libretube.extensions.toLocalPlaylistItem
import com.github.libretube.extensions.toStreamItem
import com.github.libretube.extensions.toastFromMainThread
import com.github.libretube.obj.ImportPlaylist
import com.github.libretube.util.PreferenceHelper
import com.github.libretube.util.ProxyHelper
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

object PlaylistsHelper {
    private val pipedPlaylistRegex =
        "[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}".toRegex()

    private val token get() = PreferenceHelper.getToken()

    val loggedIn: Boolean get() = token != ""

    suspend fun getPlaylists(): List<Playlists> {
        if (loggedIn) return RetrofitInstance.authApi.getUserPlaylists(token)

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
        return when (getPrivatePlaylistType(playlistId)) {
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
        if (!loggedIn) {
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
            RetrofitInstance.authApi.createPlaylist(token, Playlists(name = playlistName))
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
            return response.playlistId
        }
        return null
    }

    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        if (!loggedIn) {
            val localPlaylist = DatabaseHolder.Database.localPlaylistsDao().getAll()
                .first { it.playlist.id.toString() == playlistId }

            for (video in videos) {
                val localPlaylistItem = video.toLocalPlaylistItem(playlistId)
                awaitQuery {
                    // avoid duplicated videos in a playlist
                    DatabaseHolder.Database.localPlaylistsDao()
                        .deletePlaylistItemsByVideoId(playlistId, localPlaylistItem.videoId)

                    // add the new video to the database
                    DatabaseHolder.Database.localPlaylistsDao().addPlaylistVideo(localPlaylistItem)

                    if (localPlaylist.playlist.thumbnailUrl == "") {
                        // set the new playlist thumbnail URL
                        localPlaylistItem.thumbnailUrl?.let {
                            localPlaylist.playlist.thumbnailUrl = it
                            DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(
                                localPlaylist.playlist
                            )
                        }
                    }
                }
            }
            return true
        }

        val playlist = PlaylistId(playlistId, videoIds = videos.map { it.url!!.toID() })
        return RetrofitInstance.authApi.addToPlaylist(token, playlist).message == "ok"
    }

    suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        if (!loggedIn) {
            val playlist = awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().getAll()
            }.first { it.playlist.id.toString() == playlistId }.playlist
            playlist.name = newName
            awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(playlist)
            }
            return true
        }

        return RetrofitInstance.authApi.renamePlaylist(
            token,
            PlaylistId(playlistId, newName = newName)
        ).playlistId != null
    }

    suspend fun removeFromPlaylist(playlistId: String, index: Int) {
        if (!loggedIn) {
            val transaction = awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().getAll()
            }.first { it.playlist.id.toString() == playlistId }
            awaitQuery {
                DatabaseHolder.Database.localPlaylistsDao().removePlaylistVideo(
                    transaction.videos[index]
                )
            }
            if (transaction.videos.size > 1) {
                if (index == 0) {
                    transaction.videos[1].thumbnailUrl?.let {
                        transaction.playlist.thumbnailUrl = it
                    }
                    awaitQuery {
                        DatabaseHolder.Database.localPlaylistsDao().updatePlaylist(
                            transaction.playlist
                        )
                    }
                }
                return
            }
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
            // if logged in, add the playlists by their ID via an api call
            val success: Boolean = if (loggedIn) {
                addToPlaylist(
                    playlistId,
                    *playlist.videos.map {
                        StreamItem(url = it)
                    }.toTypedArray()
                )
            } else {
                // if not logged in, all video information needs to become fetched manually
                try {
                    val streamItems = playlist.videos.map {
                        RetrofitInstance.api.getStreams(it).toStreamItem(it)
                    }
                    addToPlaylist(playlistId, *streamItems.toTypedArray())
                } catch (e: Exception) {
                    false
                }
            }
            appContext.toastFromMainThread(
                if (success) R.string.importsuccess else R.string.server_error
            )
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
                            videos = list.relatedStreams.map {
                                "$YOUTUBE_FRONTEND_URL/watch?v=${it.url!!.toID()}"
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

    fun clonePlaylist(context: Context, playlistId: String) {
        val appContext = context.applicationContext
        if (!loggedIn) {
            CoroutineScope(Dispatchers.IO).launch {
                val playlist = try {
                    RetrofitInstance.api.getPlaylist(playlistId)
                } catch (e: Exception) {
                    appContext.toastFromMainThread(R.string.server_error)
                    return@launch
                }
                val newPlaylist = createPlaylist(playlist.name ?: "Unknown name", appContext)
                newPlaylist ?: return@launch

                addToPlaylist(newPlaylist, *playlist.relatedStreams.toTypedArray())

                var nextPage = playlist.nextpage
                while (nextPage != null) {
                    nextPage = try {
                        RetrofitInstance.api.getPlaylistNextPage(playlistId, nextPage).apply {
                            addToPlaylist(newPlaylist, *relatedStreams.toTypedArray())
                        }.nextpage
                    } catch (e: Exception) {
                        return@launch
                    }
                }
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val response = try {
                RetrofitInstance.authApi.clonePlaylist(
                    token,
                    PlaylistId(playlistId)
                )
            } catch (e: Exception) {
                Log.e(TAG(), e.toString())
                return@launch
            }
            appContext?.toastFromMainThread(
                if (response.playlistId != null) R.string.playlistCloned else R.string.server_error
            )
        }
    }

    fun getPrivatePlaylistType(): PlaylistType {
        return if (loggedIn) PlaylistType.PRIVATE else PlaylistType.LOCAL
    }

    private fun getPrivatePlaylistType(playlistId: String): PlaylistType {
        if (playlistId.all { it.isDigit() }) return PlaylistType.LOCAL
        if (playlistId.matches(pipedPlaylistRegex)) return PlaylistType.PRIVATE
        return PlaylistType.PUBLIC
    }
}
