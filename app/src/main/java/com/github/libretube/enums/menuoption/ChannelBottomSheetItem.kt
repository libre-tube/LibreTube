package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class ChannelBottomSheetItem(
    @StringRes titleStringRes: Int,
    id: Id,
    @DrawableRes drawableRes: Int?,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    id = id,
    drawable = drawableRes,
) {
    data object Share : ChannelBottomSheetItem(
        titleStringRes = R.string.share,
        id = Id.ChannelShare,
        drawableRes = R.drawable.ic_share,
    )

    data object PlayLatestVideos : ChannelBottomSheetItem(
        titleStringRes = R.string.play_latest_videos,
        id = Id.ChannelPlayLatest,
        drawableRes = R.drawable.ic_playlist,
    )

    data object PlayOnBackground : ChannelBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        id = Id.ChannelBackgroundPlay,
        drawableRes = R.drawable.ic_headphones,
    )

    data object AddToGroup : ChannelBottomSheetItem(
        titleStringRes = R.string.add_to_group,
        id = Id.ChannelAddToGroup,
        drawableRes = R.drawable.ic_add_to_bookmark,
    )
}