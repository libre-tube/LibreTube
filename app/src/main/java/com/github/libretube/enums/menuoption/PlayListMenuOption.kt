package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R

enum class PlayListMenuOption(
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int,
): MenuOption {
    PLAY_ON_BACKGROUND(R.string.playOnBackground, R.drawable.ic_headphones),
    DOWNLOAD(R.string.download, R.drawable.ic_download),
    ADD_TO_QUEUE(R.string.add_to_queue, R.drawable.ic_queue),
    SHARE(R.string.share, R.drawable.ic_share),
    CLONE(R.string.clonePlaylist, R.drawable.ic_copy),
    REMOVE_FROM_BOOKMARKS(R.string.remove_bookmark, R.drawable.ic_remove_from_bookmark),
    ADD_TO_BOOKMARKS(R.string.add_to_bookmarks, R.drawable.ic_add_to_bookmark),
    EXPORT(R.string.export_playlist, R.drawable.ic_export),
    RENAME(R.string.renamePlaylist, R.drawable.ic_title),
    CHANGE_DESCRIPTION(R.string.change_playlist_description, R.drawable.ic_description),
    DELETE(R.string.deletePlaylist, R.drawable.ic_delete),
}