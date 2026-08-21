package com.github.libretube.enums

import androidx.annotation.StringRes
import com.github.libretube.R

enum class DownloadTab {
    VIDEO,
    AUDIO,
    PLAYLIST
}

enum class DownloadSortingOrder(@StringRes val stringId: Int) {
    OLDEST(R.string.least_recent),
    NEWEST(R.string.most_recent),
    ALPHABETIC(R.string.alphabetic),
    DURATION(R.string.duration),
    CHANNEL(R.string.sort_channel),
    SIZE(R.string.sort_size)
}
