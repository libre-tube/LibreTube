package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class PlayListBottomSheetItem(
    @get:StringRes val titleStringRes: Int,
    val drawableRes: Int,
) : BottomSheetItem<BottomSheetItemTitle.StringResource>(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {
    data object PlayOnBackground : PlayListBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        drawableRes = R.drawable.ic_headphones,
    )

    data object Download : PlayListBottomSheetItem(
        titleStringRes = R.string.download,
        drawableRes = R.drawable.ic_download,
    )

    data object AddToQueue : PlayListBottomSheetItem(
        titleStringRes = R.string.add_to_queue,
        drawableRes = R.drawable.ic_queue,
    )

    data object Share : PlayListBottomSheetItem(
        titleStringRes = R.string.share,
        drawableRes = R.drawable.ic_share,
    )

    data object Clone : PlayListBottomSheetItem(
        titleStringRes = R.string.clonePlaylist,
        drawableRes = R.drawable.ic_copy,
    )

    data object RemoveFromBookmarks : PlayListBottomSheetItem(
        titleStringRes = R.string.remove_bookmark,
        drawableRes = R.drawable.ic_remove_from_bookmark,
    )

    data object AddToBookmarks : PlayListBottomSheetItem(
        titleStringRes = R.string.add_to_bookmarks,
        drawableRes = R.drawable.ic_add_to_bookmark,
    )

    data object Export : PlayListBottomSheetItem(
        titleStringRes = R.string.export_playlist,
        drawableRes = R.drawable.ic_export,
    )

    data object Rename : PlayListBottomSheetItem(
        titleStringRes = R.string.renamePlaylist,
        drawableRes = R.drawable.ic_edit,
    )

    data object ChangeDescription : PlayListBottomSheetItem(
        titleStringRes = R.string.change_playlist_description,
        drawableRes = R.drawable.ic_description,
    )

    data object Delete : PlayListBottomSheetItem(
        titleStringRes = R.string.deletePlaylist,
        drawableRes = R.drawable.ic_delete,
    )
}