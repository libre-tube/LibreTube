package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class PlayListSortModeBottomSheetItem(
    @StringRes titleStringRes: Int,
    id: Id,
    @DrawableRes drawableRes: Int?,
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    id = id,
    drawable = drawableRes,
) {
    companion object {
        val entries by lazy {
            listOf(
                LeastRecent,
                MostRecent,
                Duration,
                DurationReversed,
                Alphabetic,
                AlphabeticReversed,
            )
        }
    }

    data object LeastRecent : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.least_recent,
        id = Id.SortOldest,
        drawableRes = R.drawable.ic_sort_by_oldest,
    )

    data object MostRecent : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.most_recent,
        id = Id.SortNewest,
        drawableRes = R.drawable.ic_sort_by_newest,
    )

    data object Duration : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.duration,
        id = Id.SortDuration,
        drawableRes = R.drawable.ic_sort_by_duration,
    )

    data object DurationReversed : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.duration_reversed,
        id = Id.SortRevDuration,
        drawableRes = R.drawable.ic_sort_by_duration_reversed,
    )

    data object Alphabetic : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.alphabetic,
        id = Id.SortAlphabetic,
        drawableRes = R.drawable.ic_sort_by_alpha,
    )

    data object AlphabeticReversed : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.alphabetic_reversed,
        id = Id.SortRevAlphabetic,
        drawableRes = R.drawable.ic_sort_by_alpha_reversed,
    )
}