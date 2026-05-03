package com.github.libretube.repo

import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.api.obj.WatchHistoryEntryMetadata
import com.github.libretube.db.DatabaseHolder.Database
import com.github.libretube.db.obj.LocalPlaylist
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SubscriptionGroup

class LocalUserDataRepository : UserDataRepository {
    override var requiresLogin: Boolean = false

    override suspend fun getPlaylist(playlistId: String): Playlist {
        val relation = Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }

        return Playlist(
            name = relation.playlist.name,
            description = relation.playlist.description,
            thumbnailUrl = relation.playlist.thumbnailUrl,
            videos = relation.videos.size,
            relatedStreams = relation.videos.map { it.toStreamItem() }
        )
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return Database.localPlaylistsDao().getAll()
            .map {
                Playlists(
                    id = it.playlist.id.toString(),
                    name = it.playlist.name,
                    shortDescription = it.playlist.description,
                    thumbnail = it.playlist.thumbnailUrl,
                    videos = it.videos.size.toLong()
                )
            }
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        val localPlaylist = Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }

        for (video in videos) {
            val localPlaylistItem = video.toLocalPlaylistItem(playlistId)

            val existingVideo = Database.localPlaylistsDao()
                .getPlaylistVideo(playlistId, localPlaylistItem.videoId)
            if (existingVideo != null) {
                // update existing video metadata
                localPlaylistItem.id = existingVideo.id
                Database.localPlaylistsDao().updatePlaylistVideo(localPlaylistItem)
                continue
            }

            // add the new video to the database
            Database.localPlaylistsDao().addPlaylistVideo(localPlaylistItem)

            val playlist = localPlaylist.playlist
            if (playlist.thumbnailUrl.isEmpty()) {
                // set the new playlist thumbnail URL
                localPlaylistItem.thumbnailUrl?.let {
                    playlist.thumbnailUrl = it
                    Database.localPlaylistsDao().updatePlaylist(playlist)
                }
            }
        }

        return true
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        val playlist = Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }.playlist
        playlist.name = newName
        Database.localPlaylistsDao().updatePlaylist(playlist)

        return true
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        val playlist = Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }.playlist
        playlist.description = newDescription
        Database.localPlaylistsDao().updatePlaylist(playlist)

        return true
    }

    override suspend fun removeFromPlaylist(playlistId: String, videoId: String, index: Int): Boolean {
        val transaction = Database.localPlaylistsDao().getAll()
            .first { it.playlist.id.toString() == playlistId }
        Database.localPlaylistsDao().removePlaylistVideo(
            transaction.videos[index]
        )
        // set a new playlist thumbnail if the first video got removed
        if (index == 0) {
            transaction.playlist.thumbnailUrl =
                transaction.videos.getOrNull(1)?.thumbnailUrl.orEmpty()
        }
        Database.localPlaylistsDao().updatePlaylist(transaction.playlist)

        return true
    }

    override suspend fun createPlaylist(playlistName: String): String {
        val playlist = LocalPlaylist(name = playlistName, thumbnailUrl = "")
        return Database.localPlaylistsDao().createPlaylist(playlist).toString()
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        Database.localPlaylistsDao().deletePlaylistById(playlistId)
        Database.localPlaylistsDao().deletePlaylistItemsByPlaylistId(playlistId)

        return true
    }

    override suspend fun subscribe(
        channelId: String, name: String, uploaderAvatar: String?, verified: Boolean
    ) {
        val localSubscription = LocalSubscription(
            channelId = channelId,
            name = name,
            avatar = uploaderAvatar,
            verified = verified
        )

        Database.localSubscriptionDao().insert(localSubscription)
    }

    override suspend fun unsubscribe(channelId: String) {
        Database.localSubscriptionDao().deleteById(channelId)
    }

    override suspend fun isSubscribed(channelId: String): Boolean {
        return Database.localSubscriptionDao().includes(channelId)
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        // load all channels that have not been fetched yet
        val unfinished = Database.localSubscriptionDao().getChannelsWithoutMetaInfo()
        runCatching {
            importSubscriptions(unfinished.map { it.channelId })
        }

        return Database.localSubscriptionDao().getAll().map {
            Subscription(
                url = it.channelId,
                name = it.name.orEmpty(),
                avatar = it.avatar,
                verified = it.verified
            )
        }
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return Database.localSubscriptionDao().getAll().map { it.channelId }
    }

    override suspend fun submitSubscriptionChannelInfosChanged(subscriptions: List<Subscription>) {
        Database.localSubscriptionDao().updateAll(subscriptions.map {
            LocalSubscription(
                it.url,
                it.name,
                it.avatar,
                it.verified
            )
        })
    }

    override suspend fun createSubscriptionGroup(name: String): String {
        Database.subscriptionGroupsDao()
            .createGroup(SubscriptionGroup(name = name))

        // name is the ID in our database modeling
        return name
    }

    override suspend fun renameSubscriptionGroup(
        subscriptionGroupId: String,
        newName: String
    ) {
        val updatedGroup = Database.subscriptionGroupsDao()
            .getByName(subscriptionGroupId)!!.copy(name = newName)

        // delete the old version of the group first before updating it, as the name is the
        // primary key
        Database.subscriptionGroupsDao().deleteGroup(subscriptionGroupId)
        Database.subscriptionGroupsDao().createGroup(updatedGroup)
    }

    override suspend fun deleteSubscriptionGroup(subscriptionGroupId: String) {
        Database.subscriptionGroupsDao()
            .deleteGroup(subscriptionGroupId)
    }

    override suspend fun getSubscriptionGroup(subscriptionGroupId: String): SubscriptionGroup {
        return Database.subscriptionGroupsDao().getByName(subscriptionGroupId)!!
    }

    override suspend fun getSubscriptionGroups(): List<SubscriptionGroup> {
        return Database.subscriptionGroupsDao().getAll()
            .sortedBy { it.index }
    }

    override suspend fun addToSubscriptionGroup(
        subscriptionGroupId: String,
        channelId: String
    ) {
        val group = Database.subscriptionGroupsDao().getByName(subscriptionGroupId)!!
        group.channels = group.channels.plus(channelId).distinct()
        Database.subscriptionGroupsDao().updateGroup(group)
    }

    override suspend fun removeFromSubscriptionGroup(
        subscriptionGroupId: String,
        channelId: String
    ) {
        val group = Database.subscriptionGroupsDao().getByName(subscriptionGroupId)!!
        group.channels = group.channels.filter { it != channelId }
        Database.subscriptionGroupsDao().updateGroup(group)
    }

    override suspend fun getPlaylistBookmarks(): List<PlaylistBookmark> {
        return Database.playlistBookmarkDao().getAll()
    }

    override suspend fun getPlaylistBookmark(playlistId: String): PlaylistBookmark? {
        return Database.playlistBookmarkDao().findById(playlistId)
    }

    override suspend fun createPlaylistBookmark(playlist: PlaylistBookmark) {
        Database.playlistBookmarkDao().insert(playlist)
    }

    override suspend fun deletePlaylistBookmark(playlistId: String) {
        Database.playlistBookmarkDao().deleteById(playlistId)
    }
}