package com.github.libretube.enums

enum class WatchHistoryStatus(
    /**
     * Whether the video is already watched completely.
     * `null` means that videos are not filtered for their watch status, i.e. all are shown.
     */
    val isWatched: Boolean?,
) {
    ALL(null),
    CONTINUE_WATCHING(false),
    FINISHED(true)
}