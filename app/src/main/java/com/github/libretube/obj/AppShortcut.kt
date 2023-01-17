package com.github.libretube.obj

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class AppShortcut(
    val action: String,
    @StringRes val label: Int,
    @DrawableRes val drawable: Int
)
