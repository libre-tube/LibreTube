package com.github.libretube.obj.bottomsheetitem

import androidx.annotation.DrawableRes

open class BottomSheetItem(
    val title: BottomSheetItemTitle,
    val id: Id,
    @get:DrawableRes @field:DrawableRes
    val drawable: Int? = null,
    val getCurrent: () -> BottomSheetItemTitle? = { null },
    val isSelected: Boolean = false,
    val onClick: () -> Unit = {},
) {
    sealed interface Id {
        data object ExoRepeatOff: Id
        data object ExoRepeatOne: Id
        data object ExoRepeatAll: Id
        data object ExoResizeMode: Id
        data object ExoPlaybackSpeed: Id
        data object ExoSleepTimer: Id
        data object ExoQuality: Id
        data object ExoAudioTrack: Id
        data object ExoCaptions: Id
        data object ExoImATeapot: Id
        //
        data object PlaylistBackgroundPlay: Id
        data object PlaylistDownload: Id
        data object PlaylistAddToQueue: Id
        data object PlaylistShare: Id
        data object PlaylistClone: Id
        data object PlaylistRemoveBookmark: Id
        data object PlaylistAddBookmark: Id
        data object PlaylistExport: Id
        data object PlaylistRename: Id
        data object PlaylistChangeDescription: Id
        data object PlaylistDelete: Id
        //
        data object ChannelShare: Id
        data object ChannelPlayLatest: Id
        data object ChannelBackgroundPlay: Id
        data object ChannelAddToGroup: Id
        //
        data object DListBackgroundPlay: Id
        data object DListNext: Id
        data object DListAddToQueue: Id
        data object DListShare: Id
        data object DListDelete: Id
        data object DListExport: Id
        data object DListGoto: Id
        //
        data object SortOldest: Id
        data object SortNewest: Id
        data object SortDuration: Id
        data object SortRevDuration: Id
        data object SortAlphabetic: Id
        data object SortRevAlphabetic: Id
        //
        data object RepeatOff: Id
        data object RepeatOne: Id
        data object RepeatAll: Id
        //
        data object ResizeFit: Id
        data object ResizeZoom: Id
        data object ResizeFill: Id
        //
        data object VideoBackgroundPlay: Id
        data object VideoNext: Id
        data object VideoQueue: Id
        data object VideoUnwatched: Id
        data object VideoWatched: Id
        data object VideoAddToPlaylist: Id
        data object VideoDownload: Id
        data object VideoShare: Id
        //
        data class DSortOrdinal(val ordinal: Int): Id
        data class SimpleItem(val index: Int): Id
    }
}