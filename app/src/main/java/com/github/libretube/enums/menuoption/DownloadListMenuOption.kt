package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R

enum class DownloadListMenuOption(
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int,
): MenuOption {
    PLAY_ON_BACKGROUND(R.string.playOnBackground, R.drawable.ic_headphones),
    PLAY_NEXT(R.string.play_next, R.drawable.ic_playlist),
    ADD_TO_QUEUE(R.string.add_to_queue, R.drawable.ic_queue),
    SHARE(R.string.share, R.drawable.ic_share),
    DELETE(R.string.delete, R.drawable.ic_delete),
    EXPORT(R.string.export, R.drawable.ic_export),
    GO_TO_VIDEO(R.string.go_to_video, R.drawable.ic_play),
}