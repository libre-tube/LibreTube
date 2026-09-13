package com.github.libretube.enums

enum class WatchHistoryStatus(val isWatched: Boolean?) {
    ALL(null),
    CONTINUE_WATCHING(false),
    FINISHED(true)
}