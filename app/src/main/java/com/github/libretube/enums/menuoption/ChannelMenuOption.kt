package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R

enum class ChannelMenuOption(
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int?,
): MenuOption {
    SHARE(R.string.share, R.drawable.ic_share),
    PLAY_LATEST_VIDEOS(R.string.play_latest_videos, R.drawable.ic_playlist),
    PLAY_ON_BACKGROUND(R.string.playOnBackground, R.drawable.ic_headphones),
    ADD_TO_GROUP(R.string.add_to_group, R.drawable.ic_add_to_bookmark),
}