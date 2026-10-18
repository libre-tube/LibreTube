package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class PlayListSortModeBottomSheetItem(
    @get:StringRes val titleStringRes: Int,
    val drawableRes: Int?,
) : BottomSheetItem<BottomSheetItemTitle.StringResource>(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
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
        drawableRes = R.drawable.ic_sort_by_oldest,
    )

    data object MostRecent : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.most_recent,
        drawableRes = R.drawable.ic_sort_by_newest,
    )

    data object Duration : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.duration,
        drawableRes = R.drawable.ic_sort_by_duration,
    )

    data object DurationReversed : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.duration_reversed,
        drawableRes = R.drawable.ic_sort_by_duration_reversed,
    )

    data object Alphabetic : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.alphabetic,
        drawableRes = R.drawable.ic_sort_by_alpha,
    )

    data object AlphabeticReversed : PlayListSortModeBottomSheetItem(
        titleStringRes = R.string.alphabetic_reversed,
        drawableRes = R.drawable.ic_sort_by_alpha_reversed,
    )
}