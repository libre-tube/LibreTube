package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R

enum class VideoMenuOption(
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int,
): MenuOption {
    PLAY_ON_BACKGROUND(R.string.playOnBackground, R.drawable.ic_headphones),
    PLAY_NEXT(R.string.play_next, R.drawable.ic_playlist),
    ADD_TO_QUEUE(R.string.add_to_queue, R.drawable.ic_queue),
    MARK_AS_UNWATCHED(R.string.mark_as_unwatched, R.drawable.ic_eye_crossed_out),
    MARK_AS_WATCHED(R.string.mark_as_watched, R.drawable.ic_eye),
    ADD_TO_PLAYLIST(R.string.addToPlaylist, R.drawable.ic_playlist_add),
    DOWNLOAD(R.string.download, R.drawable.ic_download),
    SHARE(R.string.share, R.drawable.ic_share),
}