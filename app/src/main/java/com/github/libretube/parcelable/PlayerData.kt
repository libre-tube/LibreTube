package com.github.libretube.parcelable

import android.os.Parcelable
import com.github.libretube.ui.fragments.DownloadSortingOrder
import com.github.libretube.ui.fragments.DownloadTab
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayerData(
    val videoId: String,
    val playlistId: String? = null,
    val channelId: String? = null,
    val keepQueue: Boolean = false,
    val timestamp: Long = 0,
    val shuffle: Boolean = false,
    val isOffline: Boolean = false,
    val downloadTab: DownloadTab? = null,
    val downloadSortingOrder: DownloadSortingOrder? = null,
) : Parcelable
