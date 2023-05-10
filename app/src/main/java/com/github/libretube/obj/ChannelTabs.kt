package com.github.libretube.obj

import androidx.annotation.IdRes
import com.github.libretube.R

sealed class ChannelTabs(
    val identifierName: String,
    @IdRes val chipId: Int,
) {
    object Playlists : ChannelTabs("playlists", R.id.playlists)
    object Shorts : ChannelTabs("shorts", R.id.shorts)
    object Livestreams : ChannelTabs("livestreams", R.id.livestreams)
    object Channels : ChannelTabs("channels", R.id.channels)
}
