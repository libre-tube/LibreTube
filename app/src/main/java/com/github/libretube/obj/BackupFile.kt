package com.github.libretube.obj

import com.github.libretube.db.obj.*

data class BackupFile(
    var watchHistory: List<WatchHistoryItem>? = null,
    var watchPositions: List<WatchPosition>? = null,
    var searchHistory: List<SearchHistoryItem>? = null,
    var localSubscriptions: List<LocalSubscription>? = null,
    var customInstances: List<CustomInstance>? = null
)
