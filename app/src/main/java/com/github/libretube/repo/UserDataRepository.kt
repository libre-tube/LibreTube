package com.github.libretube.repo

import android.util.Log
import com.github.libretube.api.MediaServiceRepository
import com.github.libretube.api.PlaylistsHelper.MAX_CONCURRENT_IMPORT_CALLS
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.api.obj.WatchHistoryEntry
import com.github.libretube.api.obj.WatchHistoryEntryMetadata
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.extensions.TAG
import com.github.libretube.extensions.parallelMap
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_DELAY
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_BATCH_SIZE
import com.github.libretube.repo.LocalFeedRepository.Companion.CHANNEL_CHUNK_SIZE
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.channel.ChannelInfo
import java.util.concurrent.atomic.AtomicInteger

interface UserDataRepository {
    var requiresLogin: Boolean

    suspend fun login(username: String, password: String): String = ""
    suspend fun register(username: String, password: String): String = ""
    suspend fun deleteAccount(password: String) = Unit

    suspend fun subscribe(channelId: String, name: String, uploaderAvatar: String?, verified: Boolean)
    suspend fun unsubscribe(channelId: String)
    // TODO: isSubscribed shouldn't be able to return null?
    suspend fun isSubscribed(channelId: String): Boolean?
    suspend fun getSubscriptions(): List<Subscription>
    suspend fun getSubscriptionChannelIds(): List<String>
    suspend fun submitSubscriptionChannelInfosChanged(subscriptions: List<Subscription>) {}

    suspend fun getPlaylist(playlistId: String): Playlist
    suspend fun getPlaylists(): List<Playlists>
    suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean
    suspend fun renamePlaylist(playlistId: String, newName: String): Boolean
    suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean
    suspend fun removeFromPlaylist(playlistId: String, videoId: String, index: Int): Boolean
    suspend fun createPlaylist(playlistName: String): String?
    suspend fun deletePlaylist(playlistId: String): Boolean

    suspend fun createSubscriptionGroup(name: String): String
    suspend fun renameSubscriptionGroup(subscriptionGroupId: String, newName: String)
    suspend fun deleteSubscriptionGroup(subscriptionGroupId: String)
    suspend fun getSubscriptionGroup(subscriptionGroupId: String): SubscriptionGroup
    suspend fun getSubscriptionGroups(): List<SubscriptionGroup>
    suspend fun addToSubscriptionGroup(subscriptionGroupId: String, channelId: String)
    suspend fun removeFromSubscriptionGroup(subscriptionGroupId: String, channelId: String)

    suspend fun addToWatchHistory(watchHistoryEntry: WatchHistoryEntry)
    suspend fun updateWatchHistoryEntry(metadata: WatchHistoryEntryMetadata)
    suspend fun removeFromWatchHistory(videoId: String)
    suspend fun getWatchHistory(page: Int): List<WatchHistoryEntry>
    suspend fun getFromWatchHistory(videoId: String): WatchHistoryEntry?
    suspend fun clearWatchHistory()

    suspend fun getPlaylistBookmarks(): List<PlaylistBookmark>
    suspend fun getPlaylistBookmark(playlistId: String): PlaylistBookmark?
    suspend fun createPlaylistBookmark(playlist: PlaylistBookmark)
    suspend fun deletePlaylistBookmark(playlistId: String)

    // The following methods can be overriden to offload the work to the server, but in most cases
    // the default implementation should work out just fine.

    suspend fun importSubscriptions(newChannels: List<String>) {
        val subscribedChannels = getSubscriptionChannelIds()

        val newFiltered = newChannels.filter { !subscribedChannels.contains(it) }

        val failedChannels = mutableListOf<String>()

        val channelExtractionCount = AtomicInteger()
        for (chunk in newFiltered.chunked(CHANNEL_CHUNK_SIZE)) {
            // avoid being rate-limited by adding random delays between requests
            val count = channelExtractionCount.get()
            if (count >= CHANNEL_BATCH_SIZE) {
                // add a delay after each BATCH_SIZE amount of fully-fetched channels
                delay(CHANNEL_BATCH_DELAY.random())
                channelExtractionCount.set(0)
            }

            chunk.parallelMap { channelId ->
                try {
                    val channelUrl = "$YOUTUBE_FRONTEND_URL/channel/${channelId}"
                    val channelInfo = ChannelInfo.getInfo(channelUrl)

                    val avatarUrl = channelInfo.avatars.maxByOrNull { it.height }?.url
                    subscribe(channelId, channelInfo.name, avatarUrl, channelInfo.isVerified)
                } catch (e: Exception) {
                    Log.e(TAG(), e.toString())
                    failedChannels.add(channelId)
                }
            }
        }

        if (!failedChannels.isEmpty()) {
            throw Exception("Failed to import ${failedChannels.joinToString(", ")}")
        }
    }

    suspend fun clonePlaylist(playlistId: String): String? {
        val playlist = MediaServiceRepository.instance.getPlaylist(playlistId)
        val newPlaylist = createPlaylist(playlist.name ?: "Unknown name") ?: return null

        addToPlaylist(newPlaylist, *playlist.relatedStreams.toTypedArray())

        var nextPage = playlist.nextpage
        while (nextPage != null) {
            nextPage = runCatching {
                MediaServiceRepository.instance.getPlaylistNextPage(playlistId, nextPage).apply {
                    addToPlaylist(newPlaylist, *relatedStreams.toTypedArray())
                }.nextpage
            }.getOrNull()
        }

        return playlistId
    }

    suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        for (playlist in playlists) {
            val playlistId = createPlaylist(playlist.name!!) ?: throw Exception("failed to create playlist")

            // if not logged in, all video information needs to become fetched manually
            // Only do so with `MAX_CONCURRENT_IMPORT_CALLS` videos at once to prevent performance issues
            for (videoIdList in playlist.videos.chunked(MAX_CONCURRENT_IMPORT_CALLS)) {
                val streams = videoIdList.parallelMap {
                    runCatching { MediaServiceRepository.instance.getStreams(it) }
                        .getOrNull()
                        ?.toStreamItem(it)
                }.filterNotNull()

                addToPlaylist(playlistId, *streams.toTypedArray())
            }
        }
    }
}