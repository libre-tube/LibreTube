package com.github.libretube.repo

import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.ltsync.obj.Channel
import com.github.libretube.api.ltsync.obj.CreatePlaylist
import com.github.libretube.api.ltsync.obj.CreateVideo
import com.github.libretube.api.ltsync.obj.DeleteUser
import com.github.libretube.api.ltsync.obj.ExtendedPlaylist
import com.github.libretube.api.ltsync.obj.ExtendedPublicPlaylist
import com.github.libretube.api.ltsync.obj.ExtendedSubscriptionGroup
import com.github.libretube.api.ltsync.obj.LoginUser
import com.github.libretube.api.ltsync.obj.RegisterUser
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.api.obj.WatchHistoryEntryMetadata
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.extensions.toID
import retrofit2.HttpException

class LibreTubeSyncServerUserDataRepository : UserDataRepository {
    override var requiresLogin: Boolean = true

    private val api get() = RetrofitInstance.libretubeSyncServerApi

    private suspend fun <T> tryHttpOrRaiseError(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw Exception(e.response()?.errorBody()?.string() ?: e.message)
        }
    }

    override suspend fun login(username: String, password: String): String {
        return tryHttpOrRaiseError {
            api.loginAccount(
                LoginUser(
                    username,
                    password
                )
            ).jwt
        }
    }

    override suspend fun register(username: String, password: String): String {
        return tryHttpOrRaiseError {
            api.registerAccount(
                RegisterUser(
                    username,
                    password
                )
            ).jwt
        }
    }

    override suspend fun deleteAccount(password: String) {
        tryHttpOrRaiseError {
            api.deleteAccount(
                DeleteUser(password)
            )
        }
    }

    override suspend fun subscribe(
        channelId: String,
        name: String,
        uploaderAvatar: String?,
        verified: Boolean
    ) {
        tryHttpOrRaiseError {
            api.subscribe(
                Channel(
                    id = channelId,
                    name = name,
                    avatar = uploaderAvatar.orEmpty(),
                    verified = verified
                )
            )
        }
    }

    override suspend fun unsubscribe(channelId: String) {
        tryHttpOrRaiseError { api.unsubscribe(channelId) }
    }

    override suspend fun isSubscribed(channelId: String): Boolean? {
        try {
            // is subscribed if we can successfully load the subscription data
            api.getSubscription(channelId)
            return true
        } catch (e: HttpException) {
            if (e.response()?.code() == 404) return false
        }

        return null
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        return tryHttpOrRaiseError { api.getSubscriptions() }.map { channel ->
            Subscription(
                url = channel.id,
                name = channel.name,
                avatar = channel.avatar,
                verified = channel.verified
            )
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return tryHttpOrRaiseError { api.getSubscriptions() }.map { it.id }
    }


    override suspend fun getPlaylist(playlistId: String): Playlist {
        return tryHttpOrRaiseError { api.getPlaylist(playlistId).toPipedPlaylist() }
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return tryHttpOrRaiseError { api.getPlaylists().map { it.toPipedPlaylists() } }
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
        tryHttpOrRaiseError {
            api.addToPlaylist(playlistId, videos.map { it.toCreateVideo() })
        }

        return true
    }

    override suspend fun renamePlaylist(
        playlistId: String,
        newName: String
    ): Boolean {
        val playlist = tryHttpOrRaiseError { getPlaylist(playlistId).copy(name = newName) }

        return runCatching { updatePlaylist(playlistId, playlist) }.isSuccess
    }

    override suspend fun changePlaylistDescription(
        playlistId: String,
        newDescription: String
    ): Boolean {
        val playlist =
            tryHttpOrRaiseError { getPlaylist(playlistId).copy(description = newDescription) }

        return runCatching { updatePlaylist(playlistId, playlist) }.isSuccess
    }

    private fun Playlist.toCreatePlaylist(): CreatePlaylist = CreatePlaylist(
        title = name.orEmpty(),
        description = description.orEmpty(),
        thumbnailUrl = thumbnailUrl
    )

    private suspend fun updatePlaylist(playlistId: String, playlist: Playlist) {
        tryHttpOrRaiseError { api.updatePlaylist(playlistId, playlist.toCreatePlaylist()) }
    }

    override suspend fun removeFromPlaylist(
        playlistId: String,
        videoId: String,
        index: Int
    ): Boolean {
        return runCatching { api.removeFromPlaylist(playlistId, videoId) }.isSuccess
    }

    override suspend fun createPlaylist(playlistName: String): String {
        return tryHttpOrRaiseError {
            api.createPlaylist(
                CreatePlaylist(
                    title = playlistName,
                    description = "",
                    thumbnailUrl = null
                )
            ).id
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return runCatching { api.deletePlaylist(playlistId) }.isSuccess
    }

    override suspend fun createSubscriptionGroup(name: String): String {
        return tryHttpOrRaiseError {
            api.createSubscriptionGroup(
                com.github.libretube.api.ltsync.obj.SubscriptionGroup(
                    "",
                    name
                )
            ).id
        }
    }

    override suspend fun renameSubscriptionGroup(
        subscriptionGroupId: String,
        newName: String
    ) {
        tryHttpOrRaiseError {
            api.updateSubscriptionGroup(
                subscriptionGroupId, com.github.libretube.api.ltsync.obj.SubscriptionGroup(
                    subscriptionGroupId, title = newName
                )
            )
        }
    }

    override suspend fun deleteSubscriptionGroup(subscriptionGroupId: String) {
        tryHttpOrRaiseError {
            api.deleteSubscriptionGroup(subscriptionGroupId)
        }
    }

    private fun ExtendedSubscriptionGroup.toSubscriptionGroup(): SubscriptionGroup {
        return SubscriptionGroup(
            id = group.id,
            name = group.title,
            channels = channels.map { it.id }
        )
    }

    override suspend fun getSubscriptionGroup(subscriptionGroupId: String): SubscriptionGroup {
        return tryHttpOrRaiseError {
            api.getSubscriptionGroup(subscriptionGroupId).toSubscriptionGroup()
        }
    }

    override suspend fun getSubscriptionGroups(): List<SubscriptionGroup> {
        return tryHttpOrRaiseError {
            api.getSubscriptionGroups().map { it.toSubscriptionGroup() }
        }
    }

    override suspend fun addToSubscriptionGroup(
        subscriptionGroupId: String,
        channelId: String
    ) {
        tryHttpOrRaiseError {
            api.addToSubscriptionGroup(subscriptionGroupId, channelId)
        }
    }

    override suspend fun removeFromSubscriptionGroup(
        subscriptionGroupId: String,
        channelId: String
    ) {
        tryHttpOrRaiseError {
            api.removeFromSubscriptionGroup(subscriptionGroupId, channelId)
        }
    }

    override suspend fun getPlaylistBookmarks(): List<PlaylistBookmark> {
        return tryHttpOrRaiseError {
            api.getPlaylistBookmarks().map {
                it.toPlaylistBookmark()
            }
        }
    }

    override suspend fun getPlaylistBookmark(playlistId: String): PlaylistBookmark? {
        return tryHttpOrRaiseError {
            try {
                api.getPlaylistBookmark(playlistId).toPlaylistBookmark()
            } catch (e: HttpException) {
                // The API returns 404 Not Found if the playlist is not bookmarked
                if (e.code() == 404) return@tryHttpOrRaiseError null
                else throw e
            }
        }
    }

    private fun PlaylistBookmark.toExtendedPublicPlaylist(): ExtendedPublicPlaylist =
        ExtendedPublicPlaylist(
            playlist = ExtendedPlaylist(
                id = playlistId,
                title = playlistName.orEmpty(),
                description = "", // TODO: also store and support playlist descriptions
                thumbnailUrl = thumbnailUrl,
                videoCount = videos.toLong()
            ),
            uploader = Channel(
                id = uploaderUrl.orEmpty(),
                avatar = uploaderAvatar.orEmpty(),
                name = uploader.orEmpty(),
                verified = false
            )
        )

    override suspend fun createPlaylistBookmark(playlist: PlaylistBookmark) {
        tryHttpOrRaiseError {
            api.createPlaylistBookmark(
                playlist.toExtendedPublicPlaylist()
            )
        }
    }

    override suspend fun deletePlaylistBookmark(playlistId: String) {
        tryHttpOrRaiseError {
            api.deletePlaylistBookmark(playlistId)
        }
    }
}