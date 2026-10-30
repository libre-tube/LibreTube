package com.github.libretube.obj.bottomsheetitem

import androidx.annotation.DrawableRes

open class BottomSheetItem(
    val title: BottomSheetItemTitle,
    @get:DrawableRes @field:DrawableRes
    val drawable: Int? = null,
    val getCurrent: () -> BottomSheetItemTitle? = { null },
    val isSelected: Boolean = false,
    val onClick: () -> Unit = {},
)