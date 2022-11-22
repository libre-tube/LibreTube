package com.github.libretube.obj

import com.github.libretube.db.obj.CustomInstance
import com.github.libretube.db.obj.LocalSubscription
import com.github.libretube.db.obj.PlaylistBookmark
import com.github.libretube.db.obj.SearchHistoryItem
import com.github.libretube.db.obj.WatchHistoryItem
import com.github.libretube.db.obj.WatchPosition

data class BackupFile(
    var watchHistory: List<WatchHistoryItem>? = null,
    var watchPositions: List<WatchPosition>? = null,
    var searchHistory: List<SearchHistoryItem>? = null,
    var localSubscriptions: List<LocalSubscription>? = null,
    var customInstances: List<CustomInstance>? = null,
    var playlistBookmarks: List<PlaylistBookmark>? = null,
    var preferences: List<PreferenceItem>? = null
)
