package com.github.libretube.enums.menuoption

import androidx.annotation.StringRes
import androidx.media3.common.Player
import com.github.libretube.R

enum class RepeatModeMenuOption(
    val playerModeKey: Int,
    @get:StringRes override val titleStringRes: Int,
    override val drawableRes: Int
) : MenuOption {
    REPEAT_MODE_OFF(
        Player.REPEAT_MODE_OFF,
        R.string.repeat_mode_none,
        R.drawable.ic_playlist,
    ),
    REPEAT_MODE_ONE(
        Player.REPEAT_MODE_ONE,
        R.string.repeat_mode_current,
        R.drawable.ic_repeat_one,
    ),
    REPEAT_MODE_ALL(
        Player.REPEAT_MODE_ALL,
        R.string.repeat_mode_all,
        R.drawable.ic_repeat,
    ),
}