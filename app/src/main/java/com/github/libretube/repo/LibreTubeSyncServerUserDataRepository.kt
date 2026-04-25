package com.github.libretube.repo

import coil3.network.HttpException
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.ltsync.obj.Channel
import com.github.libretube.api.ltsync.obj.DeleteUser
import com.github.libretube.api.ltsync.obj.LoginUser
import com.github.libretube.api.ltsync.obj.RegisterUser
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.obj.PipedImportPlaylist

class LibreTubeSyncServerUserDataRepository : UserDataRepository {
    override var requiresLogin: Boolean = true

    private val api get() = RetrofitInstance.libretubeSyncServerApi

    override suspend fun login(username: String, password: String): String {
        try {
            return api.loginAccount(
                LoginUser(
                    username,
                    password
                )
            ).jwt
        } catch (e: HttpException) {
            throw Exception(e.response.body?.toString() ?: e.message)
        }
    }

    override suspend fun register(username: String, password: String): String {
        try {
            return api.registerAccount(
                RegisterUser(
                    username,
                    password
                )
            ).jwt
        } catch (e: HttpException) {
            throw Exception(e.response.body?.toString() ?: e.message)
        }
    }

    override suspend fun deleteAccount(password: String) {
        try {
            api.deleteAccount(
                DeleteUser(password)
            )
        } catch (e: HttpException) {
            throw Exception(e.response.body?.toString() ?: e.message)
        }
    }

    override suspend fun subscribe(
        channelId: String,
        name: String,
        uploaderAvatar: String?,
        verified: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun unsubscribe(channelId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun isSubscribed(channelId: String): Boolean? {
        TODO("Not yet implemented")
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        TODO("Not yet implemented")
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        TODO("Not yet implemented")
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylist(playlistId: String): Playlist {
        TODO("Not yet implemented")
    }

    override suspend fun getPlaylists(): List<Playlists> {
        TODO("Not yet implemented")
    }

    override suspend fun addToPlaylist(
        playlistId: String,
        vararg videos: StreamItem
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun renamePlaylist(
        playlistId: String,
        newName: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changePlaylistDescription(
        playlistId: String,
        newDescription: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun clonePlaylist(playlistId: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun removeFromPlaylist(
        playlistId: String,
        index: Int
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        TODO("Not yet implemented")
    }

    override suspend fun createPlaylist(playlistName: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        TODO("Not yet implemented")
    }
}