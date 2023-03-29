package com.github.libretube.obj

import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.db.obj.LocalPlaylistWithVideos
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.SubscriptionGroup
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition
import kotlinx.serialization.Serializable

@Serializable
data class BackupFile(
    var watchHistory: List<WatchHistoryItem>? = emptyList(),
    var watchPositions: List<WatchPosition>? = emptyList(),
    var searchHistory: List<SearchHistoryItem>? = emptyList(),
    var localSubscriptions: List<LocalSubscription>? = emptyList(),
    var customInstances: List<CustomInstance>? = emptyList(),
    var playlistBookmarks: List<PlaylistBookmark>? = emptyList(),
    var localPlaylists: List<LocalPlaylistWithVideos>? = emptyList(),
    var preferences: List<PreferenceItem>? = emptyList(),
    var channelGroups: List<SubscriptionGroup>? = emptyList()
)
