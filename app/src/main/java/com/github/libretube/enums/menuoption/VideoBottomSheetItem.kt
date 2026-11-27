package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class VideoBottomSheetItem(
    @StringRes titleStringRes: Int,
    id: Id,
    @DrawableRes drawableRes: Int,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    id = id,
    drawable = drawableRes,
) {
    data object PlayOnBackground : VideoBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        id = Id.VideoBackgroundPlay,
        drawableRes = R.drawable.ic_headphones,
    )

    data object PlayNext : VideoBottomSheetItem(
        titleStringRes = R.string.play_next,
        id = Id.VideoNext,
        drawableRes = R.drawable.ic_playlist,
    )

    data object AddToQueue : VideoBottomSheetItem(
        titleStringRes = R.string.add_to_queue,
        id = Id.VideoQueue,
        drawableRes = R.drawable.ic_queue,
    )

    data object MarkAsUnwatched : VideoBottomSheetItem(
        titleStringRes = R.string.mark_as_unwatched,
        id = Id.VideoUnwatched,
        drawableRes = R.drawable.ic_eye_crossed_out,
    )

    data object MarkAsWatched : VideoBottomSheetItem(
        titleStringRes = R.string.mark_as_watched,
        id = Id.VideoWatched,
        drawableRes = R.drawable.ic_eye,
    )

    data object AddToPlaylist : VideoBottomSheetItem(
        titleStringRes = R.string.addToPlaylist,
        id = Id.VideoAddToPlaylist,
        drawableRes = R.drawable.ic_playlist_add,
    )

    data object Download : VideoBottomSheetItem(
        titleStringRes = R.string.download,
        id = Id.VideoDownload,
        drawableRes = R.drawable.ic_download,
    )

    data object Share : VideoBottomSheetItem(
        titleStringRes = R.string.share,
        id = Id.VideoShare,
        drawableRes = R.drawable.ic_share,
    )
}