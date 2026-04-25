package com.github.libretube.repo

import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.obj.PipedImportPlaylist

class LibreTubeSyncServerUserDataRepository: UserDataRepository {
    override var requiresLogin: Boolean = true

    private val token get() = PreferenceHelper.getToken()

    override suspend fun login(username: String, password: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun register(username: String, password: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(password: String) {
        TODO("Not yet implemented")
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