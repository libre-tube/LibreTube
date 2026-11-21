package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class DownloadListMenuOption(
    @StringRes titleStringRes: Int,
    id: Id,
    @DrawableRes drawableRes: Int,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    id = id,
    drawable = drawableRes,
) {
    data object PlayOnBackground :
        DownloadListMenuOption(R.string.playOnBackground, id=Id.DListBackgroundPlay, R.drawable.ic_headphones)

    data object PlayNext : DownloadListMenuOption(R.string.play_next, id=Id.DListNext,R.drawable.ic_playlist)
    data object AddToQueue : DownloadListMenuOption(R.string.add_to_queue,id=Id.DListAddToQueue, R.drawable.ic_queue)
    data object Share : DownloadListMenuOption(R.string.share, id=Id.DListShare,R.drawable.ic_share)
    data object Delete : DownloadListMenuOption(R.string.delete,id=Id.DListDelete, R.drawable.ic_delete)
    data object Export : DownloadListMenuOption(R.string.export,id=Id.DListExport, R.drawable.ic_export)
    data object GoToVideo : DownloadListMenuOption(R.string.go_to_video,id=Id.DListGoto, R.drawable.ic_play)
}