package com.github.libretube.enums.menuoption

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.media3.common.Player
import com.github.libretube.R
import com.github.libretube.obj.bottomsheetitem.BottomSheetItem
import com.github.libretube.obj.bottomsheetitem.BottomSheetItemTitle

sealed class RepeatModeBottomSheetItem(
    val playerModeKey: Int,
    @StringRes titleStringRes: Int,
    @DrawableRes drawableRes: Int
) : BottomSheetItem(
    title = BottomSheetItemTitle.StringResource(titleStringRes),
    drawable = drawableRes,
) {

    companion object {
        val entries = listOf(
            RepeatModeOff,
            RepeatModeOne,
            RepeatModeAll,
        )
    }

    data object RepeatModeOff : RepeatModeBottomSheetItem(
        Player.REPEAT_MODE_OFF,
        R.string.repeat_mode_none,
        R.drawable.ic_playlist,
    )

    data object RepeatModeOne : RepeatModeBottomSheetItem(
        Player.REPEAT_MODE_ONE,
        R.string.repeat_mode_current,
        R.drawable.ic_repeat_one,
    )

    data object RepeatModeAll : RepeatModeBottomSheetItem(
        Player.REPEAT_MODE_ALL,
        R.string.repeat_mode_all,
        R.drawable.ic_repeat,
    )
}