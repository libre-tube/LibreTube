package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class DownloadListMenuOption(
    @get:StringRes val titleStringRes: Int,
    val drawableRes: Int,
) : BottomSheetItem<BottomSheetItemTitle.StringResource>(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {
    data object PlayOnBackground :
        DownloadListMenuOption(R.string.playOnBackground, R.drawable.ic_headphones)

    data object PlayNext : DownloadListMenuOption(R.string.play_next, R.drawable.ic_playlist)
    data object AddToQueue : DownloadListMenuOption(R.string.add_to_queue, R.drawable.ic_queue)
    data object Share : DownloadListMenuOption(R.string.share, R.drawable.ic_share)
    data object Delete : DownloadListMenuOption(R.string.delete, R.drawable.ic_delete)
    data object Export : DownloadListMenuOption(R.string.export, R.drawable.ic_export)
    data object GoToVideo : DownloadListMenuOption(R.string.go_to_video, R.drawable.ic_play)
}