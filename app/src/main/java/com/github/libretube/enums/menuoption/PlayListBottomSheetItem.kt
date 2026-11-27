package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle
sealed class PlayListBottomSheetItem(
    @StringRes titleStringRes: Int,
    id: Id,
    @DrawableRes drawableRes: Int,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    id = id,
    drawable = drawableRes,
) {
    data object PlayOnBackground : PlayListBottomSheetItem(
        titleStringRes = R.string.playOnBackground,
        id = Id.PlaylistBackgroundPlay,
        drawableRes = R.drawable.ic_headphones,
    )

    data object Download : PlayListBottomSheetItem(
        titleStringRes = R.string.download,
        id = Id.PlaylistDownload,
        drawableRes = R.drawable.ic_download,
    )

    data object AddToQueue : PlayListBottomSheetItem(
        titleStringRes = R.string.add_to_queue,
        id = Id.PlaylistAddToQueue,
        drawableRes = R.drawable.ic_queue,
    )

    data object Share : PlayListBottomSheetItem(
        titleStringRes = R.string.share,
        id = Id.PlaylistShare,
        drawableRes = R.drawable.ic_share,
    )

    data object Clone : PlayListBottomSheetItem(
        titleStringRes = R.string.clonePlaylist,
        id = Id.PlaylistClone,
        drawableRes = R.drawable.ic_copy,
    )

    data object RemoveFromBookmarks : PlayListBottomSheetItem(
        titleStringRes = R.string.remove_bookmark,
        id = Id.PlaylistRemoveBookmark,
        drawableRes = R.drawable.ic_remove_from_bookmark,
    )

    data object AddToBookmarks : PlayListBottomSheetItem(
        titleStringRes = R.string.add_to_bookmarks,
        id = Id.PlaylistAddBookmark,
        drawableRes = R.drawable.ic_add_to_bookmark,
    )

    data object Export : PlayListBottomSheetItem(
        titleStringRes = R.string.export_playlist,
        id = Id.PlaylistExport,
        drawableRes = R.drawable.ic_export,
    )

    data object Rename : PlayListBottomSheetItem(
        titleStringRes = R.string.renamePlaylist,
        id = Id.PlaylistRename,
        drawableRes = R.drawable.ic_edit,
    )

    data object ChangeDescription : PlayListBottomSheetItem(
        titleStringRes = R.string.change_playlist_description,
        id = Id.PlaylistChangeDescription,
        drawableRes = R.drawable.ic_description,
    )

    data object Delete : PlayListBottomSheetItem(
        titleStringRes = R.string.deletePlaylist,
        id = Id.PlaylistDelete,
        drawableRes = R.drawable.ic_delete,
    )
}