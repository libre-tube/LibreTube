package com.github.libretube.repo

import coil3.network.HttpException
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.ltsync.obj.Channel
import com.github.libretube.api.ltsync.obj.CreatePlaylist
import com.github.libretube.api.ltsync.obj.CreateVideo
import com.github.libretube.api.ltsync.obj.DeleteUser
import com.github.libretube.api.ltsync.obj.LoginUser
import com.github.libretube.api.ltsync.obj.RegisterUser
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.extensions.toID

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
        api.subscribe(
            Channel(
                id = channelId,
                name = name,
                avatar = uploaderAvatar.orEmpty(),
                verified = verified
            )
        )
    }

    override suspend fun unsubscribe(channelId: String) {
        api.unsubscribe(channelId)
    }

    override suspend fun isSubscribed(channelId: String): Boolean? {
        try {
            // is subscribed if we can successfully load the subscription data
            api.getSubscription(channelId)
            return true
        } catch (e: HttpException) {
            if (e.response.code == 404) return false
        }

        return null
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        return api.getSubscriptions().map { channel ->
            Subscription(
                url = channel.id,
                name = channel.name,
                avatar = channel.avatar,
                verified = channel.verified
            )
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return getSubscriptions().map { it.url }
    }


    override suspend fun getPlaylist(playlistId: String): Playlist {
        return api.getPlaylist(playlistId).toPipedPlaylist()
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return api.getPlaylists().map { it.toPipedPlaylists() }
    }

    private fun StreamItem.toCreateVideo(): CreateVideo = CreateVideo(
        id = url!!.toID(),
        title = title.orEmpty(),
        thumbnailUrl = thumbnail.orEmpty(),
        duration = duration?.toInt() ?: -1,
        uploadDate = uploaded,
        uploader = Channel(
            id = uploaderUrl!!.toID(),
            name = uploaderName.orEmpty(),
            avatar = uploaderAvatar.orEmpty(),
            verified = uploaderVerified == true
        )
    )

    override suspend fun addToPlaylist(
        playlistId: String,
        vararg videos: StreamItem
    ): Boolean {
        runCatching {
            api.addToPlaylist(playlistId, videos.map { it.toCreateVideo() })
        }.isSuccess

        return true
    }

    override suspend fun renamePlaylist(
        playlistId: String,
        newName: String
    ): Boolean {
        val playlist = getPlaylist(playlistId).copy(name = newName)

        return runCatching { updatePlaylist(playlistId, playlist) }.isSuccess
    }

    override suspend fun changePlaylistDescription(
        playlistId: String,
        newDescription: String
    ): Boolean {
        val playlist = getPlaylist(playlistId).copy(description = newDescription)

        return runCatching { updatePlaylist(playlistId, playlist) }.isSuccess
    }

    private fun Playlist.toCreatePlaylist(): CreatePlaylist = CreatePlaylist(
        title = name.orEmpty(),
        description = description.orEmpty(),
        thumbnailUrl = thumbnailUrl
    )

    private suspend fun updatePlaylist(playlistId: String, playlist: Playlist) {
        api.updatePlaylist(playlistId, playlist.toCreatePlaylist())
    }

    override suspend fun removeFromPlaylist(
        playlistId: String,
        videoId: String,
        index: Int
    ): Boolean {
        return runCatching { api.removeFromPlaylist(playlistId, videoId) }.isSuccess
    }

    override suspend fun createPlaylist(playlistName: String): String {
        return api.createPlaylist(
            CreatePlaylist(
                title = playlistName,
                description = "",
                thumbnailUrl = null
            )
        ).id
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return runCatching { api.deletePlaylist(playlistId) }.isSuccess
    }
}